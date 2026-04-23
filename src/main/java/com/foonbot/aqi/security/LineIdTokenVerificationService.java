package com.foonbot.aqi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class LineIdTokenVerificationService {

    private final RestTemplate restTemplate;

    @Value("${line.login.channel-id:}")
    private String channelId;

    @Value("${line.login.verify-url:https://api.line.me/oauth2/v2.1/verify}")
    private String verifyUrl;

    public LineIdTokenVerificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return channelId != null && !channelId.isBlank();
    }

    public String verifyAndExtractUserId(String idToken) {
        String safeToken = requireNonBlank(idToken, "LINE ID token is required");
        String safeChannelId = requireNonBlank(channelId, "LINE login channel ID is not configured");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id_token", safeToken);
        form.add("client_id", safeChannelId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    verifyUrl,
                    new HttpEntity<>(form, headers),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalArgumentException("LINE identity verification failed");
            }

            Object audience = response.getBody().get("aud");
            if (!(audience instanceof String aud) || !safeChannelId.equals(aud)) {
                throw new IllegalArgumentException("LINE identity audience mismatch");
            }

            Object subject = response.getBody().get("sub");
            if (!(subject instanceof String userId) || userId.isBlank()) {
                throw new IllegalArgumentException("LINE identity did not include a user ID");
            }

            return userId.trim();
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Unable to verify LINE identity", ex);
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
