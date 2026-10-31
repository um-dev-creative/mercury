package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.api.v1.to.RecipientTO;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Factory that builds the channel-specific message payload for a single campaign recipient
 * and resolves the Kafka topic name for a given channel code.
 *
 * <p>Extracting this responsibility from {@link CampaignServiceImpl} keeps the service
 * focused on campaign orchestration while this component owns message-construction details.
 */
@Component
public class CampaignMessageFactory {

    private static final Logger logger = LoggerFactory.getLogger(CampaignMessageFactory.class);
    private static final String USER_ID_KEY = "userId";

    private static final String EMAIL_TOPIC = "mercury-email-messages";
    private static final String DEFAULT_MEDIA_TYPE = "NONE";
    private static final List<String> PUSH_INSTRUCTIONS =
            List.of("title", "icon", "click_action", "priority", "badge", "sound");

    /** Topic shared by SMS, Telegram, WhatsApp and Push; consumers split them by routing.channel. */
    private final String unifiedTopic;

    public CampaignMessageFactory(@Value("${umdc.consumer.topics.notification}") String unifiedTopic) {
        this.unifiedTopic = unifiedTopic;
    }

    /**
     * Returns the Kafka topic name for the given channel code.
     * Email keeps its own topic; every other channel publishes to the unified one.
     *
     * @param channelCode the channel code (e.g. "email", "sms")
     * @return fully-qualified topic name
     */
    public String topicFor(String channelCode) {
        return ChannelType.fromCode(channelCode) == ChannelType.EMAIL ? EMAIL_TOPIC : unifiedTopic;
    }

    /**
     * Creates the appropriate message payload for the given channel, campaign, recipient and params.
     *
     * @param channelCode the channel code
     * @param campaignId  the campaign UUID
     * @param recipient   the target recipient
     * @param params      template parameters
     * @param templateId  the template UUID
     * @return channel-specific message object ready to publish on Kafka
     * @throws IllegalStateException if the channel code is not recognised
     */
    public Object createMessage(
            String channelCode,
            UUID campaignId,
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId) {

        ChannelType type = ChannelType.fromCode(channelCode);

        return switch (type) {
            case EMAIL -> createEmailMessage(recipient, params, templateId,
                    LocalDateTime.now(ZoneId.of(ZoneOffset.UTC.getId())));
            case SMS, TELEGRAM, WHATSAPP, PUSH -> createNotificationEvent(type, campaignId, recipient, params, templateId);
        };
    }

    // ── per-channel builders ──────────────────────────────────────────────────

    private EmailMessageTO createEmailMessage(
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId,
            LocalDateTime now) {

        UUID userId = resolveUserId(recipient);

        List<EmailContact> to = List.of(new EmailContact(recipient.identifier(), recipient.name(), null));
        List<EmailContact> cc = List.of();

        String from    = stringParam(params, CampaignService.FROM_KEY);
        String subject = stringParam(params, CampaignService.SUBJECT_KEY);
        String body    = stringParam(params, CampaignService.BODY_KEY);

        return new EmailMessageTO(
                templateId,
                userId,
                from,
                to,
                cc,
                subject,
                body,
                now,
                params
        );
    }

    private NotificationEventTO createNotificationEvent(
            ChannelType type,
            UUID campaignId,
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId) {

        String messageId = UUID.randomUUID().toString();
        UUID userId = resolveUserId(recipient);

        return new NotificationEventTO(
                new NotificationEventTO.Header(
                        campaignId.toString(), messageId, Instant.now().toString(), messageId),
                new NotificationEventTO.Routing(
                        type.name(), userId != null ? userId.toString() : null, recipient.identifier()),
                new NotificationEventTO.Content(
                        templateId.toString(),
                        stringParam(params, "template_name"),
                        stringParam(params, "template_language"),
                        stringParam(params, CampaignService.MESSAGE_KEY),
                        stringParam(params, "media_url"),
                        stringParam(params, "media_type", DEFAULT_MEDIA_TYPE)),
                new NotificationEventTO.Payload(
                        params != null ? params : Map.of(),
                        deliveryInstructions(type, params)));
    }

    /** The options {@code NotificationEventConsumerServiceImpl} reads back for each channel. */
    private static Map<String, Object> deliveryInstructions(ChannelType type, Map<String, Object> params) {
        var instructions = new HashMap<String, Object>();
        switch (type) {
            case SMS -> instructions.put("sender_id", stringParam(params, CampaignService.SENDER_ID_KEY));
            case TELEGRAM -> instructions.put("parse_mode", stringParam(params, CampaignService.PARSE_MODE_KEY));
            case WHATSAPP -> instructions.put("message_type", stringParam(params, "message_type", "text"));
            case PUSH -> {
                instructions.put("platform", stringParam(params, "platform", "web"));
                for (String key : PUSH_INSTRUCTIONS) {
                    instructions.put(key, params != null ? params.get(key) : null);
                }
            }
            case EMAIL -> throw new IllegalArgumentException("Email has its own message type");
        }
        instructions.values().removeIf(Objects::isNull);
        return instructions;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Extracts a String value from params, or {@code null} if absent / wrong type. */
    private static String stringParam(Map<String, Object> params, String key) {
        return params != null && params.get(key) instanceof String s ? s : null;
    }

    /** Extracts a String value from params, returning {@code defaultValue} when absent. */
    private static String stringParam(Map<String, Object> params, String key, String defaultValue) {
        String v = stringParam(params, key);
        return v != null ? v : defaultValue;
    }

    /**
     * Resolves the userId from a recipient's customParams.
     * Accepts both {@link UUID} values and UUID-formatted strings.
     */
    private UUID resolveUserId(RecipientTO recipient) {
        if (recipient.customParams() == null) {
            return null;
        }
        Object raw = recipient.customParams().get(USER_ID_KEY);
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        if (raw instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ex) {
                logger.debug("Invalid UUID for userId in recipient customParams: {}", raw, ex);
            }
        }
        return null;
    }
}

