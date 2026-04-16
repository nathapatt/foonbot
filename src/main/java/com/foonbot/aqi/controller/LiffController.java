package com.foonbot.aqi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RestController
public class LiffController {

    @Value("${liff.aqi-id:${liff.id:}}")
    private String aqiLiffId;

    @Value("${liff.settings-id:}")
    private String settingsLiffId;

    @Value("${liff.history-id:}")
    private String historyLiffId;

    /**
     * Serves the LIFF AQI HTML page at /liff/aqi
     * This takes precedence over Spring Boot static resource handling.
     */
    @GetMapping(value = "/liff/aqi", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> liffAqi() throws IOException {
        String html = loadHtml("static/liff/aqi/index.html")
                .replace("__LIFF_AQI_ID__", aqiLiffId == null ? "" : aqiLiffId);
        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(MediaType.TEXT_HTML))
                .body(html);
    }

    @GetMapping(value = "/liff/settings", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> liffSettings() throws IOException {
        String html = loadHtml("static/liff/settings/index.html")
                .replace("__LIFF_SETTINGS_ID__", settingsLiffId == null ? "" : settingsLiffId);
        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(MediaType.TEXT_HTML))
                .body(html);
    }

    @GetMapping(value = "/liff/history", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> liffHistory() throws IOException {
        String html = loadHtml("static/liff/history/index.html")
                .replace("__LIFF_HISTORY_ID__", historyLiffId == null ? "" : historyLiffId);
        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(MediaType.TEXT_HTML))
                .body(html);
    }

    private @NonNull String loadHtml(@NonNull String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(Objects.requireNonNull(path));
        return Objects.requireNonNull(resource.getContentAsString(Objects.requireNonNull(StandardCharsets.UTF_8)));
    }
}
