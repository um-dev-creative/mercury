package com.umdc.mercury.api.v1.to;

import com.umdc.mercury.constant.DeliveryFailureReason;
import com.umdc.mercury.constant.DeliveryStatusType;

import java.time.Instant;
import java.util.UUID;

/**
 * State of a message accepted by {@code POST /api/v1/mail} or {@code POST /api/v1/messages}.
 *
 * @param id            the id returned when it was accepted
 * @param status        {@code OPENED} (queued), {@code SENT} (handed to the provider) or {@code FAILED}
 * @param createdAt     when it was accepted
 * @param updatedAt     when the status last changed
 * @param failureReason why the message is {@code FAILED} (for push, {@code DEVICE_NOT_REGISTERED} means the device token is dead and must be
 *                      dropped); {@code null} when it did not fail or the channel does not report a reason
 */
public record MailStatusResponse(UUID id, DeliveryStatusType status, Instant createdAt, Instant updatedAt, DeliveryFailureReason failureReason) {

    /** A state without a failure reason (e-mail, and every message that did not fail). */
    public MailStatusResponse(UUID id, DeliveryStatusType status, Instant createdAt, Instant updatedAt) {
        this(id, status, createdAt, updatedAt, null);
    }
}
