package com.umdc.mercury.provider;

import com.umdc.mercury.constant.DeliveryFailureReason;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Delivers queued push messages through the Expo Push API, the counterpart of {@link TelegramMessageDispatcher}. It decides, per message, one of
 * four things and records it on the document (the caller persists the documents it returns):
 * <ul>
 *   <li>Expo accepted it: {@code SENT}, with the receipt id kept so the final answer can be read later ({@link #checkReceipts});</li>
 *   <li>Expo or the token says it cannot be delivered: {@code FAILED} with a {@link DeliveryFailureReason}; {@code DEVICE_NOT_REGISTERED}
 *       is how the caller learns a device token is dead;</li>
 *   <li>a transient problem (429, 5xx, timeout, {@code MessageRateExceeded}): the message stays {@code OPENED} with one more attempt counted, and
 *       becomes {@code FAILED} when {@code max-delivery-attempts} is reached (bounded retries);</li>
 *   <li>an operator problem (401/403: access token): nothing changes, the messages wait until it is fixed.</li>
 * </ul>
 * Expo's {@code ok} ticket only means "accepted": {@code DeviceNotRegistered} normally arrives in the receipt, minutes later, which is why
 * {@link #checkReceipts} can turn a {@code SENT} message into {@code FAILED}. The device token is never logged.
 */
@Component
public class ExpoPushDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ExpoPushDispatcher.class);
    /** Expo clears receipts after 24 hours. */
    private static final int SINGLE = 1;
    private static final Duration RECEIPT_RETENTION = Duration.ofHours(24);

    private final ExpoPushClient client;
    private final ExpoPushProperties properties;

    public ExpoPushDispatcher(ExpoPushClient client, ExpoPushProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /** Whether the Expo delivery is switched on ({@code umdc.mercury.push.expo.enabled}). */
    public boolean isEnabled() {
        return properties.enabled();
    }

    /** How long after the send the receipts are read. */
    public Duration receiptDelay() {
        return properties.receiptDelay();
    }

    /**
     * Sends {@code messages} (all {@code OPENED}) in batches of at most 100.
     *
     * @return the documents whose state changed (to persist); documents not returned were left untouched
     * @throws IllegalStateException when called while the channel is off
     */
    public List<PushNotificationMessageDocument> dispatch(List<PushNotificationMessageDocument> messages) {
        if (!isEnabled()) {
            throw new IllegalStateException("Push dispatch requested while the Expo channel is off");
        }
        final var changed = new ArrayList<PushNotificationMessageDocument>();
        final var sendable = new ArrayList<PushNotificationMessageDocument>();
        for (PushNotificationMessageDocument message : messages) {
            if (ExpoPushPayload.isExpoToken(message.getDeviceToken()) && ExpoPushPayload.hasText(message)) {
                sendable.add(message);
            } else {
                fail(message, ExpoPushPayload.isExpoToken(message.getDeviceToken()) ? DeliveryFailureReason.INVALID_MESSAGE : DeliveryFailureReason.INVALID_TOKEN, changed);
            }
        }
        for (int from = 0; from < sendable.size(); from += ExpoPushClient.MAX_MESSAGES) {
            final var chunk = sendable.subList(from, Math.min(from + ExpoPushClient.MAX_MESSAGES, sendable.size()));
            if (!sendChunk(chunk, changed)) {
                break;
            }
        }
        return changed;
    }

    /** @return false when the remaining batches must wait (the access token is refused) */
    private boolean sendChunk(List<PushNotificationMessageDocument> chunk, List<PushNotificationMessageDocument> changed) {
        final var result = client.send(chunk.stream().map(message -> ExpoPushPayload.of(message, properties.androidChannelId())).toList());
        switch (result) {
            case ExpoPushResult.Tickets tickets when tickets.tickets().size() == chunk.size() -> {
                for (int i = 0; i < chunk.size(); i++) {
                    applyTicket(chunk.get(i), tickets.tickets().get(i), changed);
                }
            }
            case ExpoPushResult.Tickets ignored -> {
                LOG.warn("Expo answered {} ticket(s) for {} message(s); treating the batch as transient", ignored.tickets().size(), chunk.size());
                chunk.forEach(message -> transientFailure(message, DeliveryFailureReason.PROVIDER_ERROR, changed));
            }
            case ExpoPushResult.Rejected rejected -> {
                LOG.warn("Expo rejected a batch of {} message(s): status={}, code={}", chunk.size(), rejected.status(), rejected.code());
                if (chunk.size() > SINGLE) {
                    // one bad message (or tokens of two Expo projects) must not take the others down: send them one by one
                    for (PushNotificationMessageDocument message : chunk) {
                        if (!sendChunk(List.of(message), changed)) {
                            return false;
                        }
                    }
                } else {
                    fail(chunk.getFirst(), DeliveryFailureReason.PROVIDER_REJECTED, changed);
                }
            }
            case ExpoPushResult.Unauthorized unauthorized -> {
                LOG.error("Expo refused the access token (HTTP {}); {} push message(s) wait until umdc.mercury.push.expo.access-token is fixed",
                        unauthorized.status(), chunk.size());
                return false;
            }
            case ExpoPushResult.Unavailable unavailable ->
                    chunk.forEach(message -> transientFailure(message, DeliveryFailureReason.PROVIDER_UNAVAILABLE, changed));
            case ExpoPushResult.Receipts ignored -> throw new IllegalStateException("A send answered receipts");
        }
        return true;
    }

    private void applyTicket(PushNotificationMessageDocument message, ExpoPushResult.Ticket ticket, List<PushNotificationMessageDocument> changed) {
        if (ticket.ok()) {
            message.setDeliveryStatus(DeliveryStatusType.SENT);
            message.setProviderReceiptId(ticket.id());
            message.setSentAt(now());
            message.setFailureReason(null);
            changed.add(message);
        } else if (ExpoPushPayload.RATE_EXCEEDED.equals(ticket.error())) {
            transientFailure(message, DeliveryFailureReason.MESSAGE_RATE_EXCEEDED, changed);
        } else {
            fail(message, ExpoPushPayload.reasonOf(ticket.error()), changed);
        }
    }

    /**
     * Reads the Expo receipts of {@code messages} (all {@code SENT} with a receipt id) in batches of at most 1000. A receipt with an error turns the
     * message {@code FAILED} (this is where {@code DeviceNotRegistered} usually shows up); an {@code ok} receipt only marks it read; a missing
     * receipt is asked again until Expo drops receipts (24 hours after the send).
     *
     * @return the documents whose state changed (to persist)
     */
    public List<PushNotificationMessageDocument> checkReceipts(List<PushNotificationMessageDocument> messages) {
        final var changed = new ArrayList<PushNotificationMessageDocument>();
        for (int from = 0; from < messages.size(); from += ExpoPushClient.MAX_RECEIPTS) {
            final var chunk = messages.subList(from, Math.min(from + ExpoPushClient.MAX_RECEIPTS, messages.size()));
            final var result = client.receipts(chunk.stream().map(PushNotificationMessageDocument::getProviderReceiptId).toList());
            if (!(result instanceof ExpoPushResult.Receipts receipts)) {
                LOG.warn("Could not read {} Expo receipt(s): {}", chunk.size(), result.getClass().getSimpleName());
                continue;
            }
            chunk.forEach(message -> applyReceipt(message, receipts.byId().get(message.getProviderReceiptId()), changed));
        }
        return changed;
    }

    private void applyReceipt(PushNotificationMessageDocument message, ExpoPushResult.Ticket receipt, List<PushNotificationMessageDocument> changed) {
        if (Objects.isNull(receipt)) {
            if (Objects.nonNull(message.getSentAt()) && message.getSentAt().isBefore(now().minus(RECEIPT_RETENTION))) {
                message.setReceiptCheckedAt(now());   // Expo no longer has it: stop asking
                changed.add(message);
            }
            return;
        }
        message.setReceiptCheckedAt(now());
        if (!receipt.ok()) {
            message.setDeliveryStatus(DeliveryStatusType.FAILED);
            message.setFailureReason(ExpoPushPayload.reasonOf(receipt.error()));
        }
        changed.add(message);
    }

    private void fail(PushNotificationMessageDocument message, DeliveryFailureReason reason, List<PushNotificationMessageDocument> changed) {
        message.setDeliveryStatus(DeliveryStatusType.FAILED);
        message.setFailureReason(reason);
        changed.add(message);
    }

    /** Counts a failed attempt; the message stays {@code OPENED} until {@code max-delivery-attempts}, then it is {@code FAILED} with {@code reason}. */
    private void transientFailure(PushNotificationMessageDocument message, DeliveryFailureReason reason, List<PushNotificationMessageDocument> changed) {
        final int attempts = Objects.requireNonNullElse(message.getDeliveryAttempts(), 0) + 1;
        message.setDeliveryAttempts(attempts);
        if (attempts >= properties.maxDeliveryAttempts()) {
            message.setDeliveryStatus(DeliveryStatusType.FAILED);
            message.setFailureReason(reason);
        }
        changed.add(message);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
