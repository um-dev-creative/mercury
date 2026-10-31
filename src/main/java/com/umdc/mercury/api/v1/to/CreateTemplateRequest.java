package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request record for creating a new reusable {@code TemplateEntity}.
 *
 * @param description    human-readable description of the template. Required.
 * @param location       metadata-only pointer to where the template content lives
 *                       (upload/download of content is out of scope). Required, max 500 chars.
 * @param fileFormat     the template's file format (e.g. {@code HTML}, {@code PDF}). Required, max 50 chars.
 * @param templateTypeId UUID of an existing, active {@code TemplateTypeEntity}. Required.
 * @param applicationId  UUID of the application the template belongs to. Required, immutable once created.
 * @param severityTypeId UUID of an existing, active {@code SeverityTypeEntity}. Required.
 */
public record CreateTemplateRequest(
        @NotNull @NotBlank
        String description,
        @NotNull @NotBlank @Size(max = 500)
        String location,
        @NotNull @NotBlank @Size(max = 50)
        String fileFormat,
        @NotNull
        UUID templateTypeId,
        @NotNull
        UUID applicationId,
        @NotNull
        UUID severityTypeId
) {
}
