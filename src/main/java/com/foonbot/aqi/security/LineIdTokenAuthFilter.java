package com.foonbot.aqi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Order(2)
public class LineIdTokenAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/users/me/settings",
            "/api/air-quality/history/me",
            "/api/air-quality/by-location"
    );

    private final LineIdTokenVerificationService lineIdTokenVerificationService;

    public LineIdTokenAuthFilter(LineIdTokenVerificationService lineIdTokenVerificationService) {
        this.lineIdTokenVerificationService = lineIdTokenVerificationService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PROTECTED_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!lineIdTokenVerificationService.isConfigured()) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "LINE login channel ID is not configured");
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpStatus.UNAUTHORIZED, "LINE ID token is required");
            return;
        }

        String idToken = authorization.substring("Bearer ".length()).trim();
        try {
            String userId = lineIdTokenVerificationService.verifyAndExtractUserId(idToken);
            request.setAttribute(SecurityRequestAttributes.AUTHENTICATED_LINE_USER_ID, userId);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            writeError(response, HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
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
