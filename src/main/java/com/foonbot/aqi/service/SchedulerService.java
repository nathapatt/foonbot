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
     * Runs every day at 7:00 AM (Asia/Bangkok = UTC+7)
     * Cron format: second  minute  hour  day  month  weekday
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Bangkok")
    public void morningNotify() {
        log.info("⏰ [Scheduler] 7:00 AM — Running morning air quality check...");
        try {
            var result = airQualityService.fetchAndNotify();
            log.info("✅ Morning notification sent. AQI: {} ({})", result.getAqiUs(), result.getAqiLevel());
        } catch (Exception e) {
            log.error("❌ Morning notification failed: {}", e.getMessage());
        }
    }

    /**
     * Runs every day at 6:00 PM (Asia/Bangkok = UTC+7)
     */
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Bangkok")
    public void eveningNotify() {
        log.info("⏰ [Scheduler] 6:00 PM — Running evening air quality check...");
        try {
            var result = airQualityService.fetchAndNotify();
            log.info("✅ Evening notification sent. AQI: {} ({})", result.getAqiUs(), result.getAqiLevel());
        } catch (Exception e) {
            log.error("❌ Evening notification failed: {}", e.getMessage());
        }
    }
}
