package com.prx.mercury.api.v1.to;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Record representing a request for verification code operations.
 * This record holds the application ID, user ID, and the verification code.
 *
 * @param applicationId The unique identifier of the application
 * @param userId        The unique identifier of the user
 * @param code          The verification code to be confirmed
 */
public record VerificationCodeRequest(UUID applicationId, UUID userId, String code) {

    public VerificationCodeRequest {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "VerificationCodeRequest{" +
                "applicationId=" + applicationId +
                ", userId=" + userId +
                ", code='" + code + '\'' +
                '}';
    }
}
