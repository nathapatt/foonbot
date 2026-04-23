package com.foonbot.aqi.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineWebhookSignatureServiceTests {

    @Test
    void acceptsValidSignature() throws Exception {
        LineWebhookSignatureService service = new LineWebhookSignatureService();
        ReflectionTestUtils.setField(service, "channelSecret", "test-secret");

        String payload = "{\"events\":[]}";
        String signature = sign("test-secret", payload);

        assertTrue(service.isValidSignature(payload, signature));
    }

    @Test
    void rejectsInvalidSignature() {
        LineWebhookSignatureService service = new LineWebhookSignatureService();
        ReflectionTestUtils.setField(service, "channelSecret", "test-secret");

        assertFalse(service.isValidSignature("{\"events\":[]}", "invalid-signature"));
    }

    private String sign(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
