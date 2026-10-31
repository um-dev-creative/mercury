package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.MessageDocument;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.document.WhatsAppMessageDocument;
import com.umdc.mercury.kafka.router.MessageChannelRouter;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Turns a message from the unified topic into the channel's document and hands it to
 * {@link MessageChannelRouter}. Records that can never be delivered (unknown channel, missing
 * messageId, invalid target) are logged and discarded; retrying them would not help.
 *
 * <p>{@code messageId} becomes the document id, so a Kafka redelivery overwrites the same document
 * instead of creating a second message. Target identifiers are personal data and are not logged.</p>
 */
@Service
public class NotificationEventConsumerServiceImpl implements NotificationEventConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumerServiceImpl.class);
    private static final Pattern E164_PHONE = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern TELEGRAM_CHAT_ID = Pattern.compile("^-?\\d{1,18}$");

    private final MessageChannelRouter messageChannelRouter;

    public NotificationEventConsumerServiceImpl(MessageChannelRouter messageChannelRouter) {
        this.messageChannelRouter = messageChannelRouter;
    }

    @Override
    public void process(NotificationEventTO event) {
        String rawChannel = event.routing().channel();
        String messageId = event.header().messageId();
        logger.info("Processing NotificationEvent: campaignId={}, messageId={}, channel={}, traceId={}",
                event.header().campaignId(), messageId, rawChannel, event.header().traceId());

        ChannelType channelType;
        try {
            channelType = ChannelType.fromCode(rawChannel);
        } catch (IllegalArgumentException _) {
            logger.warn("Unknown channel type '{}' for messageId={}, discarding", rawChannel, messageId);
            return;
        }

        if (channelType == ChannelType.EMAIL) {
            logger.warn("EMAIL is consumed from its own topic, not the unified one; discarding messageId={}", messageId);
            return;
        }
        if (Objects.isNull(messageId) || messageId.isBlank()) {
            logger.warn("Message without messageId on channel {}, discarding", channelType);
            return;
        }
        if (!isValidTarget(channelType, event.routing().target_identifier())) {
            logger.warn("Invalid target identifier for channel {}, discarding messageId={}", channelType, messageId);
            return;
        }

        switch (channelType) {
            case SMS -> messageChannelRouter.routeSms(toSms(event));
            case TELEGRAM -> messageChannelRouter.routeTelegram(toTelegram(event));
            case WHATSAPP -> messageChannelRouter.routeWhatsApp(toWhatsApp(event));
            case PUSH -> messageChannelRouter.routePush(toPush(event));
            case EMAIL -> throw new IllegalStateException("EMAIL was handled above");
        }
    }

    private static boolean isValidTarget(ChannelType channelType, String target) {
        if (Objects.isNull(target) || target.isBlank()) {
            return false;
        }
        return switch (channelType) {
            case SMS, WHATSAPP -> E164_PHONE.matcher(target).matches();
            case TELEGRAM -> TELEGRAM_CHAT_ID.matcher(target).matches();
            case PUSH, EMAIL -> true;
        };
    }

    private SmsMessageDocument toSms(NotificationEventTO event) {
        SmsMessageDocument document = withCommonFields(new SmsMessageDocument(), event, ChannelType.SMS);
        document.setPhoneNumber(event.routing().target_identifier());
        document.setMessage(event.content().body());
        document.setSenderId(instruction(event, "sender_id"));
        return document;
    }

    private TelegramMessageDocument toTelegram(NotificationEventTO event) {
        TelegramMessageDocument document = withCommonFields(new TelegramMessageDocument(), event, ChannelType.TELEGRAM);
        document.setChatId(Long.valueOf(event.routing().target_identifier()));
        document.setMessage(event.content().body());
        document.setParseMode(instruction(event, "parse_mode"));
        return document;
    }

    private WhatsAppMessageDocument toWhatsApp(NotificationEventTO event) {
        WhatsAppMessageDocument document = withCommonFields(new WhatsAppMessageDocument(), event, ChannelType.WHATSAPP);
        document.setPhoneNumber(event.routing().target_identifier());
        document.setMessageType(instruction(event, "message_type", "text"));
        document.setContent(event.content().body());
        document.setTemplateName(event.content().templateName());
        document.setTemplateLanguage(event.content().language());
        return document;
    }

    private PushNotificationMessageDocument toPush(NotificationEventTO event) {
        PushNotificationMessageDocument document =
                withCommonFields(new PushNotificationMessageDocument(), event, ChannelType.PUSH);
        document.setDeviceToken(event.routing().target_identifier());
        document.setPlatform(instruction(event, "platform", "web"));
        document.setTitle(instruction(event, "title"));
        document.setBody(event.content().body());
        document.setIcon(instruction(event, "icon"));
        document.setImage(event.content().mediaUrl());
        document.setClickAction(instruction(event, "click_action"));
        document.setPriority(instruction(event, "priority", "normal"));
        document.setBadge(instructions(event).get("badge") instanceof Number badge ? badge.intValue() : null);
        document.setSound(instruction(event, "sound"));
        return document;
    }

    private <T extends MessageDocument> T withCommonFields(T document, NotificationEventTO event, ChannelType channelType) {
        document.setId(event.header().messageId());
        document.setCampaignId(toUuid(event.header().campaignId()));
        document.setUserId(toUuid(event.routing().recipient_id()));
        document.setTemplateDefinedId(toUuid(event.content().templateId()));
        document.setSendDate(toSendDate(event.header().timestamp()));
        document.setParams(Objects.nonNull(event.payload()) ? event.payload().context() : null);
        document.setMediaUrl(event.content().mediaUrl());
        document.setMediaType(event.content().mediatype());
        document.setChannelType(channelType);
        document.setDeliveryStatus(DeliveryStatusType.OPENED);
        return document;
    }

    private static Map<String, Object> instructions(NotificationEventTO event) {
        return Objects.nonNull(event.payload()) && Objects.nonNull(event.payload().delivery_instructions())
                ? event.payload().delivery_instructions()
                : Map.of();
    }

    private static String instruction(NotificationEventTO event, String key) {
        return instruction(event, key, null);
    }

    private static String instruction(NotificationEventTO event, String key, String defaultValue) {
        return instructions(event).get(key) instanceof String value ? value : defaultValue;
    }

    private static UUID toUuid(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException _) {
            logger.debug("Ignoring value that is not a UUID: {}", value);
            return null;
        }
    }

    private static LocalDateTime toSendDate(String timestamp) {
        if (Objects.nonNull(timestamp)) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(timestamp), ZoneOffset.UTC);
            } catch (DateTimeParseException _) {
                logger.debug("Ignoring invalid timestamp: {}", timestamp);
            }
        }
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
