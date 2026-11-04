package com.umdc.mercury.template.storage;

/**
 * The template changed since the caller read it: its etag no longer matches the one expected.
 */
public class TemplateConflictException extends RuntimeException {

    public TemplateConflictException(String name) {
        super("The template changed since it was read: " + name);
    }
}
