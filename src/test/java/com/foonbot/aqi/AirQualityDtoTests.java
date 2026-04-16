package com.foonbot.aqi;

import com.foonbot.aqi.dtos.AirQualityDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AirQualityDtoTests {

    @Test
    void resolvesAqiLevels() {
        assertEquals("Good", AirQualityDto.resolveAqiLevel(40));
        assertEquals("Moderate", AirQualityDto.resolveAqiLevel(80));
        assertEquals("Unhealthy for Sensitive Groups", AirQualityDto.resolveAqiLevel(120));
        assertEquals("Unhealthy", AirQualityDto.resolveAqiLevel(180));
    }

    @Test
    void resolvesPollutantNames() {
        assertEquals("PM2.5", AirQualityDto.resolvePollutantName("p2"));
        assertEquals("PM10", AirQualityDto.resolvePollutantName("p1"));
        assertEquals("Ozone (O3)", AirQualityDto.resolvePollutantName("o3"));
    }
}
