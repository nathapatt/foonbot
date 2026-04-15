package com.foonbot.aqi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "line_users")
public class LineUser {

    @Id
    @Column(name = "line_user_id", nullable = false, updatable = false)
    private String lineUserId;

    @Column(name = "last_lat")
    private Double lastLat;

    @Column(name = "last_lon")
    private Double lastLon;

    @Column(name = "last_location_at")
    private LocalDateTime lastLocationAt;

    @Column(name = "notify_enabled", nullable = false)
    private Boolean notifyEnabled = true;

    @Column(name = "notify_time", nullable = false)
    private LocalTime notifyTime = LocalTime.of(7, 0);

    @Column(name = "timezone", nullable = false)
    private String timezone = "Asia/Bangkok";

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LineUser() {
    }

    public LineUser(String lineUserId) {
        this.lineUserId = lineUserId;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.notifyEnabled == null) {
            this.notifyEnabled = true;
        }
        if (this.notifyTime == null) {
            this.notifyTime = LocalTime.of(7, 0);
        }
        if (this.timezone == null || this.timezone.isBlank()) {
            this.timezone = "Asia/Bangkok";
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getLineUserId() {
        return lineUserId;
    }

    public void setLineUserId(String lineUserId) {
        this.lineUserId = lineUserId;
    }

    public Double getLastLat() {
        return lastLat;
    }

    public void setLastLat(Double lastLat) {
        this.lastLat = lastLat;
    }

    public Double getLastLon() {
        return lastLon;
    }

    public void setLastLon(Double lastLon) {
        this.lastLon = lastLon;
    }

    public LocalDateTime getLastLocationAt() {
        return lastLocationAt;
    }

    public void setLastLocationAt(LocalDateTime lastLocationAt) {
        this.lastLocationAt = lastLocationAt;
    }

    public Boolean getNotifyEnabled() {
        return notifyEnabled;
    }

    public void setNotifyEnabled(Boolean notifyEnabled) {
        this.notifyEnabled = notifyEnabled;
    }

    public LocalTime getNotifyTime() {
        return notifyTime;
    }

    public void setNotifyTime(LocalTime notifyTime) {
        this.notifyTime = notifyTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public LocalDateTime getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(LocalDateTime lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
