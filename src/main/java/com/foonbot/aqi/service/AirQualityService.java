package com.foonbot.aqi.service;

import com.foonbot.aqi.dtos.AirQualityDto;
import com.foonbot.aqi.dtos.UpdateUserSettingsRequest;
import com.foonbot.aqi.dtos.UserSettingsDto;
import com.foonbot.aqi.model.AirQualityRecord;
import com.foonbot.aqi.model.LineUser;
import com.foonbot.aqi.repository.AirQualityRepository;
import com.foonbot.aqi.repository.LineUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityService.class);

    private static final LocalTime DEFAULT_NOTIFY_TIME = LocalTime.of(7, 0);
    private static final String DEFAULT_TIMEZONE = "Asia/Bangkok";
    private static final int DEFAULT_HISTORY_LIMIT = 30;
    private static final int MAX_HISTORY_LIMIT = 200;
    private static final int SCHEDULE_BATCH_SIZE = 200;

    private final IQAirService iqAirService;
    private final LineMessagingService lineMessagingService;
    private final HealthGuidelineService healthGuidelineService;
    private final AirQualityRepository repository;
    private final LineUserRepository lineUserRepository;

    public AirQualityService(IQAirService iqAirService,
                             LineMessagingService lineMessagingService,
                             HealthGuidelineService healthGuidelineService,
                             AirQualityRepository repository,
                             LineUserRepository lineUserRepository) {
        this.iqAirService = iqAirService;
        this.lineMessagingService = lineMessagingService;
        this.healthGuidelineService = healthGuidelineService;
        this.repository = repository;
        this.lineUserRepository = lineUserRepository;
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
        // 0. Upsert user and latest location
        LineUser user = getOrCreateUser(userId);
        user.setLastLat(lat);
        user.setLastLon(lon);
        user.setLastLocationAt(LocalDateTime.now());
        lineUserRepository.save(user);

        // 1. Fetch nearest city AQI from IQAir using GPS coordinates
        AirQualityRecord record = iqAirService.fetchAndSaveByLocation(lat, lon);
        record.setLineUser(user);

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

    public UserSettingsDto getUserSettings(String userId) {
        LineUser user = getOrCreateUser(userId);
        return new UserSettingsDto(user);
    }

    public UserSettingsDto updateUserSettings(UpdateUserSettingsRequest request) {
        String userId = requireUserId(request.getUserId());

        LineUser user = getOrCreateUser(userId);

        boolean hasLocation = user.getLastLat() != null && user.getLastLon() != null;
        boolean wantsScheduleActive = request.getNotifyEnabled() == null || Boolean.TRUE.equals(request.getNotifyEnabled());
        if (!hasLocation && wantsScheduleActive) {
            throw new IllegalArgumentException("Please check AQI once to share location before enabling schedule");
        }

        if (request.getNotifyEnabled() != null) {
            user.setNotifyEnabled(request.getNotifyEnabled());
        }

        if (request.getNotifyTime() != null && !request.getNotifyTime().isBlank()) {
            try {
                user.setNotifyTime(LocalTime.parse(request.getNotifyTime().trim()));
            } catch (DateTimeException ex) {
                throw new IllegalArgumentException("Invalid notifyTime format. Use HH:mm");
            }
        }

        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            String timezone = request.getTimezone().trim();
            try {
                ZoneId.of(timezone);
                user.setTimezone(timezone);
            } catch (DateTimeException ex) {
                throw new IllegalArgumentException("Invalid timezone. Example: Asia/Bangkok");
            }
        }

        LineUser updated = lineUserRepository.save(user);
        return new UserSettingsDto(updated);
    }

    public List<AirQualityDto> getUserHistory(String userId, Integer limit) {
        String safeUserId = requireUserId(userId);
        int safeLimit = resolveHistoryLimit(limit);

        return repository.findByLineUserLineUserIdOrderByFetchedAtDesc(
                        safeUserId,
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "fetchedAt"))
                )
                .stream()
                .map(AirQualityDto::new)
                .collect(Collectors.toList());
    }

    public void replyHealthGuideline(String replyToken, String userId) {
        try {
            String guideline = healthGuidelineService.generateGuidelineText(userId);
            lineMessagingService.replyText(replyToken, guideline);
        } catch (IllegalArgumentException ex) {
            lineMessagingService.replyText(replyToken, ex.getMessage());
        }
    }

    public ScheduledDispatchResult dispatchDueUserNotifications() {
        int candidates = 0;
        int sent = 0;
        int failed = 0;

        Page<LineUser> page;
        int pageIndex = 0;

        do {
            page = lineUserRepository.findByNotifyEnabledTrueAndLastLatIsNotNullAndLastLonIsNotNull(
                    PageRequest.of(pageIndex, SCHEDULE_BATCH_SIZE)
            );

            for (LineUser user : page.getContent()) {
                candidates++;

                if (!isDueNow(user)) {
                    continue;
                }

                try {
                    sendScheduledNotification(user);
                    sent++;
                } catch (Exception ex) {
                    failed++;
                    log.error("Scheduled AQI push failed for user {}: {}", user.getLineUserId(), ex.getMessage());
                }
            }

            pageIndex++;
        } while (page.hasNext());

        return new ScheduledDispatchResult(candidates, sent, failed);
    }

    private void sendScheduledNotification(LineUser user) {
        AirQualityRecord record = iqAirService.fetchAndSaveByLocation(user.getLastLat(), user.getLastLon());
        record.setLineUser(user);

        lineMessagingService.pushNotification(user.getLineUserId(), record);

        record.setNotifiedLine(true);
        repository.save(record);

        ZoneId zone = resolveZoneOrDefault(user.getTimezone());
        user.setLastNotifiedAt(LocalDateTime.now(zone));
        lineUserRepository.save(user);
    }

    private boolean isDueNow(LineUser user) {
        ZoneId zone = resolveZoneOrDefault(user.getTimezone());
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalTime notifyTime = user.getNotifyTime() != null ? user.getNotifyTime() : DEFAULT_NOTIFY_TIME;

        if (now.getHour() != notifyTime.getHour() || now.getMinute() != notifyTime.getMinute()) {
            return false;
        }

        LocalDateTime scheduledAtToday = now.toLocalDate().atTime(notifyTime);

        return user.getLastNotifiedAt() == null || user.getLastNotifiedAt().isBefore(scheduledAtToday);
    }

    private ZoneId resolveZoneOrDefault(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }

        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException ex) {
            log.warn("Invalid timezone '{}' for user settings. Fallback to {}", timezone, DEFAULT_TIMEZONE);
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    private @NonNull LineUser getOrCreateUser(String userId) {
        String safeUserId = Objects.requireNonNull(requireUserId(userId));
        LineUser existingUser = lineUserRepository.findById(safeUserId).orElse(null);
        if (existingUser != null) {
            if (applyDefaultSettings(existingUser)) {
                return Objects.requireNonNull(lineUserRepository.save(existingUser));
            }
            return existingUser;
        }

        LineUser newUser = new LineUser(safeUserId);
        applyDefaultSettings(newUser);
        return Objects.requireNonNull(lineUserRepository.save(newUser));
    }

    private boolean applyDefaultSettings(LineUser user) {
        boolean changed = false;

        if (user.getNotifyEnabled() == null) {
            user.setNotifyEnabled(true);
            changed = true;
        }
        if (user.getNotifyTime() == null) {
            user.setNotifyTime(DEFAULT_NOTIFY_TIME);
            changed = true;
        }
        if (user.getTimezone() == null || user.getTimezone().isBlank()) {
            user.setTimezone(DEFAULT_TIMEZONE);
            changed = true;
        }

        return changed;
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId.trim();
    }

    private int resolveHistoryLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HISTORY_LIMIT;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        return Math.min(limit, MAX_HISTORY_LIMIT);
    }

    public record ScheduledDispatchResult(int candidates, int sent, int failed) {
    }
}
