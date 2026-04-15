package com.foonbot.aqi.dtos;

import com.foonbot.aqi.model.LineUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserSettingsDto {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String userId;
    private final Boolean notifyEnabled;
    private final String notifyTime;
    private final String timezone;
    private final Double lastLat;
    private final Double lastLon;
    private final LocalDateTime lastLocationAt;

    public UserSettingsDto(LineUser user) {
        this.userId = user.getLineUserId();
        this.notifyEnabled = user.getNotifyEnabled();
        this.notifyTime = user.getNotifyTime() != null ? user.getNotifyTime().format(TIME_FORMATTER) : null;
        this.timezone = user.getTimezone();
        this.lastLat = user.getLastLat();
        this.lastLon = user.getLastLon();
        this.lastLocationAt = user.getLastLocationAt();
    }

    public String getUserId() {
        return userId;
    }

    public Boolean getNotifyEnabled() {
        return notifyEnabled;
    }

    public String getNotifyTime() {
        return notifyTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public Double getLastLat() {
        return lastLat;
    }

    public Double getLastLon() {
        return lastLon;
    }

    public LocalDateTime getLastLocationAt() {
        return lastLocationAt;
    }
}
