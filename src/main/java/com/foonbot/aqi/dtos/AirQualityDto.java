package com.foonbot.aqi.dtos;

import com.foonbot.aqi.model.AirQualityRecord;
import java.time.LocalDateTime;

/**
 * Clean output DTO returned by the controller.
 * Converts raw AQI code into a human-readable level.
 */
public class AirQualityDto {

    private final Long id;
    private final String userId;
    private final String city;
    private final String state;
    private final String country;
    private final Integer aqiUs;
    private final String aqiLevel;       // Good / Moderate / Unhealthy / etc.
    private final String mainPollutant;  // Human-readable: PM2.5, PM10, Ozone, etc.
    private final Double temperature;
    private final Integer humidity;
    private final LocalDateTime fetchedAt;
    private final Boolean notifiedLine;

    // ─── Constructor from entity ───────────────────────────────────────────────

    public AirQualityDto(AirQualityRecord record) {
        this.id = record.getId();
        this.userId = record.getLineUser() != null ? record.getLineUser().getLineUserId() : null;
        this.city = record.getCity();
        this.state = record.getState();
        this.country = record.getCountry();
        this.aqiUs = record.getAqiUs();
        this.aqiLevel = resolveAqiLevel(record.getAqiUs());
        this.mainPollutant = resolvePollutantName(record.getMainPollutant());
        this.temperature = record.getTemperature();
        this.humidity = record.getHumidity();
        this.fetchedAt = record.getFetchedAt();
        this.notifiedLine = record.getNotifiedLine();
    }

    // ─── AQI Level (US EPA standard) ──────────────────────────────────────────

    public static String resolveAqiLevel(Integer aqi) {
        if (aqi == null) return "Unknown";
        if (aqi <= 50)  return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    // ─── Pollutant code → readable name ───────────────────────────────────────

    public static String resolvePollutantName(String code) {
        if (code == null) return "Unknown";
        return switch (code) {
            case "p2" -> "PM2.5";
            case "p1" -> "PM10";
            case "o3" -> "Ozone (O3)";
            case "n2" -> "NO2";
            case "s2" -> "SO2";
            case "co" -> "CO";
            default   -> code;
        };
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public Integer getAqiUs() { return aqiUs; }
    public String getAqiLevel() { return aqiLevel; }
    public String getMainPollutant() { return mainPollutant; }
    public Double getTemperature() { return temperature; }
    public Integer getHumidity() { return humidity; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public Boolean getNotifiedLine() { return notifiedLine; }
}
