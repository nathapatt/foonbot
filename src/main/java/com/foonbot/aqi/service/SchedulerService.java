package com.foonbot.aqi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final AirQualityService airQualityService;

    public SchedulerService(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    /**
     * Runs every minute and sends AQI only to users due at their configured local time.
     */
    @Scheduled(cron = "0 * * * * *")
    public void userScheduleNotify() {
        try {
            AirQualityService.ScheduledDispatchResult result = airQualityService.dispatchDueUserNotifications();
            if (result.sent() > 0 || result.failed() > 0) {
                log.info("Scheduler run complete. Candidates: {}, sent: {}, failed: {}",
                        result.candidates(), result.sent(), result.failed());
            }
        } catch (Exception e) {
            log.error("Scheduler dispatch failed: {}", e.getMessage());
        }
    }
}
