package com.prx.mercury.kafka.router;

import com.umdc.mercury.api.v1.service.TemplateDefinedService;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.document.WhatsAppMessageDocument;
import com.umdc.mercury.kafka.consumer.service.PushChannelService;
import com.umdc.mercury.kafka.consumer.service.SmsChannelService;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.kafka.consumer.service.WhatsAppChannelService;
import com.umdc.mercury.kafka.router.MessageChannelRouter;
import com.umdc.mercury.kafka.to.PushNotificationMessageTO;
import com.umdc.mercury.kafka.to.WhatsAppMessageTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("routeWhatsApp converts the TO to a document and delegates to WhatsAppChannelService")
    void routeWhatsApp_convertsAndDelegates() {
        router = new MessageChannelRouter(smsService, telegramService, whatsAppChannelService, pushChannelService, templateDefinedService);
        UUID templateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        WhatsAppMessageTO message = new WhatsAppMessageTO(
                templateId, userId, "+15550001111", "text", "hello",
                "welcome", "en", null, null, null, now, Map.of("k", "v"), campaignId);
        TemplateDefinedTO template = new TemplateDefinedTO(templateId, null, null, null, null, null, null, null, null);
        when(templateDefinedService.find(templateId)).thenReturn(template);

        router.routeWhatsApp(message);

        ArgumentCaptor<WhatsAppMessageDocument> captor = ArgumentCaptor.forClass(WhatsAppMessageDocument.class);
        verify(whatsAppChannelService).send(captor.capture(), eq(template));
        WhatsAppMessageDocument document = captor.getValue();
        assertEquals(campaignId, document.getCampaignId());
        assertEquals(userId, document.getUserId());
        assertEquals(templateId, document.getTemplateDefinedId());
        assertEquals("+15550001111", document.getPhoneNumber());
        assertEquals("text", document.getMessageType());
        assertEquals("hello", document.getContent());
        assertEquals("welcome", document.getTemplateName());
        assertEquals("en", document.getTemplateLanguage());
        assertEquals(DeliveryStatusType.OPENED, document.getDeliveryStatus());
    }

    @Test
    @DisplayName("routePush converts the TO to a document and delegates to PushChannelService")
    void routePush_convertsAndDelegates() {
        router = new MessageChannelRouter(smsService, telegramService, whatsAppChannelService, pushChannelService, templateDefinedService);
        UUID templateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        PushNotificationMessageTO message = new PushNotificationMessageTO(
                templateId, userId, "device-token-1", "android", "Welcome", "Hello there",
                "icon.png", "image.png", "OPEN_APP", "high", 3, "default", now, Map.of("k", "v"), campaignId);
        TemplateDefinedTO template = new TemplateDefinedTO(templateId, null, null, null, null, null, null, null, null);
        when(templateDefinedService.find(templateId)).thenReturn(template);

        router.routePush(message);

        ArgumentCaptor<PushNotificationMessageDocument> captor = ArgumentCaptor.forClass(PushNotificationMessageDocument.class);
        verify(pushChannelService).send(captor.capture(), eq(template));
        PushNotificationMessageDocument document = captor.getValue();
        assertEquals(campaignId, document.getCampaignId());
        assertEquals(userId, document.getUserId());
        assertEquals(templateId, document.getTemplateDefinedId());
        assertEquals("device-token-1", document.getDeviceToken());
        assertEquals("android", document.getPlatform());
        assertEquals("Welcome", document.getTitle());
        assertEquals("Hello there", document.getBody());
        assertEquals("high", document.getPriority());
        assertEquals(3, document.getBadge());
        assertEquals(DeliveryStatusType.OPENED, document.getDeliveryStatus());
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
