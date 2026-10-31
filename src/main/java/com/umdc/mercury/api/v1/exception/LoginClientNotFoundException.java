package com.umdc.mercury.api.v1.exception;

/// Thrown when the login client with the requested id does not exist. Mapped to `404 Not Found` by the global exception handler.
public class LoginClientNotFoundException extends RuntimeException {

    /// @param message human-readable description of the missing login client
    public LoginClientNotFoundException(String message) {
        super(message);
    }
}
