package com.foonbot.aqi.service;

import com.foonbot.aqi.dtos.AirQualityDto;
import com.foonbot.aqi.model.AirQualityRecord;
import com.foonbot.aqi.repository.AirQualityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AirQualityService {

    private final IQAirService iqAirService;
    private final LineMessagingService lineMessagingService;
    private final AirQualityRepository repository;

    public AirQualityService(IQAirService iqAirService,
                             LineMessagingService lineMessagingService,
                             AirQualityRepository repository) {
        this.iqAirService = iqAirService;
        this.lineMessagingService = lineMessagingService;
        this.repository = repository;
    }

    /**
     * Fetches latest air quality, saves to DB, sends LINE notification,
     * and returns the result as a DTO.
     */
    public AirQualityDto fetchAndNotify() {
        // 1. Fetch from IQAir and save to DB
        AirQualityRecord record = iqAirService.fetchAndSave();

        // 2. Send LINE notification
        lineMessagingService.sendNotification(record);

        // 3. Mark record as notified and update in DB
        record.setNotifiedLine(true);
        repository.save(record);

        // 4. Return clean DTO to caller
        return new AirQualityDto(record);
    }

    /**
     * Fetches latest air quality, saves to DB, replies to a specific LINE user,
     * and returns the result as a DTO.
     */
    public AirQualityDto fetchAndReply(String replyToken) {
        // 1. Fetch from IQAir and save to DB
        AirQualityRecord record = iqAirService.fetchAndSave();

        // 2. Reply to the specific user via LINE
        lineMessagingService.replyNotification(replyToken, record);

        // 3. Mark record as notified and update in DB
        record.setNotifiedLine(true);
        repository.save(record);

        // 4. Return clean DTO to caller
        return new AirQualityDto(record);
    }

    /**
     * Fetches air quality based on GPS coordinates from LIFF,
     * then pushes the result to a specific LINE user by userId.
     */
    public AirQualityDto fetchAndPushByLocation(String userId, double lat, double lon) {
        // 1. Fetch nearest city AQI from IQAir using GPS coordinates
        AirQualityRecord record = iqAirService.fetchAndSaveByLocation(lat, lon);

        // 2. Push result to the LINE user (no replyToken needed)
        lineMessagingService.pushNotification(userId, record);

        // 3. Mark record as notified and update in DB
        record.setNotifiedLine(true);
        repository.save(record);

        // 4. Return clean DTO to caller
        return new AirQualityDto(record);
    }

    /**
     * Fetches latest air quality and saves to DB — does NOT send LINE notification.
     * Use this to test IQAir integration without LINE.
     */
    public AirQualityDto fetchOnly() {
        AirQualityRecord record = iqAirService.fetchAndSave();
        return new AirQualityDto(record);
    }

    /**
     * Returns the 10 most recent air quality records from DB.
     */
    public List<AirQualityDto> getHistory() {
        return repository.findTop10ByOrderByFetchedAtDesc()
                .stream()
                .map(AirQualityDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the single most recent record from DB.
     */
    public AirQualityDto getLatest() {
        AirQualityRecord record = repository.findTopByOrderByFetchedAtDesc();
        if (record == null) {
            throw new RuntimeException("No air quality records found yet. Try calling /notify first.");
        }
        return new AirQualityDto(record);
    }
}
