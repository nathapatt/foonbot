package com.foonbot.aqi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foonbot.aqi.security.LineWebhookSignatureService;
import com.foonbot.aqi.service.AirQualityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/line")
public class LineWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);

    private final AirQualityService airQualityService;
    private final LineWebhookSignatureService lineWebhookSignatureService;
    private final ObjectMapper objectMapper;

    public LineWebhookController(AirQualityService airQualityService,
                                 LineWebhookSignatureService lineWebhookSignatureService,
                                 ObjectMapper objectMapper) {
        this.airQualityService = airQualityService;
        this.lineWebhookSignatureService = lineWebhookSignatureService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles text commands that are meant to be answered directly in LINE chat.
     * AQI checking itself is handled through the LIFF location flow, not this webhook.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(name = "X-Line-Signature", required = false) String signature,
            @RequestBody String payload) {
        if (!lineWebhookSignatureService.isConfigured()) {
            log.error("LINE webhook secret is not configured");
            return ResponseEntity.status(503).build();
        }

        if (!lineWebhookSignatureService.isValidSignature(payload, signature)) {
            log.warn("Rejected LINE webhook with invalid signature");
            return ResponseEntity.status(401).build();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Invalid LINE webhook payload: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        }

        JsonNode events = root.path("events");
        if (!events.isArray()) {
            return ResponseEntity.ok().build();
        }

        for (JsonNode event : events) {
            String type = event.path("type").asText();
            String replyToken = event.path("replyToken").asText(null);
            String userId = event.path("source").path("userId").asText(null);

            if ("message".equals(type)) {
                JsonNode message = event.path("message");
                String messageType = message.path("type").asText();

                if ("text".equals(messageType)) {
                    String text = message.path("text").asText("");

                    if (isHealthGuidelineCommand(text)) {
                        try {
                            airQualityService.replyHealthGuidelineText(replyToken, userId);
                        } catch (Exception e) {
                            log.error("Failed to generate LINE health guideline: {}", e.getMessage(), e);
                        }
                    }
                }
            }
        }

        // LINE requires a 200 OK response to confirm receipt.
        return ResponseEntity.ok().build();
    }

    private boolean isHealthGuidelineCommand(String text) {
        if (text == null) {
            return false;
        }

        String normalized = text.trim().toLowerCase();
        return normalized.equals("health guideline")
                || normalized.equals("health guidelines")
                || normalized.equals("คำแนะนำสุขภาพ");
    }
}
