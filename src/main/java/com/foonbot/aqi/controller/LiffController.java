package com.foonbot.aqi.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class LiffController {

    /**
     * Serves the LIFF AQI HTML page at /liff/aqi
     * This takes precedence over Spring Boot static resource handling.
     */
    @GetMapping(value = "/liff/aqi", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> liffAqi() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/liff/aqi/index.html");
        String html = resource.getContentAsString(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
