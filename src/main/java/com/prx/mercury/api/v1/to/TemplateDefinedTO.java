package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TemplateDefinedTO is a record representing the association and metadata definitions
 * of a template within an application context. It encapsulates necessary information about
 * the template, user, and application it is tied to, along with timestamps and state details.
 *
 * Fields:
 * - id: A unique identifier for this TemplateDefinedTO.
 * - template: The associated template object of type TemplateTO.
 * - userId: The unique identifier of the user linked to this template definition.
 * - applicationId: The unique identifier of the application where this template is used.
 * - createdAt: The timestamp indicating when this record was created.
 * - updatedAt: The timestamp indicating the last update made to this record.
 * - expiredAt: The timestamp indicating when this record is marked as expired.
 * - active: A flag indicating whether this record is active.
 * - frequencyTypeId: The unique identifier of the frequency type associated with this template definition.
 *
 * Behavior:
 * - Provides a comprehensive toString implementation for easy logging or debugging.
 */
public record TemplateDefinedTO(
        UUID id,
        TemplateTO template,
        UUID userId,
        UUID applicationId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime expiredAt,
        Boolean active,
        UUID frequencyTypeId
) {

        @Override
        public String toString() {
                return "TemplateDefinedTO{" +
                        "id=" + id +
                        ", template=" + template +
                        ", userId=" + userId +
                        ", applicationId=" + applicationId +
                        ", createdAt=" + createdAt +
                        ", updatedAt=" + updatedAt +
                        ", expiredAt=" + expiredAt +
                        ", active=" + active +
                        ", frequencyTypeId=" + frequencyTypeId +
                        '}';
        }
}
