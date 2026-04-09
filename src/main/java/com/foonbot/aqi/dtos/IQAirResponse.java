package com.foonbot.aqi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the raw JSON response from IQAir API.
 *
 * Example: GET https://api.airvisual.com/v2/city?city=Nakhon+Nayok&state=Nakhon+Nayok&country=Thailand&key=...
 *
 * {
 *   "status": "success",
 *   "data": {
 *     "city": "...", "state": "...", "country": "...",
 *     "current": {
 *       "weather": { "tp": 33, "hu": 72, ... },
 *       "pollution": { "aqius": 85, "mainus": "p2", ... }
 *     }
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IQAirResponse {

    private String status;
    private Data data;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    // ─── Nested: data ─────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String city;
        private String state;
        private String country;
        private Current current;

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public Current getCurrent() { return current; }
        public void setCurrent(Current current) { this.current = current; }
    }

    // ─── Nested: data.current ─────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current {
        private Weather weather;
        private Pollution pollution;

        public Weather getWeather() { return weather; }
        public void setWeather(Weather weather) { this.weather = weather; }

        public Pollution getPollution() { return pollution; }
        public void setPollution(Pollution pollution) { this.pollution = pollution; }
    }

    // ─── Nested: data.current.weather ─────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        @JsonProperty("tp")
        private Double temperature;   // °C

        @JsonProperty("hu")
        private Integer humidity;     // %

        @JsonProperty("ws")
        private Double windSpeed;     // m/s

        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }

        public Integer getHumidity() { return humidity; }
        public void setHumidity(Integer humidity) { this.humidity = humidity; }

        public Double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    }

    // ─── Nested: data.current.pollution ───────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pollution {
        @JsonProperty("aqius")
        private Integer aqiUs;        // US AQI (EPA standard)

        @JsonProperty("mainus")
        private String mainPollutant; // p2=PM2.5, p1=PM10, o3=Ozone, n2=NO2, s2=SO2, co=CO

        public Integer getAqiUs() { return aqiUs; }
        public void setAqiUs(Integer aqiUs) { this.aqiUs = aqiUs; }

        public String getMainPollutant() { return mainPollutant; }
        public void setMainPollutant(String mainPollutant) { this.mainPollutant = mainPollutant; }
    }
}
