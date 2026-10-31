package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.service.CampaignMessageFactory;
import com.umdc.mercury.api.v1.service.CampaignService;
import com.umdc.mercury.api.v1.to.RecipientTO;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.document.WhatsAppMessageDocument;
import com.umdc.mercury.kafka.router.MessageChannelRouter;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventConsumerServiceImpl unit tests")
class NotificationEventConsumerServiceImplTest {

    private static final String TOPIC = "mercury-unified-message";
    private static final String CAMPAIGN_ID = "92a1b7c4-5678-4321-0987-1234567890ab";
    private static final String MESSAGE_ID = "f3d2e1f0-4321-0987-1234-567890123456";
    private static final String TEMPLATE_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String RECIPIENT_ID = "f47ac10b-58cc-4373-a567-0e02b2c3d479";

    /** The representation agreed for the unified topic (docs/topic-general-message.md). */
    private static final String AGREED_JSON = """
            {
              "header": {
                "campaignId": "%s",
                "messageId": "%s",
                "timestamp": "2023-11-15T10:30:00Z",
                "traceId": "a1b2c3d4e5f6g7h8"
              },
              "routing": {
                "channel": "WHATSAPP",
                "recipient_id": "%s",
                "target_identifier": "+34600112233"
              },
              "content": {
                "templateId": "%s",
                "templateName": "welcome_confirmation_v1",
                "language": "es",
                "body": "Hola, gracias por tu compra. Tu pedido está en camino.",
                "mediaUrl": "https://cdn.ejemplo.com/img.jpg",
                "mediatype": "IMAGE"
              },
              "payload": {
                "context": {"order_id": 77},
                "delivery_instructions": {"message_type": "template"}
              }
            }
            """.formatted(CAMPAIGN_ID, MESSAGE_ID, RECIPIENT_ID, TEMPLATE_ID);

    @Mock
    private MessageChannelRouter router;

    private NotificationEventConsumerServiceImpl service() {
        return new NotificationEventConsumerServiceImpl(router);
    }

    private static NotificationEventTO eventFor(String channel, String target, Map<String, Object> instructions) {
        return new NotificationEventTO(
                new NotificationEventTO.Header(CAMPAIGN_ID, MESSAGE_ID, "2026-01-01T10:00:00Z", "trace-1"),
                new NotificationEventTO.Routing(channel, RECIPIENT_ID, target),
                new NotificationEventTO.Content(TEMPLATE_ID, "welcome", "en", "body", null, "NONE"),
                new NotificationEventTO.Payload(Map.of("name", "Ana"), instructions));
    }

    private static NotificationEventTO deserialize(byte[] json) {
        try (JacksonJsonDeserializer<NotificationEventTO> deserializer =
                     new JacksonJsonDeserializer<>(NotificationEventTO.class)) {
            deserializer.setUseTypeHeaders(false);
            return deserializer.deserialize(TOPIC, json);
        }
    }

    /** Same path as production: factory, Kafka serializer, Kafka deserializer. */
    private static NotificationEventTO published(String channel, String identifier, Map<String, Object> params) {
        Object message = new CampaignMessageFactory(TOPIC).createMessage(
                channel, UUID.fromString(CAMPAIGN_ID), new RecipientTO(identifier, "User", Map.of("userId", RECIPIENT_ID)),
                params, UUID.fromString(TEMPLATE_ID));
        try (JacksonJsonSerializer<Object> serializer = new JacksonJsonSerializer<>()) {
            return deserialize(serializer.serialize(TOPIC, message));
        }
    }

    @Test
    @DisplayName("the agreed JSON representation deserializes and reaches the WhatsApp channel")
    void process_agreedJson() {
        service().process(deserialize(AGREED_JSON.getBytes(StandardCharsets.UTF_8)));

        ArgumentCaptor<WhatsAppMessageDocument> captor = ArgumentCaptor.forClass(WhatsAppMessageDocument.class);
        verify(router).routeWhatsApp(captor.capture());
        WhatsAppMessageDocument document = captor.getValue();
        assertEquals(MESSAGE_ID, document.getId());
        assertEquals(UUID.fromString(CAMPAIGN_ID), document.getCampaignId());
        assertEquals(UUID.fromString(RECIPIENT_ID), document.getUserId());
        assertEquals(UUID.fromString(TEMPLATE_ID), document.getTemplateDefinedId());
        assertEquals(LocalDateTime.of(2023, 11, 15, 10, 30), document.getSendDate());
        assertEquals("+34600112233", document.getPhoneNumber());
        assertEquals("template", document.getMessageType());
        assertEquals("Hola, gracias por tu compra. Tu pedido está en camino.", document.getContent());
        assertEquals("welcome_confirmation_v1", document.getTemplateName());
        assertEquals("es", document.getTemplateLanguage());
        assertEquals("https://cdn.ejemplo.com/img.jpg", document.getMediaUrl());
        assertEquals("IMAGE", document.getMediaType());
        assertEquals(77, document.getParams().get("order_id"));
        assertEquals(ChannelType.WHATSAPP, document.getChannelType());
        assertEquals(DeliveryStatusType.OPENED, document.getDeliveryStatus());
    }

