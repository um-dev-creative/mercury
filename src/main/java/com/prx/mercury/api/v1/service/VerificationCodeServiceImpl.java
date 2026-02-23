package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.VerificationCodeRequest;
import com.prx.mercury.api.v1.to.VerificationCodeTO;
import com.prx.mercury.jpa.sql.entity.VerificationCodeEntity;
import com.prx.mercury.jpa.sql.repository.VerificationCodeRepository;
import com.prx.mercury.mapper.VerificationCodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service implementation for managing verification codes.
 * This service handles operations related to the creation and confirmation
 * of verification codes used in application authentication flows.
 */
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeServiceImpl.class);

    private final VerificationCodeRepository verificationCodeRepository;
    private final VerificationCodeMapper verificationCodeMapper;

    /**
     * Constructs a new VerificationCodeServiceImpl with required dependencies.
     *
     * @param verificationCodeRepository Repository for verification code persistence operations
     * @param verificationCodeMapper     Mapper for converting between document and transfer objects
     */
    public VerificationCodeServiceImpl(VerificationCodeRepository verificationCodeRepository, VerificationCodeMapper verificationCodeMapper) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.verificationCodeMapper = verificationCodeMapper;
    }

    /**
     * Confirms a verification code for a user and application.
     * This method validates the provided code against stored verification codes,
     * updates attempt counts, and marks the code as verified if it matches.
     *
     * @param verificationCodeRequest The request containing userId, applicationId and code to verify
     * @return ResponseEntity with ACCEPTED status if verified, NOT_ACCEPTABLE otherwise
     * @throws IllegalArgumentException if no valid verification code is found for the user and application
     */
    @Override
    public ResponseEntity<Void> confirmCode(VerificationCodeRequest verificationCodeRequest) {
        var verificationCodeEntityOptional = this.verificationCodeRepository.findByUserIdAndApplicationIdAndExpiresAtBeforeAndIsVerified(
                verificationCodeRequest.userId(),
                verificationCodeRequest.applicationId(),
                LocalDateTime.now(), false);
        if (verificationCodeEntityOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("message", "No valid verification code found for the user and application")
                    .build();
        }
        var collectionResult = verificationCodeEntityOptional.stream()
                .filter(verificationCodeEntity ->
                        !verificationCodeEntity.getIsVerified()
                                && verificationCodeEntity.getAttempts() < verificationCodeEntity.getMaxAttempts())
                .map(verificationCodeEntity -> {
                    verificationCodeEntity.setAttempts(verificationCodeEntity.getAttempts() + 1);
                    verificationCodeEntity.setModifiedAt(LocalDateTime.now());
                    verificationCodeEntity.setModifiedBy(verificationCodeRequest.userId().toString());
                    if (verificationCodeEntity.getVerificationCode().equals(verificationCodeRequest.code())) {
                        verificationCodeEntity.setIsVerified(true);
                        verificationCodeEntity.setVerifiedAt(LocalDateTime.now());
                        logger.debug("Verification code confirmed: {}", verificationCodeEntity);
                    }
                    return verificationCodeEntity;
                }).toList();

        verificationCodeRepository.saveAll(collectionResult);

        var result = collectionResult.stream().filter(VerificationCodeEntity::getIsVerified).findFirst();
        return result.isPresent() ? ResponseEntity.status(HttpStatus.ACCEPTED).build() :
                ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    /**
     * Creates a new verification code entry.
     * Maps the provided transfer object to an document, persists it to the database,
     * and returns the created code as a transfer object.
     *
     * @param verificationCodeTO The verification code transfer object to create
     * @return The created verification code as a transfer object
     */
    @Override
    public VerificationCodeTO create(VerificationCodeTO verificationCodeTO) {
        logger.debug("Creating verification code: {}", verificationCodeTO);

        return verificationCodeMapper.toVerificationCodeTO(
                verificationCodeRepository.save(verificationCodeMapper
                        .toVerificationCodeEntity(verificationCodeTO)));
    }

    /**
     * Retrieves the latest is_verified status for a user.
     * This method queries the repository for the most recent verification code
     * status for the given userId, handling not found and error cases.
     *
     * @param userId The ID of the user whose verification status is to be retrieved
     * @return ResponseEntity containing the is_verified status, NOT_FOUND if no code exists,
     *         BAD_REQUEST for invalid userId format, INTERNAL_SERVER_ERROR for other errors
     */
    @Override
    public ResponseEntity<Boolean> getLatestIsVerifiedStatus(String userId) {
        try {
            java.util.UUID uuid = java.util.UUID.fromString(userId);
            Boolean isVerified = verificationCodeRepository.findLatestIsVerifiedByUserId(uuid);
            if (isVerified == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(isVerified);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
