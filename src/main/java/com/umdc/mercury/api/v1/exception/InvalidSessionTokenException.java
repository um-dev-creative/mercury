package com.umdc.mercury.api.v1.exception;

/**
 * Thrown when a caller-supplied {@code session-token} cannot be cryptographically verified —
 * missing, expired, tampered, or otherwise failing signature validation.
 */
public class InvalidSessionTokenException extends RuntimeException {
    public InvalidSessionTokenException(String message) {
        super(message);
    }
}
