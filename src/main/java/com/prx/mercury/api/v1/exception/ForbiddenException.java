package com.prx.mercury.api.v1.exception;

/**
 * Thrown when the caller is not authorized to perform an action on a resource.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