    @Test
    @DisplayName("a campaign SMS survives the trip through Kafka serialization")
    void process_publishedSms() {
        service().process(published("sms", "+15550001111",
                Map.of(CampaignService.MESSAGE_KEY, "SMS body", CampaignService.SENDER_ID_KEY, "Mercury")));

        ArgumentCaptor<SmsMessageDocument> captor = ArgumentCaptor.forClass(SmsMessageDocument.class);
        verify(router).routeSms(captor.capture());
        assertEquals("+15550001111", captor.getValue().getPhoneNumber());
        assertEquals("SMS body", captor.getValue().getMessage());
        assertEquals("Mercury", captor.getValue().getSenderId());
        assertEquals(UUID.fromString(RECIPIENT_ID), captor.getValue().getUserId());
        assertEquals(ChannelType.SMS, captor.getValue().getChannelType());
    }

    @Test
    @DisplayName("a campaign Telegram message survives the trip through Kafka serialization")
    void process_publishedTelegram() {
        service().process(published("telegram", "-100123456",
                Map.of(CampaignService.MESSAGE_KEY, "TG body", CampaignService.PARSE_MODE_KEY, "HTML")));

        ArgumentCaptor<TelegramMessageDocument> captor = ArgumentCaptor.forClass(TelegramMessageDocument.class);
        verify(router).routeTelegram(captor.capture());
        assertEquals(-100123456L, captor.getValue().getChatId());
        assertEquals("TG body", captor.getValue().getMessage());
        assertEquals("HTML", captor.getValue().getParseMode());
    }

    @Test
    @DisplayName("a campaign WhatsApp message survives the trip through Kafka serialization")
    void process_publishedWhatsApp() {
        service().process(published("whatsapp", "+15550002222", Map.of(CampaignService.MESSAGE_KEY, "WA body")));

        ArgumentCaptor<WhatsAppMessageDocument> captor = ArgumentCaptor.forClass(WhatsAppMessageDocument.class);
        verify(router).routeWhatsApp(captor.capture());
        assertEquals("WA body", captor.getValue().getContent());
        assertEquals("text", captor.getValue().getMessageType());
    }

    @Test
    @DisplayName("a campaign Push message keeps every delivery option")
    void process_publishedPush() {
        service().process(published("push", "device-token", Map.of(
                CampaignService.MESSAGE_KEY, "Push body", "title", "Hi", "platform", "android", "icon", "i.png",
                "click_action", "OPEN", "priority", "high", "badge", 3, "sound", "ding",
                "media_url", "https://cdn.example.com/p.png", "media_type", "IMAGE")));

        ArgumentCaptor<PushNotificationMessageDocument> captor = ArgumentCaptor.forClass(PushNotificationMessageDocument.class);
        verify(router).routePush(captor.capture());
        PushNotificationMessageDocument document = captor.getValue();
        assertEquals("device-token", document.getDeviceToken());
        assertEquals("android", document.getPlatform());
        assertEquals("Hi", document.getTitle());
        assertEquals("Push body", document.getBody());
        assertEquals("i.png", document.getIcon());
        assertEquals("https://cdn.example.com/p.png", document.getImage());
        assertEquals("OPEN", document.getClickAction());
        assertEquals("high", document.getPriority());
        assertEquals(3, document.getBadge());
        assertEquals("ding", document.getSound());
    }

    @Test
    @DisplayName("channel matching ignores case and defaults missing Push options")
    void process_lowerCaseChannelAndNullInstructions() {
        service().process(eventFor("push", "device-token", null));

        ArgumentCaptor<PushNotificationMessageDocument> captor = ArgumentCaptor.forClass(PushNotificationMessageDocument.class);
        verify(router).routePush(captor.capture());
        assertEquals("web", captor.getValue().getPlatform());
        assertEquals("normal", captor.getValue().getPriority());
        assertNull(captor.getValue().getBadge());
    }

    @Test
    @DisplayName("ids that are not UUIDs and a bad timestamp do not lose the message")
    void process_toleratesNonUuidIdsAndBadTimestamp() {
        service().process(new NotificationEventTO(
                new NotificationEventTO.Header("campaign-1", MESSAGE_ID, "yesterday", null),
                new NotificationEventTO.Routing("SMS", "customer-7", "+15550001111"),
                new NotificationEventTO.Content(null, null, null, "body", null, null),
                null));

        ArgumentCaptor<SmsMessageDocument> captor = ArgumentCaptor.forClass(SmsMessageDocument.class);
        verify(router).routeSms(captor.capture());
        assertNull(captor.getValue().getCampaignId());
        assertNull(captor.getValue().getUserId());
        assertNull(captor.getValue().getParams());
        assertNotNull(captor.getValue().getSendDate());
    }

    @ParameterizedTest
    @CsvSource({
            "SMS, 600112233",
            "SMS, ''",
            "WHATSAPP, not-a-phone",
            "TELEGRAM, @channel",
            "TELEGRAM, 99999999999999999999",
            "PUSH, ' '",
            "EMAIL, ana@example.com",
            "fax, +15550001111"
    })
    @DisplayName("undeliverable records are discarded without reaching a channel")
    void process_discardsUndeliverable(String channel, String target) {
        service().process(eventFor(channel, target, Map.of()));

        verifyNoInteractions(router);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("a record without messageId is discarded because it could not be deduplicated")
    void process_discardsBlankMessageId(String messageId) {
        service().process(new NotificationEventTO(
                new NotificationEventTO.Header(CAMPAIGN_ID, messageId, null, null),
                new NotificationEventTO.Routing("SMS", RECIPIENT_ID, "+15550001111"),
                new NotificationEventTO.Content(TEMPLATE_ID, null, null, "body", null, "NONE"),
                null));

        verifyNoInteractions(router);
    }

    @Test
    @DisplayName("a missing channel is discarded")
    void process_discardsNullChannel() {
        service().process(eventFor(null, "+15550001111", Map.of()));

        verifyNoInteractions(router);
    }
}
