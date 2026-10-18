package com.umdc.mercury.api.v1.exception;

/** Thrown when a caller asks for the status of a mail it did not submit or that does not exist (any more). */
public class MailNotFoundException extends RuntimeException {
    public MailNotFoundException(String message) {
        super(message);
    }
}
