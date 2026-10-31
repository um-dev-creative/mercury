package com.umdc.mercury.api.v1.to;

import com.umdc.mercury.constant.DeliveryStatusType;

import java.time.Instant;
import java.util.UUID;

/**
 * State of a mail accepted by {@code POST /api/v1/mail}.
 *
 * @param id        the id returned when it was accepted
 * @param status    {@code OPENED} (queued), {@code SENT} (handed to the SMTP server) or {@code FAILED}
 * @param createdAt when it was accepted
 * @param updatedAt when the status last changed
 */
public record MailStatusResponse(UUID id, DeliveryStatusType status, Instant createdAt, Instant updatedAt) {
}
