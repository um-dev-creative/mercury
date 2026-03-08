package com.prx.mercury.api.v1.to;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a template object with various metadata and configuration details.
 * This record is immutable and encapsulates the attributes required to define and manage a template.
 *
 * Fields:
 * - id: The unique identifier for the template.
 * - description: A detailed description of the template.
 * - location: The location or path where the template is stored or hosted.
 * - fileFormat: The file format of the template (e.g., "PDF", "HTML").
 * - templateType: The type of template, represented by a {@code TemplateTypeTO} object.
 * - application: The unique identifier of the application associated with the template.
 * - createdAt: The timestamp indicating when the template was created.
 * - updatedAt: The timestamp indicating when the template was last updated.
 * - isActive: Indicates whether the template is currently active.
 */
public record TemplateTO(UUID id,
                         String description,
                         String location,
                         String fileFormat,
                         TemplateTypeTO templateType,
                         UUID application,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt,
                         Boolean isActive) {

    @Override
    public String toString() {
        return "TemplateTO{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", fileFormat='" + fileFormat + '\'' +
                ", templateType=" + templateType +
                ", application=" + application +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                '}';
    }
}
