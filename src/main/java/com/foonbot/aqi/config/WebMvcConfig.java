package com.foonbot.aqi.config;

import com.foonbot.aqi.security.AuthenticatedLineUserIdArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticatedLineUserIdArgumentResolver authenticatedLineUserIdArgumentResolver;

    public WebMvcConfig(AuthenticatedLineUserIdArgumentResolver authenticatedLineUserIdArgumentResolver) {
        this.authenticatedLineUserIdArgumentResolver = authenticatedLineUserIdArgumentResolver;
    }

    /**
     * Redirect /liff/aqi → /liff/aqi/ so Spring Boot serves index.html correctly.
     * Also register /liff/aqi/ to forward to the static index.html.
     */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addRedirectViewController("/liff/aqi", "/liff/aqi/");
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedLineUserIdArgumentResolver);
    }
}
