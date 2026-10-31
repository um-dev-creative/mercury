package com.umdc.mercury.jpa.nosql.document;

import com.umdc.mercury.constant.DeliveryStatusType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Delivery log of one mail accepted through {@code POST /api/v1/mail}: it outlives the in-flight
 * {@link EmailMessageDocument} (deleted once the message is archived) so a caller can ask for the state
 * of its mail, and its id doubles as the idempotency claim ({@code caller:Idempotency-Key}). It holds no
 * recipient, subject or parameter: only what is needed to answer and to detect a reused key.
 *
 * @param id            {@code caller:key} when the caller sent an {@code Idempotency-Key}, else the message id
 * @param messageId     the id returned to the caller
 * @param caller        login-client alias that submitted it
 * @param requestHash   SHA-256 of the canonical request, to tell a replay from a reused key
 * @param status        last known delivery state
 * @param createdAt     when it was accepted; the TTL index drops the row after the retention period
 * @param updatedAt     when {@code status} last changed
 */
@Document(collection = "mail_tracking")
public record MailTrackingDocument(
        @Id String id,
        @Indexed(unique = true) UUID messageId,
        String caller,
        String requestHash,
        DeliveryStatusType status,
        @Indexed(expireAfter = "${umdc.mail.tracking-retention:30d}") Instant createdAt,
        Instant updatedAt) {

    public MailTrackingDocument withStatus(DeliveryStatusType newStatus, Instant at) {
        return new MailTrackingDocument(id, messageId, caller, requestHash, newStatus, createdAt, at);
    }
}
