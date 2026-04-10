package com.foonbot.aqi.service;

import com.foonbot.aqi.dtos.IQAirResponse;
import com.foonbot.aqi.model.AirQualityRecord;
import com.foonbot.aqi.repository.AirQualityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class IQAirService {

    private final RestTemplate restTemplate;
    private final AirQualityRepository repository;

    @Value("${iqair.api.key}")
    private String apiKey;

    @Value("${iqair.city}")
    private String city;

    @Value("${iqair.state}")
    private String state;

    @Value("${iqair.country}")
    private String country;

    public IQAirService(RestTemplate restTemplate, AirQualityRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    /**
     * Fetches current air quality from IQAir API for the configured city,
     * maps the response to an entity, saves it to the database, and returns it.
     */
    public AirQualityRecord fetchAndSave() {
        java.net.URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("api.airvisual.com")
                .path("/v2/city")
                .queryParam("city", city)
                .queryParam("state", state)
                .queryParam("country", country)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        IQAirResponse response = restTemplate.getForObject(uri, IQAirResponse.class);

        if (response == null || !"success".equals(response.getStatus())) {
            throw new RuntimeException("IQAir API returned an error: " +
                    (response != null ? response.getStatus() : "null response"));
        }

        IQAirResponse.Data data = response.getData();
        IQAirResponse.Pollution pollution = data.getCurrent().getPollution();
        IQAirResponse.Weather weather = data.getCurrent().getWeather();

        AirQualityRecord record = new AirQualityRecord(
                data.getCity(),
                data.getState(),
                data.getCountry(),
                pollution.getAqiUs(),
                pollution.getMainPollutant(),
                weather.getTemperature(),
                weather.getHumidity()
        );

        return repository.save(record);
    }

    /**
     * Fetches air quality from IQAir using GPS coordinates (nearest city lookup).
     * Used when the user shares location via LIFF.
     */
    public AirQualityRecord fetchAndSaveByLocation(double lat, double lon) {
        java.net.URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("api.airvisual.com")
                .path("/v2/nearest_city")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        IQAirResponse response = restTemplate.getForObject(uri, IQAirResponse.class);

        if (response == null || !"success".equals(response.getStatus())) {
            throw new RuntimeException("IQAir API returned an error: " +
                    (response != null ? response.getStatus() : "null response"));
        }

        IQAirResponse.Data data = response.getData();
        IQAirResponse.Pollution pollution = data.getCurrent().getPollution();
        IQAirResponse.Weather weather = data.getCurrent().getWeather();

        AirQualityRecord record = new AirQualityRecord(
                data.getCity(),
                data.getState(),
                data.getCountry(),
                pollution.getAqiUs(),
                pollution.getMainPollutant(),
                weather.getTemperature(),
                weather.getHumidity()
        );

        return repository.save(record);
    }
}
