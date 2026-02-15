package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationCodeRequestTest {

    @Test
    @DisplayName("Create VerificationCodeRequest with valid data")
    void createWithValidData() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String code = "ABC123";

        VerificationCodeRequest req = new VerificationCodeRequest(appId, userId, code);

        assertNotNull(req);
        assertEquals(appId, req.applicationId());
        assertEquals(userId, req.userId());
        assertEquals(code, req.code());
        assertTrue(req.toString().contains(code));
        assertTrue(req.toString().contains(userId.toString()));
    }

    @Test
    @DisplayName("Creating VerificationCodeRequest with null | empty | blank code throws IllegalArgumentException")
    void createWithNullCodeThrows() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () -> new VerificationCodeRequest(appId, userId, null));
        assertEquals("Code cannot be null or empty", exNull.getMessage());
        IllegalArgumentException exEmpty = assertThrows(IllegalArgumentException.class, () -> new VerificationCodeRequest(appId, userId, ""));
        assertEquals("Code cannot be null or empty", exEmpty.getMessage());
        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () -> new VerificationCodeRequest(appId, userId, "   "));
        assertEquals("Code cannot be null or empty", exBlank.getMessage());
    }

    @Test
    @DisplayName("Creating VerificationCodeRequest with null userId throws IllegalArgumentException")
    void createWithNullUserIdThrows() {
        UUID appId = UUID.randomUUID();
        String code = "XYZ";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new VerificationCodeRequest(appId, null, code));
        assertEquals("User ID cannot be null", ex.getMessage());
    }
}

