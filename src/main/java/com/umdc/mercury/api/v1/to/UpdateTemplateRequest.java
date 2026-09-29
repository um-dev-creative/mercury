package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request record for updating the mutable metadata of an existing {@code TemplateEntity}.
 *
 * <p>Fields are optional; only non-null values are applied. {@code applicationId} is
 * intentionally absent — the owning application is immutable once a template is created.</p>
 *
 * @param description    new description, or {@code null} to leave unchanged.
 * @param location       new metadata-only location, or {@code null} to leave unchanged. Max 500 chars.
 * @param fileFormat     new file format, or {@code null} to leave unchanged. Max 50 chars.
 * @param templateTypeId UUID of a different existing, active {@code TemplateTypeEntity}, or {@code null} to leave unchanged.
 * @param severityTypeId UUID of a different existing, active {@code SeverityTypeEntity}, or {@code null} to leave unchanged.
 */
public record UpdateTemplateRequest(
        String description,
        @Size(max = 500)
        String location,
        @Size(max = 50)
        String fileFormat,
        UUID templateTypeId,
        UUID severityTypeId
) {
}
