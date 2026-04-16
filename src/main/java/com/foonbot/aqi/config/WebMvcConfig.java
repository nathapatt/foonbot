package com.foonbot.aqi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Redirect /liff/aqi → /liff/aqi/ so Spring Boot serves index.html correctly.
     * Also register /liff/aqi/ to forward to the static index.html.
     */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addRedirectViewController("/liff/aqi", "/liff/aqi/");
    }
}
