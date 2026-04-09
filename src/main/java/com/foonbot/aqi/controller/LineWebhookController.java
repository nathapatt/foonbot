package com.foonbot.aqi.controller;

import com.foonbot.aqi.service.AirQualityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/line")
public class LineWebhookController {

    private final AirQualityService airQualityService;

    public LineWebhookController(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("events")) {
            return ResponseEntity.ok().build();
        }

        List<Map<String, Object>> events = (List<Map<String, Object>>) payload.get("events");

        for (Map<String, Object> event : events) {
            String type = (String) event.get("type");
            String replyToken = (String) event.get("replyToken");

            if ("message".equals(type) && event.containsKey("message")) {
                Map<String, Object> message = (Map<String, Object>) event.get("message");
                String messageType = (String) message.get("type");

                if ("text".equals(messageType)) {
                    String text = (String) message.get("text");

                    // ─────────────────────────────────────────────────────────────
                    // Action 1: "Check AQI"
                    // Instantly fetch real-time data and reply with Flex Message
                    // ─────────────────────────────────────────────────────────────
                    if ("Check AQI".equalsIgnoreCase(text)) {
                        try {
                            airQualityService.fetchAndReply(replyToken);
                        } catch (Exception e) {
                            System.err.println("Failed to fetch or reply: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // LINE requires a 200 OK response to confirm receipt.
        return ResponseEntity.ok().build();
    }
}
