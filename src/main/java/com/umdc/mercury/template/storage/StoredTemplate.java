package com.umdc.mercury.template.storage;

import java.nio.charset.StandardCharsets;

/**
 * A template with its content.
 *
 * @param metadata Name, etag and last change.
 * @param content  The bytes, UTF-8 text.
 */
public record StoredTemplate(TemplateMetadata metadata, byte[] content) {

    /** @return the content as text. */
    public String text() {
        return new String(content, StandardCharsets.UTF_8);
    }
}
