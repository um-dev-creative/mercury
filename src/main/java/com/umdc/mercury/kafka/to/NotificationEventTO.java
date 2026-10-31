package com.umdc.mercury.kafka.to;

import java.util.Map;

/**
 * Unified message representation for various communication channels (WhatsApp, Telegram, Email, Push, SMS).
 * This record corresponds to the NotificationEvent Avro schema; see docs/topic-general-message.md.
 *
 * <p>SMS, Telegram, WhatsApp and Push share one Kafka topic and are told apart by
 * {@link Routing#channel()}. {@link Payload#context()} holds the template parameters and
 * {@link Payload#delivery_instructions()} the channel-specific options.</p>
 */
public record NotificationEventTO(
    Header header,
    Routing routing,
    Content content,
    Payload payload
) {
    public record Header(
        String campaignId,
        String messageId,
        String timestamp,
        String traceId
    ) {}

    public record Routing(
        String channel,
        String recipient_id,
        String target_identifier
    ) {}

    public record Content(
        String templateId,
        String templateName,
        String language,
        String body,
        String mediaUrl,      // Can be null
        String mediatype
    ) {}

    public record Payload(
        Map<String, Object> context,
        Map<String, Object> delivery_instructions
    ) {}
}
