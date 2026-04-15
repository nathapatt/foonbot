package com.foonbot.aqi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "air_quality_records")
public class AirQualityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country;

    // US AQI value (EPA standard)
    @Column(name = "aqi_us")
    private Integer aqiUs;

    // Main pollutant code: p2 = PM2.5, p1 = PM10, o3 = Ozone, etc.
    @Column(name = "main_pollutant")
    private String mainPollutant;

    // Temperature in Celsius
    @Column(name = "temperature")
    private Double temperature;

    // Humidity percentage
    @Column(name = "humidity")
    private Integer humidity;

    // When this record was fetched from IQAir
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    // Whether LINE Notify was sent for this record
    @Column(name = "notified_line")
    private Boolean notifiedLine = false;

    @ManyToOne
    @JoinColumn(name = "line_user_id")
    private LineUser lineUser;

    // ─── Constructors ──────────────────────────────────────────────────────────

    public AirQualityRecord() {}

    public AirQualityRecord(String city, String state, String country,
                            Integer aqiUs, String mainPollutant,
                            Double temperature, Integer humidity) {
        this.city = city;
        this.state = state;
        this.country = country;
        this.aqiUs = aqiUs;
        this.mainPollutant = mainPollutant;
        this.temperature = temperature;
        this.humidity = humidity;
        this.fetchedAt = LocalDateTime.now();
        this.notifiedLine = false;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Integer getAqiUs() { return aqiUs; }
    public void setAqiUs(Integer aqiUs) { this.aqiUs = aqiUs; }

    public String getMainPollutant() { return mainPollutant; }
    public void setMainPollutant(String mainPollutant) { this.mainPollutant = mainPollutant; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getHumidity() { return humidity; }
    public void setHumidity(Integer humidity) { this.humidity = humidity; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }

    public Boolean getNotifiedLine() { return notifiedLine; }
    public void setNotifiedLine(Boolean notifiedLine) { this.notifiedLine = notifiedLine; }

    public LineUser getLineUser() { return lineUser; }
    public void setLineUser(LineUser lineUser) { this.lineUser = lineUser; }
}
