package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a message record with details about its content, origin, recipients, and metadata.
 * This record is immutable and encapsulates information about a message sent within the system.
 *
 * Fields:
 * - id: The unique identifier of the message.
 * - templateDefinedId: The unique identifier of the template used to define the message content.
 * - from: The sender's contact information, typically an email address.
 * - content: The textual content of the message.
 * - subject: The subject of the message.
 * - createdAt: The timestamp when the message was created.
 * - updatedAt: The timestamp when the message was last updated.
 * - messageStatusTypeId: The identifier representing the current status of the message (e.g., sent, pending).
 * - to: A list of primary recipients for the message, represented as {@code EmailContact} objects.
 * - cc: A list of secondary recipients (CC) for the message, represented as {@code EmailContact} objects.
 *
 * Serialization:
 * - The {@code createdAt} and {@code updatedAt} fields are serialized using the date-time pattern
 *   defined in {@code DateUtil.PATTERN_DATE_TIME}.
 *
 * Behaviors:
 * - The record provides an overridden {@code toString} method to display a string representation
 *   of the message details, including all fields.
 */
public record MessageRecordTO(
        UUID id,
        UUID templateDefinedId,
        String from,
        String content,
        String subject,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime updatedAt,
        UUID messageStatusTypeId,
        List<EmailContact> to,
        List<EmailContact> cc
) {

        @Override
        public String toString() {
                return "MessageRecordTO{" +
                        "id=" + id +
                        ", templateDefinedId=" + templateDefinedId +
                        ", from='" + from + '\'' +
                        ", content='" + content + '\'' +
                        ", subject='" + subject + '\'' +
                        ", createdAt=" + createdAt +
                        ", updatedAt=" + updatedAt +
                        ", messageStatusTypeId=" + messageStatusTypeId +
                        ", to=" + to +
                        ", cc=" + cc +
                        '}';
        }
}
