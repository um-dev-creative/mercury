package com.prx.mercury.api.v1.to;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the type of a template along with its metadata and status.
 * This record is immutable and encapsulates information necessary to define a template type.
 *
 * Fields:
 * - id: The unique identifier for the template type.
 * - name: The name of the template type.
 * - description: A detailed description of the template type.
 * - createdAt: The timestamp indicating when the template type was created.
 * - updatedAt: The timestamp indicating when the template type was last updated.
 * - isActive: Indicates whether the template type is currently active.
 */
public record TemplateTypeTO(UUID id,
                             String name,
                             String description,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt,
                             Boolean isActive) {

    @Override
    public String toString() {
        return "TemplateTypeTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                '}';
    }
}
