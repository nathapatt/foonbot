package com.foonbot.aqi.service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.foonbot.aqi.dtos.AirQualityDto;
import com.foonbot.aqi.model.AirQualityRecord;

@Service
public class LineMessagingService {

    private final RestTemplate restTemplate;

    @Value("${line.messaging.channel-token}")
    private String channelToken;

    @Value("${line.messaging.broadcast-url}")
    private String broadcastUrl;

    @Value("${line.messaging.reply-url}")
    private String replyUrl;

    @Value("${line.messaging.push-url}")
    private String pushUrl;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public LineMessagingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Broadcasts a Flex Message air quality card to all users who friended the bot.
     */
    public void sendNotification(AirQualityRecord record) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(channelToken, "LINE channel token must be set"));

        Map<String, Object> flexMessage = buildFlexMessage(record);

        Map<String, Object> body = new HashMap<>();
        body.put("messages", List.of(flexMessage));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = Objects.requireNonNull(broadcastUrl, "LINE broadcast URL must be set");

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("LINE Messaging API failed: "
                    + response.getStatusCode() + " — " + response.getBody());
        }
    }

    public void replyText(String replyToken, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(channelToken, "LINE channel token must be set"));

        Map<String, Object> textMessage = new LinkedHashMap<>();
        textMessage.put("type", "text");
        textMessage.put("text", text);

        Map<String, Object> body = new HashMap<>();
        body.put("replyToken", replyToken);
        body.put("messages", List.of(textMessage));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = Objects.requireNonNull(replyUrl, "LINE reply URL must be set");

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("LINE Reply API failed: "
                    + response.getStatusCode() + " — " + response.getBody());
        }
    }

    /**
     * Pushes a Flex Message air quality card to a specific LINE user by userId.
     * Used by LIFF flow where there is no replyToken.
     */
    public void pushNotification(String userId, AirQualityRecord record) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(channelToken, "LINE channel token must be set"));

        Map<String, Object> flexMessage = buildFlexMessage(record);

        Map<String, Object> body = new HashMap<>();
        body.put("to", userId);
        body.put("messages", List.of(flexMessage));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = Objects.requireNonNull(pushUrl, "LINE push URL must be set");

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("LINE Push API failed: "
                    + response.getStatusCode() + " — " + response.getBody());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Clean, Professional Flex Message Builder (No Emojis, Modern Weather App Look)
    // ─────────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildFlexMessage(AirQualityRecord r) {
        String aqiLevel     = AirQualityDto.resolveAqiLevel(r.getAqiUs());
        String pollutant    = AirQualityDto.resolvePollutantName(r.getMainPollutant());
        String themeColor   = resolveAqiHex(r.getAqiUs());
        String fetchedTime  = r.getFetchedAt() != null ? r.getFetchedAt().format(FORMATTER) : "N/A";

        // ── Header (City Name and Status) ─────────────────────────────────────────
        Map<String, Object> header = box("vertical", List.of(
                text("CURRENT AIR QUALITY", "xs", "bold", "#ffffffcc", null).margin("none").build(),
                text(r.getCity(), "xl", "bold", "#ffffff", null).margin("sm").build()
        ), themeColor, "20px").build();

        // ── Big AQI Display (Center) ──────────────────────────────────────────────
        Map<String, Object> mainAqiBox = box("vertical", List.of(
                text("US AQI", "sm", "bold", "#888888", "center").build(),
                text(String.valueOf(r.getAqiUs()), "5xl", "bold", themeColor, "center").build(),
                text(aqiLevel, "md", "bold", themeColor, "center").build()
        ), null, null).alignItems("center").build();

        // ── Stats Row (Temperature, Humidity, Pollutant) ─────────────────────────
        Map<String, Object> statsRow = box("horizontal", List.of(
                statColumn("Temp", String.format("%.1f°C", r.getTemperature())),
                statColumn("Humidity", r.getHumidity() + "%"),
                statColumn("Pollutant", pollutant)
        ), null, null).margin("xl").build();

        // ── Body ─────────────────────────────────────────────────────────────────
        Map<String, Object> body = box("vertical", List.of(
                mainAqiBox,
                separator("xl"),
                statsRow
        ), null, "20px").build();

        // ── Footer (Timestamp) ───────────────────────────────────────────────────
        Map<String, Object> footer = box("vertical", List.of(
                text("Updated at " + fetchedTime, "xs", "regular", "#b0b0b0", "center").build()
        ), null, "10px").build();

        // ── Bubble Container ─────────────────────────────────────────────────────
        Map<String, Object> bubble = new LinkedHashMap<>();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo"); // Cleaner, taller size
        bubble.put("header", header);
        bubble.put("body", body);
        bubble.put("footer", footer);

        // ── Flex Envelope ────────────────────────────────────────────────────────
        Map<String, Object> flexMsg = new LinkedHashMap<>();
        flexMsg.put("type", "flex");
        flexMsg.put("altText", "Air Quality in " + r.getCity() + ": AQI " + r.getAqiUs() + " (" + aqiLevel + ")");
        flexMsg.put("contents", bubble);
        return flexMsg;
    }

    // ─── Component Helpers ────────────────────────────────────────────────────────

    /** A box (layout container) wrapper for cleaner syntax. */
    private BoxBuilder box(String layout, List<Object> contents, String backgroundColor, String paddingAll) {
        return new BoxBuilder(layout, contents, backgroundColor, paddingAll);
    }

    /** A text component wrapper for cleaner syntax. */
    private TextBuilder text(String text, String size, String weight, String color, String align) {
        return new TextBuilder(text, size, weight, color, align);
    }

    /** A vertical column for a single stat symbol. */
    private Map<String, Object> statColumn(String label, String value) {
        return box("vertical", List.of(
                text(label, "xs", "regular", "#888888", "center").build(),
                text(value, "sm", "bold", "#333333", "center").margin("sm").build()
        ), null, null).build();
    }

    /** A horizontal separator line. */
    private Map<String, Object> separator(String margin) {
        Map<String, Object> sep = new LinkedHashMap<>();
        sep.put("type", "separator");
        if (margin != null) sep.put("margin", margin);
        return sep;
    }

    // ─── Builder Classes for Fluent API ──────────────────────────────────────────

    private static class BoxBuilder {
        private final Map<String, Object> m = new LinkedHashMap<>();
        BoxBuilder(String layout, List<Object> contents, String bg, String pad) {
            m.put("type", "box");
            m.put("layout", layout);
            m.put("contents", contents);
            if (bg != null) m.put("backgroundColor", bg);
            if (pad != null) m.put("paddingAll", pad);
        }
        BoxBuilder margin(String margin) { m.put("margin", margin); return this; }
        BoxBuilder alignItems(String align) { m.put("alignItems", align); return this; }
        Map<String, Object> build() { return m; }
    }

    private static class TextBuilder {
        private final Map<String, Object> m = new LinkedHashMap<>();
        TextBuilder(String text, String size, String weight, String color, String align) {
            m.put("type", "text");
            m.put("text", text);
            m.put("size", size);
            m.put("weight", weight);
            m.put("color", color);
            m.put("wrap", true);
            if (align != null) m.put("align", align);
        }
        TextBuilder margin(String margin) { m.put("margin", margin); return this; }
        Map<String, Object> build() { return m; }
    }

    // ─── AQI Theme Colors ────────────────────────────────────────────────────────

    /**
     * Maps AQI index to the official U.S. AQI colors.
     * IQAir states it uses the U.S. EPA AQI standard for AQI color coding.
     */
    private String resolveAqiHex(Integer aqi) {
        if (aqi == null)  return "#9E9E9E";
        if (aqi <= 50)    return "#ACD060"; // Good
        if (aqi <= 100)   return "#F6D55F"; // Moderate
        if (aqi <= 150)   return "#FC9A54"; // Unhealthy for Sensitive Groups
        if (aqi <= 200)   return "#F6686C"; // Unhealthy
        if (aqi <= 300)   return "#A57EBB"; // Very Unhealthy
        return "#A07785";                   // Hazardous
    }
}
