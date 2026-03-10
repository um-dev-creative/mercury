package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a transfer object for verification codes, encapsulating the details
 * necessary for managing and tracking verification codes within a system.
 * This record is immutable and provides a clear structure for verification code-related data.
 *
 * Fields:
 * - id: The unique identifier of the verification code.
 * - userId: The unique identifier of the user associated with the verification code.
 * - applicationId: The unique identifier of the application associated with the verification process.
 * - verificationCode: The verification code issued to the user for validation.
 * - createdAt: The timestamp indicating when the verification code was created.
 * - modifiedAt: The last timestamp when the verification code entry was updated.
 * - expiredAt: The expiration timestamp for the verification code.
 * - verifiedAt: The timestamp indicating when the verification code was successfully verified.
 * - isVerified: A boolean indicating whether the verification code has been successfully verified.
 * - attempts: The current number of verification attempts made by the user.
 * - maxAttempts: The maximum number of permitted attempts for the verification process.
 * - createdBy: The identifier of the entity or user who created the verification code.
 * - updatedBy: The identifier of the entity or user who last updated the verification code entry.
 * - messageRecordId: The unique identifier of the message record associated with the verification process,
 *   typically used for tracking purposes.
 *
 * Behavior:
 * - The record provides a structured way to handle the lifecycle of a verification code, including
 *   creation, validation, expiration, and tracking of attempts.
 * - Timestamps for creation, modifications, expiration, and verification are formatted as per the
 *   specified date-time pattern.
 */
public record VerificationCodeTO(
        UUID id,
        UUID userId,
        UUID applicationId,
        String verificationCode,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime modifiedAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime expiredAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime verifiedAt,
        Boolean isVerified,
        Integer attempts,
        Integer maxAttempts,
        String createdBy,
        String updatedBy,
        UUID messageRecordId
) {

        @Override
        public String toString() {
                return "VerificationCodeTO{" +
                        "id=" + id +
                        ", userId=" + userId +
                        ", applicationId=" + applicationId +
                        ", verificationCode='" + verificationCode + '\'' +
                        ", createdAt=" + createdAt +
                        ", modifiedAt=" + modifiedAt +
                        ", expiredAt=" + expiredAt +
                        ", verifiedAt=" + verifiedAt +
                        ", isVerified=" + isVerified +
                        ", attempts=" + attempts +
                        ", maxAttempts=" + maxAttempts +
                        ", createdBy='" + createdBy + '\'' +
                        ", updatedBy='" + updatedBy + '\'' +
                        ", messageRecordId=" + messageRecordId +
                        '}';
        }
}
