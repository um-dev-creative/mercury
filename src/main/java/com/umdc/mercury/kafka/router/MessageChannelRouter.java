package com.umdc.mercury.kafka.router;

import com.umdc.mercury.api.v1.service.TemplateDefinedService;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.document.WhatsAppMessageDocument;
import com.umdc.mercury.kafka.consumer.service.SmsChannelService;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.kafka.consumer.service.WhatsAppChannelService;
import com.umdc.mercury.kafka.to.WhatsAppMessageTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Dispatches an inbound channel message, consumed from its Kafka topic by
 * {@link com.umdc.mercury.kafka.listener.MultiChannelListener}, to the {@link com.umdc.mercury.kafka.consumer.service.ChannelService}
 * responsible for persisting/sending it, resolving the associated template first.
 *
 * <p>Email is intentionally not routed here: it is consumed and persisted through its
 * own dedicated listener/service pair independent of this router.</p>
 */
@Service
public class MessageChannelRouter {

    private static final Logger logger = LoggerFactory.getLogger(MessageChannelRouter.class);

    private final SmsChannelService smsService;
    private final TelegramChannelService telegramService;
    private final WhatsAppChannelService whatsAppChannelService;
    private final TemplateDefinedService templateDefinedService;

    public MessageChannelRouter(
            SmsChannelService smsService,
            TelegramChannelService telegramService,
            WhatsAppChannelService whatsAppChannelService,
            TemplateDefinedService templateDefinedService) {
        this.smsService = smsService;
        this.telegramService = telegramService;
        this.whatsAppChannelService = whatsAppChannelService;
        this.templateDefinedService = templateDefinedService;
    }

    public void routeSms(SmsMessageDocument message) {
        TemplateDefinedTO template = resolveTemplate(message.getTemplateDefinedId());
        smsService.send(message, template);
    }

    public void routeTelegram(TelegramMessageDocument message) {
        TemplateDefinedTO template = resolveTemplate(message.getTemplateDefinedId());
        telegramService.send(message, template);
    }

    public void routeWhatsApp(WhatsAppMessageTO message) {
        TemplateDefinedTO template = resolveTemplate(message.templateDefinedId());
        whatsAppChannelService.send(toDocument(message), template);
    }

    private TemplateDefinedTO resolveTemplate(UUID templateDefinedId) {
        try {
            return templateDefinedService.find(templateDefinedId);
        } catch (RuntimeException ex) {
            logger.warn("Unable to resolve template. templateDefinedId={}, reason={}", templateDefinedId, ex.getMessage());
            return null;
        }
    }

    private WhatsAppMessageDocument toDocument(WhatsAppMessageTO message) {
        WhatsAppMessageDocument document = new WhatsAppMessageDocument();
        document.setCampaignId(message.campaignId());
        document.setUserId(message.userId());
        document.setTemplateDefinedId(message.templateDefinedId());
        document.setSendDate(message.sendDate());
        document.setParams(message.params());
        document.setPhoneNumber(message.phoneNumber());
        document.setMessageType(message.messageType());
        document.setContent(message.content());
        document.setTemplateName(message.templateName());
        document.setTemplateLanguage(message.templateLanguage());
        document.setDeliveryStatus(DeliveryStatusType.OPENED);
        document.setChannelType(ChannelType.WHATSAPP);
        return document;
    }
}
