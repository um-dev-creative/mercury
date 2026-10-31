package com.umdc.mercury.api.v1.exception;

import java.util.UUID;

/**
 * Thrown when a template with the requested {@link UUID} does not exist, or exists
 * but is no longer active.
 *
 * <p>Mapped to HTTP {@code 404 Not Found} by the global exception handler.</p>
 */
public class TemplateNotFoundException extends RuntimeException {

    /**
     * Constructs a {@code TemplateNotFoundException} with a detail message.
     *
     * @param message human-readable description of the missing resource.
     */
    public TemplateNotFoundException(String message) {
        super(message);
    }
}
