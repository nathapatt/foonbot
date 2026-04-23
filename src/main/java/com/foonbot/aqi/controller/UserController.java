package com.foonbot.aqi.controller;

import com.foonbot.aqi.dtos.UpdateUserSettingsRequest;
import com.foonbot.aqi.dtos.UserSettingsDto;
import com.foonbot.aqi.security.AuthenticatedLineUserId;
import com.foonbot.aqi.service.AirQualityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AirQualityService airQualityService;

    public UserController(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    @GetMapping("/me/settings")
    public ResponseEntity<UserSettingsDto> getSettings(@AuthenticatedLineUserId String userId) {
        return ResponseEntity.ok(airQualityService.getUserSettings(userId));
    }

    @PutMapping("/me/settings")
    public ResponseEntity<UserSettingsDto> updateSettings(@AuthenticatedLineUserId String userId,
                                                          @RequestBody UpdateUserSettingsRequest request) {
        return ResponseEntity.ok(airQualityService.updateUserSettings(userId, request));
    }
}
