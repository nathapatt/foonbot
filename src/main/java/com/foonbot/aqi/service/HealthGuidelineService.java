package com.foonbot.aqi.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foonbot.aqi.dtos.AirQualityDto;
import com.foonbot.aqi.repository.AirQualityRepository;

@Service
public class HealthGuidelineService {

    private static final Logger log = LoggerFactory.getLogger(HealthGuidelineService.class);

    private static final int DEFAULT_HISTORY_LIMIT = 14;
    private static final int MAX_HISTORY_LIMIT = 30;

    private final AirQualityRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String groqModel;

    public HealthGuidelineService(AirQualityRepository repository,
                                  RestTemplate restTemplate,
                                  ObjectMapper objectMapper) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateGuidelineText(String userId) {
        String safeUserId = requireUserId(userId);
        int limit = DEFAULT_HISTORY_LIMIT;

        List<AirQualityDto> history = repository.findByLineUserLineUserIdOrderByFetchedAtDesc(
                        safeUserId,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "fetchedAt"))
                )
                .stream()
                .map(AirQualityDto::new)
                .toList();

        if (history.isEmpty()) {
            throw new IllegalArgumentException("Please check AQI once first so I can build your health guideline.");
        }

        GuidanceContext context = buildContext(history);
        return generateWithGroq(context).orElseGet(() -> buildFallbackText(context));
    }

    private @NonNull GuidanceContext buildContext(List<AirQualityDto> history) {
        AirQualityDto latest = history.get(0);
        String trend = resolveTrend(history);
        String cautionGroup = resolveCautionGroup(latest.getAqiUs());
        List<String> actions = resolveActions(latest.getAqiUs(), latest.getMainPollutant(), trend);
        String trendNote = switch (trend) {
            case "improving" -> "Recent readings are improving compared with your older records.";
            case "worsening" -> "Recent readings are getting worse compared with your older records.";
            default -> "Recent readings are fairly stable.";
        };
        String trendNoteThai = switch (trend) {
            case "improving" -> "ค่าช่วงล่าสุดดีขึ้นเมื่อเทียบกับค่าก่อนหน้า";
            case "worsening" -> "ค่าช่วงล่าสุดแย่ลงเมื่อเทียบกับค่าก่อนหน้า";
            default -> "ค่าช่วงล่าสุดค่อนข้างคงที่";
        };
        int averageAqi = (int) Math.round(average(history));
        int minAqi = history.stream()
                .map(AirQualityDto::getAqiUs)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(safeInt(latest.getAqiUs()));
        int maxAqi = history.stream()
                .map(AirQualityDto::getAqiUs)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(safeInt(latest.getAqiUs()));
        String dominantPollutant = resolveDominantPollutant(history);
        String variability = resolveVariability(minAqi, maxAqi);
        String insightSeed = buildInsightSeed(trend, variability, dominantPollutant, latest);

        return new GuidanceContext(
                latest,
                history,
                trend,
                cautionGroup,
                actions,
                trendNote,
                trendNoteThai,
                averageAqi,
                minAqi,
                maxAqi,
                dominantPollutant,
                variability,
                insightSeed
        );
    }

    private java.util.Optional<String> generateWithGroq(GuidanceContext context) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String apiKey = requireNonBlank(groqApiKey, "groqApiKey");
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", defaultIfBlank(groqModel, "openai/gpt-oss-120b"));
            body.put("temperature", 0.2);
            body.put("max_tokens", 500);
            body.put("response_format", Map.of("type", "json_object"));
            body.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", """
                                    You write short Thai health guidance for an AQI LINE bot.
                                    Use only the provided AQI data, history summary, and risk summary.
                                    Do not diagnose, do not invent medical facts, and do not mention doctors unless symptoms are severe.
                                    Write every field in Thai only. Do not leave English phrases in the final response.
                                    Add a few suitable emojis naturally in the response, but keep it clean and not excessive.
                                    Return valid JSON only with keys:
                                    title, summary, insight, whoShouldBeCareful, actions, disclaimer.
                                    actions must be an array of 3 short Thai bullet-style strings without numbering.
                                    The summary should focus on today.
                                    The insight should briefly interpret the history pattern in a personal way.
                                    Keep the tone calm, practical, and easy to understand.
                                    """
                    ),
                    Map.of("role", "user", "content", buildPrompt(context))
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String apiUrl = defaultIfBlank(groqApiUrl, "https://api.groq.com/openai/v1/chat/completions");
            JsonNode response = restTemplate.postForObject(apiUrl, request, JsonNode.class);
            if (response == null) {
                return java.util.Optional.empty();
            }

            String content = response.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");

            if (content.isBlank()) {
                return java.util.Optional.empty();
            }

            JsonNode json = objectMapper.readTree(content);
            return java.util.Optional.of(formatReply(json, context));
        } catch (RestClientException | IOException ex) {
            log.warn("Groq health guideline fallback: {}", ex.getMessage());
            return java.util.Optional.empty();
        }
    }

    private String buildPrompt(GuidanceContext context) {
        StringBuilder historyBlock = new StringBuilder();
        int count = Math.min(context.history().size(), MAX_HISTORY_LIMIT);
        for (int i = 0; i < count; i++) {
            AirQualityDto item = context.history().get(i);
            historyBlock.append("- ")
                    .append(item.getFetchedAt())
                    .append(" | AQI ").append(item.getAqiUs())
                    .append(" | ").append(item.getAqiLevel())
                    .append(" | ").append(item.getMainPollutant())
                    .append("\n");
        }

        return """
                Create a short Thai health-guideline reply for a LINE chat.
                All fields in your answer must be Thai only.

                Latest AQI: %d
                Latest level: %s
                Main pollutant: %s
                Average AQI in recent history: %d
                Lowest AQI in recent history: %d
                Highest AQI in recent history: %d
                Trend: %s
                Variability: %s
                Dominant pollutant in history: %s
                Caution group: %s
                Insight seed: %s
                Rule-based actions:
                %s

                Recent history:
                %s

                Reminder:
                - Keep the answer short and human.
                - Focus on practical advice for today.
                - Make the insight feel based on the user's own history, not generic.
                - Mention sensitive groups when relevant.
                - Add a few suitable emojis naturally, but do not overuse them.
                - Add a short disclaimer that this is general information, not medical diagnosis.
                """.formatted(
                safeInt(context.latest().getAqiUs()),
                defaultText(context.latest().getAqiLevel(), "Unknown"),
                defaultText(context.latest().getMainPollutant(), "Unknown"),
                context.averageAqi(),
                context.minAqi(),
                context.maxAqi(),
                context.trend(),
                context.variability(),
                context.dominantPollutant(),
                context.cautionGroup(),
                context.insightSeed(),
                String.join("\n", context.actions()),
                historyBlock
        );
    }

    private String formatReply(JsonNode json, GuidanceContext context) {
        String title = ensureEmojiPrefix(textOrDefault(json, "title", "🌿 คำแนะนำสุขภาพวันนี้"), "🌿 ");
        String summary = textOrDefault(json, "summary", buildSummary(context));
        String insight = textOrDefault(json, "insight", context.insightSeed());
        String who = textOrDefault(json, "whoShouldBeCareful", context.cautionGroup());
        String disclaimer = textOrDefault(json, "disclaimer", "ℹ️ ข้อมูลนี้เป็นคำแนะนำทั่วไป ไม่ใช่การวินิจฉัยทางการแพทย์");

        List<String> actions = new ArrayList<>();
        JsonNode actionNode = json.path("actions");
        if (actionNode.isArray()) {
            for (JsonNode item : actionNode) {
                String text = sanitizeBullet(item.asText(""));
                if (!text.isBlank()) {
                    actions.add(text);
                }
            }
        }
        if (actions.isEmpty()) {
            actions = context.actions();
        }

        StringBuilder reply = new StringBuilder();
        reply.append(title).append("\n\n");
        reply.append(normalizeThaiText(summary)).append("\n\n");
        reply.append("📊 จากประวัติของคุณ: ").append(normalizeThaiText(insight)).append("\n\n");
        reply.append("⚠️ ควรระวัง: ").append(normalizeThaiText(who)).append("\n\n");
        reply.append("✅ วันนี้ควรทำ:\n");
        for (String action : actions.stream().limit(3).toList()) {
            reply.append("• ").append(sanitizeBullet(normalizeThaiText(action))).append("\n");
        }
        reply.append("\n📈 แนวโน้ม: ").append(context.trendNoteThai()).append("\n");
        reply.append("\n").append(normalizeThaiText(disclaimer));
        return reply.toString().trim();
    }

    private String buildFallbackText(GuidanceContext context) {
        StringBuilder reply = new StringBuilder();
        reply.append("คำแนะนำสุขภาพวันนี้!!").append("\n\n");
        reply.append(buildSummary(context)).append("\n\n");
        reply.append("📊 จากประวัติของคุณ: ").append("\n").append(context.insightSeed()).append("\n\n");
        reply.append("⚠️ ควรระวัง: ").append("\n").append(context.cautionGroup()).append("\n\n");
        reply.append("📍 วันนี้ควรทำ:\n");
        for (String action : context.actions()) {
            reply.append("• ").append(sanitizeBullet(action)).append("\n");
        }
        reply.append("\nแนวโน้ม: ").append(context.trendNoteThai()).append("\n");
        reply.append("\n** ข้อมูลนี้เป็นคำแนะนำทั่วไป ไม่ใช่การวินิจฉัยทางการแพทย์ **");
        return reply.toString().trim();
    }

    private String buildSummary(GuidanceContext context) {
        return "AQI ปัจจุบันอยู่ที่ %d ระดับ \"%s\" มลพิษหลักคือ %s และค่าเฉลี่ยช่วงล่าสุดอยู่ที่ประมาณ %d".formatted(
                safeInt(context.latest().getAqiUs()),
                toThaiAqiLevel(context.latest().getAqiLevel()),
                toThaiPollutantName(context.latest().getMainPollutant()),
                context.averageAqi()
        );
    }

    private @NonNull String resolveTrend(List<AirQualityDto> history) {
        if (history.size() < 4) {
            return "stable";
        }

        double recentAverage = average(history.subList(0, Math.min(3, history.size())));
        int olderStart = Math.max(history.size() - 3, 0);
        double olderAverage = average(history.subList(olderStart, history.size()));
        double diff = recentAverage - olderAverage;

        if (diff >= 8) {
            return "worsening";
        }
        if (diff <= -8) {
            return "improving";
        }
        return "stable";
    }

    private @NonNull String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private @NonNull String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private double average(List<AirQualityDto> items) {
        return items.stream()
                .map(AirQualityDto::getAqiUs)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
    }

    private String resolveDominantPollutant(List<AirQualityDto> history) {
        Map<String, Integer> counts = new HashMap<>();
        for (AirQualityDto item : history) {
            String pollutant = defaultText(item.getMainPollutant(), "Unknown");
            counts.put(pollutant, counts.getOrDefault(pollutant, 0) + 1);
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultText(history.get(0).getMainPollutant(), "Unknown"));
    }

    private String resolveVariability(int minAqi, int maxAqi) {
        int spread = maxAqi - minAqi;
        if (spread >= 40) {
            return "swinging";
        }
        if (spread >= 20) {
            return "moderately changing";
        }
        return "fairly steady";
    }

    private String buildInsightSeed(String trend, String variability, String dominantPollutant, AirQualityDto latest) {
        String pollutantText = toThaiPollutantName(defaultText(dominantPollutant, defaultText(latest.getMainPollutant(), "Unknown")));
        return switch (trend) {
            case "worsening" -> "ช่วงล่าสุดค่า AQI ของคุณมีแนวโน้มสูงขึ้น และ " + pollutantText + " เป็นมลพิษที่พบเด่นในประวัติ";
            case "improving" -> "ช่วงล่าสุดค่า AQI ของคุณเริ่มดีขึ้น แต่ยังควรระวัง " + pollutantText + " เป็นหลัก";
            default -> switch (variability) {
                case "swinging" -> "ค่า AQI ของคุณยังแกว่งพอสมควรในช่วงที่ผ่านมา และ " + pollutantText + " เป็นมลพิษที่พบเด่น";
                case "moderately changing" -> "ค่า AQI ของคุณค่อนข้างเปลี่ยนขึ้นลงเล็กน้อยในช่วงที่ผ่านมา โดย " + pollutantText + " พบค่อนข้างบ่อย";
                default -> "ค่า AQI ของคุณค่อนข้างนิ่งในช่วงที่ผ่านมา และ " + pollutantText + " เป็นมลพิษหลักที่เจอบ่อย";
            };
        };
    }

    private String resolveCautionGroup(Integer aqi) {
        if (aqi == null || aqi <= 50) {
            return "คนทั่วไปมักทำกิจกรรมได้ตามปกติ";
        }
        if (aqi <= 100) {
            return "ผู้ที่ไวต่อมลพิษควรสังเกตอาการเมื่อทำกิจกรรมกลางแจ้ง";
        }
        if (aqi <= 150) {
            return "เด็ก ผู้สูงอายุ และผู้มีโรคหัวใจหรือโรคปอดควรลดกิจกรรมกลางแจ้งหนัก ๆ";
        }
        if (aqi <= 200) {
            return "ทุกคนควรลดกิจกรรมกลางแจ้งหนัก ๆ โดยเฉพาะกลุ่มเสี่ยง";
        }
        return "ทุกคนควรหลีกเลี่ยงกิจกรรมกลางแจ้งให้น้อยที่สุด โดยเฉพาะกลุ่มเสี่ยง";
    }

    private List<String> resolveActions(Integer aqi, String pollutant, String trend) {
        List<String> actions = new ArrayList<>();

        if (aqi == null || aqi <= 50) {
            actions.add("ทำกิจกรรมกลางแจ้งได้ตามปกติ แต่ยังควรเช็กค่า AQI ต่อเนื่อง");
            actions.add("หากเป็นคนไวต่อมลพิษ ให้สังเกตอาการระคายเคืองหรือหายใจไม่สบาย");
            actions.add(trend.equals("worsening")
                    ? "แนวโน้มเริ่มแย่ลง ควรกลับมาตรวจอีกครั้งในภายหลัง"
                    : "ค่าอากาศยังอยู่ในระดับที่ค่อนข้างดี");
            return actions;
        }

        if (aqi <= 100) {
            actions.add("คนทั่วไปยังทำกิจกรรมได้ แต่ลดกิจกรรมกลางแจ้งหนัก ๆ ถ้ารู้สึกระคายเคือง");
            actions.add("ผู้ที่ไวต่อมลพิษควรพักเป็นระยะและสังเกตอาการผิดปกติ");
            actions.add("ถ้าค่าแนวโน้มสูงขึ้นในวันนี้ ควรติดตาม AQI อีกครั้งช่วงถัดไป");
            return actions;
        }

        if (aqi <= 150) {
            actions.add("กลุ่มเสี่ยงควรลดเวลาทำกิจกรรมกลางแจ้ง โดยเฉพาะกิจกรรมที่ใช้แรงมาก");
            actions.add("หากออกนอกอาคาร เลือกช่วงเวลาสั้นลงและพักบ่อยขึ้น");
            actions.add("ถ้าหายใจไม่สบาย ไอ หรือแสบตา ควรย้ายไปอยู่ในอาคาร");
            return actions;
        }

        if (aqi <= 200) {
            actions.add("ลดกิจกรรมกลางแจ้งหนัก ๆ และเลี่ยงการออกกำลังกายกลางแจ้ง");
            actions.add("ปิดหน้าต่างเมื่อทำได้ และอยู่ในที่อากาศถ่ายเทดีภายในอาคาร");
            actions.add("ถ้าจำเป็นต้องออกไปข้างนอก ให้ลดเวลาที่อยู่นอกอาคารให้น้อยลง");
            return actions;
        }

        actions.add("หลีกเลี่ยงกิจกรรมกลางแจ้งให้น้อยที่สุด โดยเฉพาะการออกแรงหนัก");
        actions.add("อยู่ในอาคารให้มากขึ้นและลดการเปิดรับอากาศภายนอกเมื่อทำได้");
        actions.add("หากมีอาการหายใจลำบาก เจ็บหน้าอก หรืออาการรุนแรง ควรขอความช่วยเหลือทางการแพทย์");
        if ("PM2.5".equalsIgnoreCase(defaultText(pollutant, ""))) {
            actions.set(1, "อยู่ในอาคารให้มากขึ้น และลดการเปิดรับฝุ่น PM2.5 จากภายนอกเมื่อทำได้");
        }
        return actions;
    }

    private String textOrDefault(JsonNode node, String key, String fallback) {
        String value = node.path(key).asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private String ensureEmojiPrefix(String text, String prefix) {
        if (text == null || text.isBlank()) {
            return prefix.trim();
        }
        return text.startsWith(prefix.trim()) ? text : prefix + text;
    }

    private String sanitizeBullet(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replaceFirst("^[•\\-\\*\\s]+", "")
                .replaceFirst("^[•\\-\\*\\s]+", "")
                .trim();
    }

    private String normalizeThaiText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                .replace("Recent readings are fairly stable.", "ค่าช่วงล่าสุดค่อนข้างคงที่")
                .replace("Recent readings are improving compared with your older records.", "ค่าช่วงล่าสุดดีขึ้นเมื่อเทียบกับค่าก่อนหน้า")
                .replace("Recent readings are getting worse compared with your older records.", "ค่าช่วงล่าสุดแย่ลงเมื่อเทียบกับค่าก่อนหน้า")
                .replace("Unhealthy for Sensitive Groups", "เริ่มมีผลต่อกลุ่มเสี่ยง")
                .replace("Very Unhealthy", "มีผลกระทบมากต่อสุขภาพ")
                .replace("Hazardous", "อันตรายต่อสุขภาพ")
                .replace("Unhealthy", "มีผลกระทบต่อสุขภาพ")
                .replace("Moderate", "ปานกลาง")
                .replace("Good", "ดี")
                .replace("PM2.5", "PM2.5")
                .trim();
    }

    private String toThaiAqiLevel(String aqiLevel) {
        if (aqiLevel == null || aqiLevel.isBlank()) {
            return "ไม่ทราบระดับ";
        }
        return switch (aqiLevel) {
            case "Good" -> "ดี";
            case "Moderate" -> "ปานกลาง";
            case "Unhealthy for Sensitive Groups" -> "เริ่มมีผลต่อกลุ่มเสี่ยง";
            case "Unhealthy" -> "มีผลกระทบต่อสุขภาพ";
            case "Very Unhealthy" -> "มีผลกระทบมากต่อสุขภาพ";
            case "Hazardous" -> "อันตรายต่อสุขภาพ";
            default -> aqiLevel;
        };
    }

    private String toThaiPollutantName(String pollutant) {
        if (pollutant == null || pollutant.isBlank()) {
            return "ไม่ทราบ";
        }
        return switch (pollutant) {
            case "PM2.5", "PM10", "NO2", "SO2", "CO" -> pollutant;
            case "Ozone (O3)" -> "โอโซน (O3)";
            case "Unknown" -> "ไม่ทราบ";
            default -> pollutant;
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Please use this feature in a direct chat with the bot.");
        }
        return userId.trim();
    }

    private record GuidanceContext(
            AirQualityDto latest,
            List<AirQualityDto> history,
            String trend,
            String cautionGroup,
            List<String> actions,
            String trendNote,
            String trendNoteThai,
            int averageAqi,
            int minAqi,
            int maxAqi,
            String dominantPollutant,
            String variability,
            String insightSeed
    ) {
    }
}
