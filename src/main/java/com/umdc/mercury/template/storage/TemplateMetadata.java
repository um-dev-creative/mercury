package com.umdc.mercury.template.storage;

import java.time.Instant;

/**
 * What is known about a stored template without reading it.
 *
 * @param name         File name.
 * @param etag         Fingerprint of the content: equal content, equal etag.
 * @param lastModified Last change.
 */
public record TemplateMetadata(String name, String etag, Instant lastModified) {
}
