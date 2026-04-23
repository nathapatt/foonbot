package com.foonbot.aqi.dtos;

import com.foonbot.aqi.model.LineUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserSettingsDto {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Boolean notifyEnabled;
    private final String notifyTime;
    private final String timezone;
    private final Boolean hasLocationSaved;
    private final LocalDateTime lastLocationAt;

    public UserSettingsDto(LineUser user) {
        this.notifyEnabled = user.getNotifyEnabled();
        this.notifyTime = user.getNotifyTime() != null ? user.getNotifyTime().format(TIME_FORMATTER) : null;
        this.timezone = user.getTimezone();
        this.hasLocationSaved = user.getLastLat() != null && user.getLastLon() != null;
        this.lastLocationAt = user.getLastLocationAt();
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

    public Boolean getHasLocationSaved() {
        return hasLocationSaved;
    }

    public LocalDateTime getLastLocationAt() {
        return lastLocationAt;
    }
}
