package com.foonbot.aqi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
@Order(1)
public class AdminSecretFilter extends OncePerRequestFilter {

    private static final Set<String> ADMIN_PATHS = Set.of(
            "/api/air-quality/notify",
            "/api/air-quality/fetch"
    );

    @Value("${app.admin.secret:}")
    private String adminSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ADMIN_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (adminSecret == null || adminSecret.isBlank()) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "Admin secret is not configured");
            return;
        }

        String providedSecret = request.getHeader("X-Admin-Secret");
        if (providedSecret == null || !matches(adminSecret.trim(), providedSecret.trim())) {
            writeError(response, HttpStatus.FORBIDDEN, "Valid admin secret is required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matches(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String message) {
        return message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
