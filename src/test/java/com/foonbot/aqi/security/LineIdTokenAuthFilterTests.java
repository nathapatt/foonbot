package com.foonbot.aqi.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LineIdTokenAuthFilterTests {

    @Test
    void rejectsProtectedRequestWithoutBearerToken() throws ServletException, IOException {
        StubLineIdTokenVerificationService verificationService = new StubLineIdTokenVerificationService();
        verificationService.configured = true;

        LineIdTokenAuthFilter filter = new LineIdTokenAuthFilter(verificationService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/settings");
        request.setServletPath("/api/users/me/settings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void storesAuthenticatedUserIdOnSuccessfulVerification() throws ServletException, IOException {
        StubLineIdTokenVerificationService verificationService = new StubLineIdTokenVerificationService();
        verificationService.configured = true;
        verificationService.userId = "U123456";

        LineIdTokenAuthFilter filter = new LineIdTokenAuthFilter(verificationService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/settings");
        request.setServletPath("/api/users/me/settings");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getErrorMessage());
        assertEquals("U123456", request.getAttribute(SecurityRequestAttributes.AUTHENTICATED_LINE_USER_ID));
    }

    private static class StubLineIdTokenVerificationService extends LineIdTokenVerificationService {
        private boolean configured;
        private String userId;

        StubLineIdTokenVerificationService() {
            super(new RestTemplate());
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String verifyAndExtractUserId(String idToken) {
            if (userId == null) {
                throw new IllegalArgumentException("Unable to verify LINE identity");
            }
            return userId;
        }
    }
}
