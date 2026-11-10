package com.umdc.mercury.ingress;

import com.umdc.mercury.constant.DeliveryStatusType;

/**
 * One delivery channel of the unified ingress ({@code POST /api/v1/messages}). A channel is <em>data plus a provider</em>: its code is
 * the string callers send in {@code channel}, and mercury finds the provider by that code at runtime ({@link ChannelRegistry}), so a new channel
 * is one more Spring bean implementing this interface (and, optionally, a row in the channel catalogue to switch it off), with no change to the
 * endpoint, the request or the callers.
 *
 * <p>A provider only has to <em>queue</em> the message durably ({@link DeliveryStatusType#OPENED}); the delivery, its retries and the final state
 * belong to the channel's scheduler, exactly as for the e-mail and the Kafka channels that already exist.</p>
 */
public interface ChannelProvider {

    /** The channel code callers use, lower case ({@code email}, {@code sms}, {@code whatsapp}, {@code telegram}, {@code push}, ...). */
    String code();

    /** Whether {@code target} is a valid recipient address for this channel (an e-mail address, an E.164 phone number, a chat id...). */
    boolean validTarget(String target);

    /** What the channel needs besides the common fields, or {@code null} when the message is complete; the text becomes the 400. */
    default String missingField(OutboundMessage message) {
        return null;
    }

    /** Queues the message durably as {@code OPENED}. A failure must leave nothing queued, so the caller can retry with the same key. */
    void queue(OutboundMessage message);
}
