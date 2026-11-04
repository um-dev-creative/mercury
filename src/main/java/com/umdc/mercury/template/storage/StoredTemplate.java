package com.umdc.mercury.template.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A template with its content.
 *
 * @param metadata Name, etag and last change.
 * @param content  The bytes, UTF-8 text.
 */
public record StoredTemplate(TemplateMetadata metadata, byte[] content) {

    @Override
    public boolean equals(Object other) {
        return other instanceof StoredTemplate that && metadata.equals(that.metadata) && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return 31 * metadata.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "StoredTemplate[" + metadata + ", " + content.length + " bytes]";
    }

    /** @return the content as text. */
    public String text() {
        return new String(content, StandardCharsets.UTF_8);
    }
}
