package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a message status type with its associated metadata.
 *
 * This record defines the structure and attributes of a message status type,
 * encapsulating properties such as its unique identifier, descriptive details,
 * creation and update timestamps, and its active state.
 *
 * Fields:
 * - id: The unique identifier for the message status type.
 * - name: The name of the message status type.
 * - description: A detailed description of the message status type.
 * - createdAt: The timestamp when the message status type was created. This field is formatted
 *              according to a specific pattern defined by {@code DateUtil.PATTERN_DATE_TIME}.
 * - updatedAt: The timestamp indicating the last update made to the message status type.
 * - active: A flag indicating whether the message status type is active or not.
 *
 * Immutability:
 * - This record is immutable and designed to provide thread-safe and encapsulated data storage.
 */
public record MessageStatusTypeTO(
        UUID id,
        String name,
        String description,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean active
) {

        @Override
        public String toString() {
                return "MessageStatusTypeTO{" +
                        "id=" + id +
                        ", name='" + name + '\'' +
                        ", description='" + description + '\'' +
                        ", createdAt=" + createdAt +
                        ", updatedAt=" + updatedAt +
                        ", active=" + active +
                        '}';
        }
}
