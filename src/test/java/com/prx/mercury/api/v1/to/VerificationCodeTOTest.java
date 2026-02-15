package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationCodeTOTest {

    @Test
    @DisplayName("Create VerificationCodeTO with valid data")
    void createVerificationCodeTOWithValidData() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        String verificationCode = "123456";
        LocalDateTime now = LocalDateTime.now();
        Boolean isVerified = false;
        Integer attempts = 0;
        Integer maxAttempts = 5;
        String createdBy = "user";
        String updatedBy = "user";
        UUID messageRecordId = UUID.randomUUID();

        VerificationCodeTO verificationCodeTO = new VerificationCodeTO(
                id, userId, applicationId, verificationCode, now, now, now.plusDays(1), null, isVerified, attempts, maxAttempts, createdBy, updatedBy, messageRecordId
        );

        assertNotNull(verificationCodeTO);
        assertEquals(id, verificationCodeTO.id());
        assertEquals(userId, verificationCodeTO.userId());
        assertEquals(applicationId, verificationCodeTO.applicationId());
        assertEquals(verificationCode, verificationCodeTO.verificationCode());
        assertEquals(now, verificationCodeTO.createdAt());
        assertEquals(now, verificationCodeTO.modifiedAt());
        assertEquals(now.plusDays(1), verificationCodeTO.expiredAt());
        assertNull(verificationCodeTO.verifiedAt());
        assertEquals(isVerified, verificationCodeTO.isVerified());
        assertEquals(attempts, verificationCodeTO.attempts());
        assertEquals(maxAttempts, verificationCodeTO.maxAttempts());
        assertEquals(createdBy, verificationCodeTO.createdBy());
        assertEquals(updatedBy, verificationCodeTO.updatedBy());
        assertEquals(messageRecordId, verificationCodeTO.messageRecordId());
    }

    @Test
    @DisplayName("Create VerificationCodeTO with null values")
    void createVerificationCodeTOWithNullValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        String verificationCode = "123456";
        LocalDateTime now = LocalDateTime.now();
        Boolean isVerified = null;
        Integer attempts = null;
        Integer maxAttempts = null;
        String createdBy = null;
        String updatedBy = null;
        UUID messageRecordId = UUID.randomUUID();

        VerificationCodeTO verificationCodeTO = new VerificationCodeTO(
                id, userId, applicationId, verificationCode, now, now, now.plusDays(1), null, isVerified, attempts, maxAttempts, createdBy, updatedBy, messageRecordId
        );

        assertNotNull(verificationCodeTO);
        assertEquals(id, verificationCodeTO.id());
        assertEquals(userId, verificationCodeTO.userId());
        assertEquals(applicationId, verificationCodeTO.applicationId());
        assertEquals(verificationCode, verificationCodeTO.verificationCode());
        assertEquals(now, verificationCodeTO.createdAt());
        assertEquals(now, verificationCodeTO.modifiedAt());
        assertEquals(now.plusDays(1), verificationCodeTO.expiredAt());
        assertNull(verificationCodeTO.verifiedAt());
        assertNull(verificationCodeTO.isVerified());
        assertNull(verificationCodeTO.attempts());
        assertNull(verificationCodeTO.maxAttempts());
        assertNull(verificationCodeTO.createdBy());
        assertNull(verificationCodeTO.updatedBy());
        assertEquals(messageRecordId, verificationCodeTO.messageRecordId());
    }

    @Test
    @DisplayName("VerificationCodeTO toString method")
    void verificationCodeTOToString() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        String verificationCode = "123456";
        LocalDateTime now = LocalDateTime.now();
        Boolean isVerified = false;
        Integer attempts = 0;
        Integer maxAttempts = 5;
        String createdBy = "user";
        String updatedBy = "user";
        UUID messageRecordId = UUID.randomUUID();

        VerificationCodeTO verificationCodeTO = new VerificationCodeTO(
                id, userId, applicationId, verificationCode, now, now, now.plusDays(1), null, isVerified, attempts, maxAttempts, createdBy, updatedBy, messageRecordId
        );

        String expectedString = "VerificationCodeTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", applicationId=" + applicationId +
                ", verificationCode='" + verificationCode + '\'' +
                ", createdAt=" + now +
                ", modifiedAt=" + now +
                ", expiredAt=" + now.plusDays(1) +
                ", verifiedAt=" + null +
                ", isVerified=" + isVerified +
                ", attempts=" + attempts +
                ", maxAttempts=" + maxAttempts +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", messageRecordId=" + messageRecordId +
                '}';

        assertEquals(expectedString, verificationCodeTO.toString());
    }
}
