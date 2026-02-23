package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.VerificationCodeRequest;
import com.prx.mercury.api.v1.to.VerificationCodeTO;
import com.prx.mercury.jpa.sql.entity.ApplicationEntity;
import com.prx.mercury.jpa.sql.entity.VerificationCodeEntity;
import com.prx.mercury.jpa.sql.repository.VerificationCodeRepository;
import com.prx.mercury.mapper.VerificationCodeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VerificationCodeServiceImplTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private VerificationCodeMapper verificationCodeMapper;

    @InjectMocks
    private VerificationCodeServiceImpl verificationCodeServiceImpl;

    public VerificationCodeServiceImplTest() {
        MockitoAnnotations.openMocks(this);
    }

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
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
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
        VerificationCodeEntity savedEntity = new VerificationCodeEntity();

        VerificationCodeTO verificationCodeTO = new VerificationCodeTO(
                id, userId, applicationId, verificationCode, now, now, now.plusDays(1), null, isVerified, attempts, maxAttempts, createdBy, updatedBy, messageRecordId
        );

        when(verificationCodeMapper.toVerificationCodeEntity(any(VerificationCodeTO.class))).thenReturn(verificationCodeEntity);
        when(verificationCodeRepository.save(any(VerificationCodeEntity.class))).thenReturn(savedEntity);
        when(verificationCodeMapper.toVerificationCodeTO(any(VerificationCodeEntity.class))).thenReturn(verificationCodeTO);

        VerificationCodeTO result = verificationCodeServiceImpl.create(verificationCodeTO);

        assertNotNull(result);
        assertEquals(verificationCodeTO, result);
        verify(verificationCodeMapper).toVerificationCodeEntity(verificationCodeTO);
        verify(verificationCodeRepository).save(verificationCodeEntity);
        verify(verificationCodeMapper).toVerificationCodeTO(savedEntity);
    }

    @Test
    @DisplayName("getLatestIsVerifiedStatus returns true when record found and isVerified is true")
    void getLatestIsVerifiedStatus_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        when(verificationCodeRepository.findLatestIsVerifiedByUserId(userId)).thenReturn(true);
        var response = verificationCodeServiceImpl.getLatestIsVerifiedStatus(userId.toString());
        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody());
    }

    @Test
    @DisplayName("getLatestIsVerifiedStatus returns false when record found and isVerified is false")
    void getLatestIsVerifiedStatus_ReturnsFalse() {
        UUID userId = UUID.randomUUID();
        when(verificationCodeRepository.findLatestIsVerifiedByUserId(userId)).thenReturn(false);
        var response = verificationCodeServiceImpl.getLatestIsVerifiedStatus(userId.toString());
        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.FALSE, response.getBody());
    }

    @Test
    @DisplayName("getLatestIsVerifiedStatus returns 404 when no record found")
    void getLatestIsVerifiedStatus_NotFound() {
        UUID userId = UUID.randomUUID();
        when(verificationCodeRepository.findLatestIsVerifiedByUserId(userId)).thenReturn(null);
        var response = verificationCodeServiceImpl.getLatestIsVerifiedStatus(userId.toString());
        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("getLatestIsVerifiedStatus returns 400 for invalid userId format")
    void getLatestIsVerifiedStatus_InvalidUserId() {
        var response = verificationCodeServiceImpl.getLatestIsVerifiedStatus("not-a-uuid");
        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("getLatestIsVerifiedStatus returns 500 on repository exception")
    void getLatestIsVerifiedStatus_RepositoryException() {
        UUID userId = UUID.randomUUID();
        when(verificationCodeRepository.findLatestIsVerifiedByUserId(userId)).thenThrow(new RuntimeException("DB error"));
        var response = verificationCodeServiceImpl.getLatestIsVerifiedStatus(userId.toString());
        assertEquals(500, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("confirmCode returns NOT_FOUND when repository returns empty list")
    void confirmCode_NotFound() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        when(verificationCodeRepository.findByUserIdAndApplicationIdAndExpiresAtBeforeAndIsVerified(eq(userId), eq(applicationId), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of());

        var request = new VerificationCodeRequest(applicationId, userId, "0000");

        ResponseEntity<Void> response = verificationCodeServiceImpl.confirmCode(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No valid verification code found for the user and application", response.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("confirmCode returns NOT_ACCEPTABLE when no eligible codes (attempts >= max)")
    void confirmCode_NoEligibleCodes() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        VerificationCodeEntity entity = new VerificationCodeEntity();
        entity.setUserId(userId);
        entity.setApplication(new ApplicationEntity());
        entity.setAttempts(3);
        entity.setMaxAttempts(3);
        entity.setIsVerified(false);
        entity.setVerificationCode("1111");

        when(verificationCodeRepository.findByUserIdAndApplicationIdAndExpiresAtBeforeAndIsVerified(eq(userId), eq(applicationId), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of(entity));

        var request = new VerificationCodeRequest(applicationId, userId, "1111");

        ArgumentCaptor<List<VerificationCodeEntity>> captor = ArgumentCaptor.forClass(List.class);
        ResponseEntity<Void> response = verificationCodeServiceImpl.confirmCode(request);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        verify(verificationCodeRepository).saveAll(captor.capture());
        List<VerificationCodeEntity> saved = captor.getValue();
        assertNotNull(saved);
        assertTrue(saved.isEmpty());
    }

    @Test
    @DisplayName("confirmCode returns ACCEPTED and marks code as verified when code matches")
    void confirmCode_CodeMatches() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        VerificationCodeEntity entity = new VerificationCodeEntity();
        entity.setUserId(userId);
        entity.setApplication(new ApplicationEntity());
        entity.setAttempts(0);
        entity.setMaxAttempts(3);
        entity.setIsVerified(false);
        entity.setVerificationCode("2222");

        when(verificationCodeRepository.findByUserIdAndApplicationIdAndExpiresAtBeforeAndIsVerified(eq(userId), eq(applicationId), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of(entity));

        var request = new VerificationCodeRequest(applicationId, userId, "2222");

        ArgumentCaptor<List<VerificationCodeEntity>> captor = ArgumentCaptor.forClass(List.class);
        ResponseEntity<Void> response = verificationCodeServiceImpl.confirmCode(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(verificationCodeRepository).saveAll(captor.capture());
        List<VerificationCodeEntity> saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(1, saved.size());
        VerificationCodeEntity savedEntity = saved.get(0);
        assertTrue(savedEntity.getIsVerified());
        assertEquals(1, savedEntity.getAttempts());
        assertNotNull(savedEntity.getModifiedAt());
        assertEquals(userId.toString(), savedEntity.getModifiedBy());
    }

    @Test
    @DisplayName("confirmCode returns NOT_ACCEPTABLE when code does not match")
    void confirmCode_CodeDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        VerificationCodeEntity entity = new VerificationCodeEntity();
        entity.setUserId(userId);
        entity.setApplication(new ApplicationEntity());
        entity.setAttempts(0);
        entity.setMaxAttempts(3);
        entity.setIsVerified(false);
        entity.setVerificationCode("3333");

        when(verificationCodeRepository.findByUserIdAndApplicationIdAndExpiresAtBeforeAndIsVerified(eq(userId), eq(applicationId), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of(entity));

        var request = new VerificationCodeRequest(applicationId, userId, "9999");

        ArgumentCaptor<List<VerificationCodeEntity>> captor = ArgumentCaptor.forClass(List.class);
        ResponseEntity<Void> response = verificationCodeServiceImpl.confirmCode(request);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        verify(verificationCodeRepository).saveAll(captor.capture());
        List<VerificationCodeEntity> saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(1, saved.size());
        VerificationCodeEntity savedEntity = saved.get(0);
        assertFalse(savedEntity.getIsVerified());
        assertEquals(1, savedEntity.getAttempts());
    }

    @Test
    @DisplayName("confirmCode accepts when one of multiple returned entities is eligible and matches")
    void confirmCode_MultipleEntitiesOneMatches() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        VerificationCodeEntity ineligible = new VerificationCodeEntity();
        ineligible.setUserId(userId);
        ineligible.setApplication(new ApplicationEntity());
        ineligible.setAttempts(3);
        ineligible.setMaxAttempts(3);
        ineligible.setIsVerified(false);
        ineligible.setVerificationCode("4444");

        VerificationCodeEntity eligible = new VerificationCodeEntity();
        eligible.setUserId(userId);
        eligible.setApplication(new ApplicationEntity());
        eligible.setAttempts(0);
        eligible.setMaxAttempts(3);
        eligible.setIsVerified(false);
        eligible.setVerificationCode("5555");

        when(verificationCodeRepository.findByUserIdAndApplicationIdAndExpiresAtBeforeAndIsVerified(eq(userId), eq(applicationId), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of(ineligible, eligible));

        var request = new VerificationCodeRequest(applicationId, userId, "5555");

        ArgumentCaptor<List<VerificationCodeEntity>> captor = ArgumentCaptor.forClass(List.class);
        ResponseEntity<Void> response = verificationCodeServiceImpl.confirmCode(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(verificationCodeRepository).saveAll(captor.capture());
        List<VerificationCodeEntity> saved = captor.getValue();
        // only eligible document should be in saved list
        assertNotNull(saved);
        assertEquals(1, saved.size());
        VerificationCodeEntity savedEntity = saved.get(0);
        assertTrue(savedEntity.getIsVerified());
        assertEquals(1, savedEntity.getAttempts());
    }

    @Test
    @DisplayName("confirmCode increments attempts for multiple eligible entities when none match")
    void confirmCode_MultipleEligibleNoneMatch() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        VerificationCodeEntity e1 = new VerificationCodeEntity();
        e1.setUserId(userId);
        e1.setApplication(new ApplicationEntity());
        e1.setAttempts(0);
        e1.setMaxAttempts(3);
        e1.setIsVerified(false);
        e1.setVerificationCode("aaa1");

        VerificationCodeEntity e2 = new VerificationCodeEntity();
        e2.setUserId(userId);
        e2.setApplication(new ApplicationEntity());
        e2.setAttempts(1);
        e2.setMaxAttempts(3);
        e2.setIsVerified(false);
        e2.setVerificationCode("aaa2");

        when(verificationCodeRepository.findByUserIdAndApplicationIdAndExpiresAtBeforeAndIsVerified(eq(userId), eq(applicationId), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of(e1, e2));

        var request = new VerificationCodeRequest(applicationId, userId, "nomatch");

        ArgumentCaptor<List<VerificationCodeEntity>> captor = ArgumentCaptor.forClass(List.class);
        ResponseEntity<Void> response = verificationCodeServiceImpl.confirmCode(request);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        verify(verificationCodeRepository).saveAll(captor.capture());
        List<VerificationCodeEntity> saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(2, saved.size());
        // attempts incremented
        assertEquals(1, saved.get(0).getAttempts());
        assertEquals(2, saved.get(1).getAttempts());
        // none verified
        assertFalse(saved.get(0).getIsVerified());
        assertFalse(saved.get(1).getIsVerified());
    }

}
