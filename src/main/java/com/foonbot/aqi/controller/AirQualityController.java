package com.foonbot.aqi.controller;

import com.foonbot.aqi.dtos.AirQualityDto;
import com.foonbot.aqi.service.AirQualityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
