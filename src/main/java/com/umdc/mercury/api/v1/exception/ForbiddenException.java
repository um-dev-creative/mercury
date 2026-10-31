package com.umdc.mercury.api.v1.exception;

/**
 * Thrown when the caller is not authorized to perform an action on a resource.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ForbiddenException} that keeps the failure that caused the refusal.
     *
     * @param message human-readable reason
     * @param cause   the underlying failure
     */
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}

