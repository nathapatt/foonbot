package com.foonbot.aqi.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class ByLocationRequest {

    @NotNull(message = "lat is required")
    @DecimalMin(value = "-90.0", message = "lat must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "lat must be between -90 and 90")
    private Double lat;

    @NotNull(message = "lon is required")
    @DecimalMin(value = "-180.0", message = "lon must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "lon must be between -180 and 180")
    private Double lon;

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }
}
