package com.umdc.mercury.kafka.router;

import com.umdc.mercury.api.v1.service.TemplateDefinedService;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.document.WhatsAppMessageDocument;
import com.umdc.mercury.kafka.consumer.service.PushChannelService;
import com.umdc.mercury.kafka.consumer.service.SmsChannelService;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.kafka.consumer.service.WhatsAppChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Dispatches an inbound channel message, consumed from the unified Kafka topic by
 * {@link com.umdc.mercury.kafka.listener.NotificationEventListener}, to the {@link com.umdc.mercury.kafka.consumer.service.ChannelService}
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
    private final PushChannelService pushChannelService;
    private final TemplateDefinedService templateDefinedService;

    public MessageChannelRouter(
            SmsChannelService smsService,
            TelegramChannelService telegramService,
            WhatsAppChannelService whatsAppChannelService,
            PushChannelService pushChannelService,
            TemplateDefinedService templateDefinedService) {
        this.smsService = smsService;
        this.telegramService = telegramService;
        this.whatsAppChannelService = whatsAppChannelService;
        this.pushChannelService = pushChannelService;
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

    public void routeWhatsApp(WhatsAppMessageDocument message) {
        TemplateDefinedTO template = resolveTemplate(message.getTemplateDefinedId());
        whatsAppChannelService.send(message, template);
    }

    public void routePush(PushNotificationMessageDocument message) {
        TemplateDefinedTO template = resolveTemplate(message.getTemplateDefinedId());
        pushChannelService.send(message, template);
    }

    private TemplateDefinedTO resolveTemplate(UUID templateDefinedId) {
        try {
            return templateDefinedService.find(templateDefinedId);
        } catch (RuntimeException ex) {
            logger.warn("Unable to resolve template. templateDefinedId={}, reason={}", templateDefinedId, ex.getMessage());
            return null;
        }
    }
}
