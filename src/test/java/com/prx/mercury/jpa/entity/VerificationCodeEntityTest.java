package com.prx.mercury.jpa.entity;

import com.prx.mercury.jpa.sql.entity.ApplicationEntity;
import com.prx.mercury.jpa.sql.entity.VerificationCodeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationCodeEntityTest {

    @Test
    @DisplayName("Create VerificationCodeEntity with valid data")
    void createVerificationCodeEntityWithValidData() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        UUID id = UUID.randomUUID();
        verificationCodeEntity.setId(id);
        verificationCodeEntity.setUserId(UUID.randomUUID());
        verificationCodeEntity.setApplication(new ApplicationEntity());
        verificationCodeEntity.setVerificationCode("123456");
        verificationCodeEntity.setCreatedAt(LocalDateTime.now());
        verificationCodeEntity.setExpiresAt(LocalDateTime.now().plusDays(1));
        verificationCodeEntity.setVerifiedAt(LocalDateTime.now());
        verificationCodeEntity.setIsVerified(true);
        verificationCodeEntity.setAttempts(1);
        verificationCodeEntity.setMaxAttempts(3);
        verificationCodeEntity.setCreatedBy("creator");
        verificationCodeEntity.setModifiedBy("modifier");
        verificationCodeEntity.setModifiedAt(LocalDateTime.now());

        assertEquals(id, verificationCodeEntity.getId());
        assertNotNull(verificationCodeEntity.getUserId());
        assertNotNull(verificationCodeEntity.getApplication());
        assertEquals("123456", verificationCodeEntity.getVerificationCode());
        assertNotNull(verificationCodeEntity.getCreatedAt());
        assertNotNull(verificationCodeEntity.getExpiresAt());
        assertNotNull(verificationCodeEntity.getVerifiedAt());
        assertTrue(verificationCodeEntity.getIsVerified());
        assertEquals(1, verificationCodeEntity.getAttempts());
        assertEquals(3, verificationCodeEntity.getMaxAttempts());
        assertEquals("creator", verificationCodeEntity.getCreatedBy());
        assertEquals("modifier", verificationCodeEntity.getModifiedBy());
        assertNotNull(verificationCodeEntity.getModifiedAt());
    }

    @Test
    @DisplayName("Set and get verification code")
    void setAndGetVerificationCode() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        verificationCodeEntity.setVerificationCode("654321");

        assertEquals("654321", verificationCodeEntity.getVerificationCode());
    }

    @Test
    @DisplayName("Set and get created at")
    void setAndGetCreatedAt() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        LocalDateTime now = LocalDateTime.now();
        verificationCodeEntity.setCreatedAt(now);

        assertEquals(now, verificationCodeEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Set and get expires at")
    void setAndGetExpiresAt() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        verificationCodeEntity.setExpiresAt(now);

        assertEquals(now, verificationCodeEntity.getExpiresAt());
    }

    @Test
    @DisplayName("Set and get verified at")
    void setAndGetVerifiedAt() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        LocalDateTime now = LocalDateTime.now();
        verificationCodeEntity.setVerifiedAt(now);

        assertEquals(now, verificationCodeEntity.getVerifiedAt());
    }

    @Test
    @DisplayName("Set and get is verified")
    void setAndGetIsVerified() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        verificationCodeEntity.setIsVerified(false);

        assertFalse(verificationCodeEntity.getIsVerified());
    }

    @Test
    @DisplayName("Set and get attempts")
    void setAndGetAttempts() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        verificationCodeEntity.setAttempts(2);

        assertEquals(2, verificationCodeEntity.getAttempts());
    }

    @Test
    @DisplayName("Set and get max attempts")
    void setAndGetMaxAttempts() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        verificationCodeEntity.setMaxAttempts(5);

        assertEquals(5, verificationCodeEntity.getMaxAttempts());
    }

    @Test
    @DisplayName("Set and get created by")
    void setAndGetCreatedBy() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        verificationCodeEntity.setCreatedBy("newCreator");

        assertEquals("newCreator", verificationCodeEntity.getCreatedBy());
    }

    @Test
    @DisplayName("Set and get modified by")
    void setAndGetModifiedBy() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        verificationCodeEntity.setModifiedBy("newModifier");

        assertEquals("newModifier", verificationCodeEntity.getModifiedBy());
    }

    @Test
    @DisplayName("Set and get modified at")
    void setAndGetModifiedAt() {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
        LocalDateTime now = LocalDateTime.now();
        verificationCodeEntity.setModifiedAt(now);

        assertEquals(now, verificationCodeEntity.getModifiedAt());
    }
}
