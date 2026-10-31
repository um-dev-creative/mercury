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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageChannelRouter routing behavior")
class MessageChannelRouterTest {

    @Mock
    private SmsChannelService smsService;

    @Mock
    private TelegramChannelService telegramService;

    @Mock
    private WhatsAppChannelService whatsAppChannelService;

    @Mock
    private PushChannelService pushChannelService;

    @Mock
    private TemplateDefinedService templateDefinedService;

    private MessageChannelRouter router;

    @Test
    @DisplayName("routeSms resolves the template and delegates to SmsChannelService")
    void routeSms_delegatesWithResolvedTemplate() {
        router = new MessageChannelRouter(smsService, telegramService, whatsAppChannelService, pushChannelService, templateDefinedService);
        UUID templateId = UUID.randomUUID();
        SmsMessageDocument message = new SmsMessageDocument();
        message.setTemplateDefinedId(templateId);
        TemplateDefinedTO template = new TemplateDefinedTO(templateId, null, null, null, null, null, null, null, null);
        when(templateDefinedService.find(templateId)).thenReturn(template);

        router.routeSms(message);

        verify(smsService).send(message, template);
    }

    @Test
    @DisplayName("routeTelegram resolves the template and delegates to TelegramChannelService")
    void routeTelegram_delegatesWithResolvedTemplate() {
        router = new MessageChannelRouter(smsService, telegramService, whatsAppChannelService, pushChannelService, templateDefinedService);
        UUID templateId = UUID.randomUUID();
        TelegramMessageDocument message = new TelegramMessageDocument();
        message.setTemplateDefinedId(templateId);
        TemplateDefinedTO template = new TemplateDefinedTO(templateId, null, null, null, null, null, null, null, null);
        when(templateDefinedService.find(templateId)).thenReturn(template);

        router.routeTelegram(message);

        verify(telegramService).send(message, template);
    }

    @Test
    @DisplayName("routeWhatsApp resolves the template and delegates to WhatsAppChannelService")
    void routeWhatsApp_delegatesWithResolvedTemplate() {
        router = new MessageChannelRouter(smsService, telegramService, whatsAppChannelService, pushChannelService, templateDefinedService);
        UUID templateId = UUID.randomUUID();
        WhatsAppMessageDocument message = new WhatsAppMessageDocument();
        message.setTemplateDefinedId(templateId);
        TemplateDefinedTO template = new TemplateDefinedTO(templateId, null, null, null, null, null, null, null, null);
        when(templateDefinedService.find(templateId)).thenReturn(template);

        router.routeWhatsApp(message);

        verify(whatsAppChannelService).send(message, template);
    }

    @Test
    @DisplayName("routePush resolves the template and delegates to PushChannelService")
    void routePush_delegatesWithResolvedTemplate() {
        router = new MessageChannelRouter(smsService, telegramService, whatsAppChannelService, pushChannelService, templateDefinedService);
        UUID templateId = UUID.randomUUID();
        PushNotificationMessageDocument message = new PushNotificationMessageDocument();
        message.setTemplateDefinedId(templateId);
        TemplateDefinedTO template = new TemplateDefinedTO(templateId, null, null, null, null, null, null, null, null);
        when(templateDefinedService.find(templateId)).thenReturn(template);

        router.routePush(message);

        verify(pushChannelService).send(message, template);
    }

    @Test
    @DisplayName("routing tolerates a template lookup failure by passing a null template")
    void route_templateLookupFails_passesNullTemplate() {
        router = new MessageChannelRouter(smsService, telegramService, whatsAppChannelService, pushChannelService, templateDefinedService);
        UUID templateId = UUID.randomUUID();
        SmsMessageDocument message = new SmsMessageDocument();
        message.setTemplateDefinedId(templateId);
        when(templateDefinedService.find(templateId)).thenThrow(new RuntimeException("template not found"));

        router.routeSms(message);

        verify(smsService).send(message, null);
    }
}
