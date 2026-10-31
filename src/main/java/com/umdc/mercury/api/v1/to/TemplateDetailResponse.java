package com.umdc.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umdc.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response record describing a single reusable template (the base {@code TemplateEntity}),
 * as opposed to an application/user-specific {@code TemplateDefinedEntity} usage.
 *
 * @param id               unique identifier of the template.
 * @param description      human-readable description.
 * @param location         metadata-only pointer to where the template content lives.
 * @param fileFormat       the template's file format (e.g. {@code HTML}, {@code PDF}).
 * @param templateTypeId   UUID of the associated template type.
 * @param templateTypeName name of the associated template type.
 * @param applicationId    UUID of the owning application.
 * @param severityTypeId   UUID of the associated severity type.
 * @param severityTypeName name of the associated severity type.
 * @param active           whether the template is currently active (soft-delete flag).
 * @param createdAt        timestamp when the template was created.
 * @param updatedAt        timestamp of the last update to the template.
 */
public record TemplateDetailResponse(
        UUID id,
        String description,
        String location,
        String fileFormat,
        UUID templateTypeId,
        String templateTypeName,
        UUID applicationId,
        UUID severityTypeId,
        String severityTypeName,
        Boolean active,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime updatedAt
) {
}
