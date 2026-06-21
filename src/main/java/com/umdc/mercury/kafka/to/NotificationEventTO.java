package com.umdc.mercury.kafka.to;

/**
 * Unified message representation for various communication channels (WhatsApp, Telegram, Email, Push, SMS).
 * This record corresponds to the NotificationEvent Avro schema.
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
        Context context,
        DeliveryInstructions delivery_instructions
    ) {}

    public record Context() {}

    public record DeliveryInstructions() {}
}
