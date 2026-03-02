package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.RecipientTO;
import com.prx.mercury.constant.ChannelType;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.EmailMessageDocument;
import com.prx.mercury.jpa.nosql.document.SmsMessageDocument;
import com.prx.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.prx.mercury.kafka.to.PushNotificationMessageTO;
import com.prx.mercury.kafka.to.WhatsAppMessageTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public CampaignMessageFactory() {
        //default constructor
    }

    /**
     * Returns the Kafka topic name for the given channel code.
     *
     * @param channelCode the channel code (e.g. "email", "sms")
     * @return fully-qualified topic name
     */
    public String topicFor(String channelCode) {
        return "mercury-" + channelCode + "-messages";
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
        DeliveryStatusType initialStatus = DeliveryStatusType.OPENED;
        LocalDateTime now = LocalDateTime.now();

        return switch (type) {
            case EMAIL    -> createEmailMessage(recipient, params, templateId, initialStatus, now);
            case SMS      -> createSmsMessage(campaignId, recipient, params, templateId, initialStatus, now);
            case TELEGRAM -> createTelegramMessage(campaignId, recipient, params, templateId, initialStatus, now);
            case WHATSAPP -> createWhatsAppMessage(campaignId, recipient, params, templateId, now);
            case PUSH     -> createPushNotificationMessage(campaignId, recipient, params, templateId, now);
        };
    }

    // ── per-channel builders ──────────────────────────────────────────────────

    private EmailMessageDocument createEmailMessage(
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId,
            DeliveryStatusType initialStatus,
            LocalDateTime now) {

        UUID userId = resolveUserId(recipient);

        List<EmailContact> to = List.of(new EmailContact(recipient.identifier(), recipient.name(), null));
        List<EmailContact> cc = List.of();

        String from    = stringParam(params, CampaignService.FROM_KEY);
        String subject = stringParam(params, CampaignService.SUBJECT_KEY);
        String body    = stringParam(params, CampaignService.BODY_KEY);

        return new EmailMessageDocument(
                null,
                UUID.randomUUID(),
                templateId,
                userId,
                from,
                to,
                cc,
                subject,
                body,
                now,
                params,
                initialStatus
        );
    }

    private SmsMessageDocument createSmsMessage(
            UUID campaignId,
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId,
            DeliveryStatusType initialStatus,
            LocalDateTime now) {

        SmsMessageDocument sms = new SmsMessageDocument();
        sms.setCampaignId(campaignId);
        sms.setChannelType(ChannelType.SMS);
        sms.setDeliveryStatus(initialStatus);
        sms.setSendDate(now);
        sms.setParams(params);
        sms.setTemplateDefinedId(templateId);
        sms.setUserId(null);
        sms.setPhoneNumber(recipient.identifier());
        sms.setMessage(stringParam(params, CampaignService.MESSAGE_KEY));
        sms.setSenderId(stringParam(params, CampaignService.SENDER_ID_KEY));
        return sms;
    }

    private TelegramMessageDocument createTelegramMessage(
            UUID campaignId,
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId,
            DeliveryStatusType initialStatus,
            LocalDateTime now) {

        TelegramMessageDocument tg = new TelegramMessageDocument();
        tg.setCampaignId(campaignId);
        tg.setChannelType(ChannelType.TELEGRAM);
        tg.setDeliveryStatus(initialStatus);
        tg.setSendDate(now);
        tg.setParams(params);
        tg.setTemplateDefinedId(templateId);

        try {
            tg.setChatId(Long.parseLong(recipient.identifier()));
        } catch (NumberFormatException ex) {
            logger.debug("Invalid chat id '{}' for recipient, setting null", recipient.identifier(), ex);
            tg.setChatId(null);
        }

        tg.setMessage(stringParam(params, CampaignService.MESSAGE_KEY));
        tg.setParseMode(stringParam(params, CampaignService.PARSE_MODE_KEY));
        return tg;
    }

    private WhatsAppMessageTO createWhatsAppMessage(
            UUID campaignId,
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId,
            LocalDateTime now) {

        String phoneNumber      = recipient.identifier();
        String messageType      = stringParam(params, "message_type", "text");
        String content          = stringParam(params, CampaignService.MESSAGE_KEY);
        String templateName     = stringParam(params, "template_name");
        String templateLanguage = stringParam(params, "template_language");

        return new WhatsAppMessageTO(
                templateId,
                resolveUserId(recipient),
                phoneNumber,
                messageType,
                content,
                templateName,
                templateLanguage,
                null,   // template_components
                null,   // media_url
                null,   // media_type
                now,
                params,
                campaignId
        );
    }

    private PushNotificationMessageTO createPushNotificationMessage(
            UUID campaignId,
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId,
            LocalDateTime now) {

        String deviceToken = recipient.identifier();
        String platform    = stringParam(params, "platform", "web");
        String title       = stringParam(params, "title");
        String body        = stringParam(params, CampaignService.MESSAGE_KEY);

        return new PushNotificationMessageTO(
                templateId,
                resolveUserId(recipient),
                deviceToken,
                platform,
                title,
                body,
                null, null, null, null, null, null,
                now,
                params,
                campaignId
        );
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

