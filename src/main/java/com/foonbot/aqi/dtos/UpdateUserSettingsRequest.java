package com.foonbot.aqi.dtos;

public class UpdateUserSettingsRequest {

    private Boolean notifyEnabled;
    private String notifyTime;
    private String timezone;

    public Boolean getNotifyEnabled() {
        return notifyEnabled;
    }

    public void setNotifyEnabled(Boolean notifyEnabled) {
        this.notifyEnabled = notifyEnabled;
    }

    public String getNotifyTime() {
        return notifyTime;
    }

    public void setNotifyTime(String notifyTime) {
        this.notifyTime = notifyTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
