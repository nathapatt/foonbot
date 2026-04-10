package com.foonbot.aqi.controller;

import com.foonbot.aqi.dtos.AirQualityDto;
import com.foonbot.aqi.service.AirQualityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/air-quality")
public class AirQualityController {

    private final AirQualityService airQualityService;

    public AirQualityController(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    /**
     * Manually trigger: fetch air quality from IQAir + send LINE notification.
     * GET /api/air-quality/notify
     */
    @GetMapping("/notify")
    public ResponseEntity<AirQualityDto> triggerNotify() {
        AirQualityDto result = airQualityService.fetchAndNotify();
        return ResponseEntity.ok(result);
    }

    /**
     * Fetch air quality from IQAir + save to DB — NO LINE notification.
     * Use this to test IQAir without LINE.
     * GET /api/air-quality/fetch
     */
    @GetMapping("/fetch")
    public ResponseEntity<AirQualityDto> fetchOnly() {
        AirQualityDto result = airQualityService.fetchOnly();
        return ResponseEntity.ok(result);
    }

    /**
     * Get the single most recent air quality record from DB.
     * GET /api/air-quality/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<AirQualityDto> latest() {
        AirQualityDto result = airQualityService.getLatest();
        return ResponseEntity.ok(result);
    }

    /**
     * Get the last 10 air quality records from DB (newest first).
     * GET /api/air-quality/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<AirQualityDto>> history() {
        List<AirQualityDto> result = airQualityService.getHistory();
        return ResponseEntity.ok(result);
    }

    /**
     * Called by the LIFF page with the user's GPS location.
     * Fetches nearest city AQI and pushes a Flex Message to the LINE user.
     * POST /api/air-quality/by-location
     * Body: { "userId": "...", "lat": 13.75, "lon": 100.50 }
     */
    @PostMapping("/by-location")
    public ResponseEntity<AirQualityDto> byLocation(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        double lat    = ((Number) body.get("lat")).doubleValue();
        double lon    = ((Number) body.get("lon")).doubleValue();

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AirQualityDto result = airQualityService.fetchAndPushByLocation(userId, lat, lon);
        return ResponseEntity.ok(result);
    }
}
