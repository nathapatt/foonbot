package com.foonbot.aqi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class LineWebhookSignatureService {

    @Value("${line.messaging.channel-secret:}")
    private String channelSecret;

    public boolean isConfigured() {
        return channelSecret != null && !channelSecret.isBlank();
    }

    public boolean isValidSignature(String payload, String providedSignature) {
        if (!isConfigured() || providedSignature == null || providedSignature.isBlank() || payload == null) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(channelSecret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expectedSignature = Base64.getEncoder().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to verify LINE webhook signature", ex);
        }
    }
}
