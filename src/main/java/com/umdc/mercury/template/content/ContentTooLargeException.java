package com.umdc.mercury.template.content;

/**
 * The content of a template is bigger than Mercury accepts.
 */
public class ContentTooLargeException extends RuntimeException {

    public ContentTooLargeException(int maxBytes) {
        super("The template content is larger than " + maxBytes + " bytes");
    }
}
