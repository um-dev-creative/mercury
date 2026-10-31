package com.umdc.mercury.api.v1.exception;

/// Thrown when a login client with the requested alias already exists. Mapped to `409 Conflict` by the global exception handler.
public class LoginClientAlreadyExistsException extends RuntimeException {

    /// @param message human-readable description of the clash
    public LoginClientAlreadyExistsException(String message) {
        super(message);
    }

    /// @param message human-readable description of the clash
    /// @param cause   what made the store refuse the second registration
    public LoginClientAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
