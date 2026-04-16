package com.foonbot.aqi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.foonbot.aqi.service.AirQualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/line")
public class LineWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);

    private final AirQualityService airQualityService;

    public LineWebhookController(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    /**
     * Handles text commands that are meant to be answered directly in LINE chat.
     * AQI checking itself is handled through the LIFF location flow, not this webhook.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody JsonNode payload) {
        JsonNode events = payload.path("events");
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
