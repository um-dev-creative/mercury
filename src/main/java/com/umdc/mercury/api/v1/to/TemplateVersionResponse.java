package com.umdc.mercury.api.v1.to;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One saved content of a template.
 *
 * @param version   Counts from 1 per template.
 * @param etag      Fingerprint of that content.
 * @param sizeBytes Size of the content in bytes.
 * @param reason    {@code SAVE}, {@code RESTORE} or {@code BASELINE} (the content before the first edit from the API).
 * @param createdBy Verified id of the person who saved it; {@code null} for a baseline.
 * @param createdAt When it was recorded (UTC).
 * @param content   The content; {@code null} in the list, present when one version is read.
 */
public record TemplateVersionResponse(int version, String etag, int sizeBytes, String reason, UUID createdBy,
                                      LocalDateTime createdAt, String content) {
}
