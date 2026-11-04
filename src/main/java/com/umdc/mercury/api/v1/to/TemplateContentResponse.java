package com.umdc.mercury.api.v1.to;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The content of a template and what an editor needs to work with it.
 *
 * @param templateId   Template the content belongs to.
 * @param name         File name ({@code location} of the template).
 * @param format       {@code MARKDOWN} for {@code .md} sources, {@code FREEMARKER} for the rest.
 * @param content      The template as written; empty if it has not been saved yet.
 * @param etag         Fingerprint to send back as {@code If-Match} when saving; empty if there is no file yet.
 * @param exists       Whether the file exists in the storage.
 * @param lastModified Last change; {@code null} if there is no file yet.
 * @param system       {@code true} for system templates, which other services depend on: saving one needs an explicit confirmation.
 * @param parameters   Parameters the template reads, from a reading of its text.
 */
public record TemplateContentResponse(UUID templateId, String name, String format, String content, String etag,
                                      boolean exists, Instant lastModified, boolean system, List<String> parameters) {
}
