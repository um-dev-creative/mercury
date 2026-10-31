package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.RecipientTO;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CampaignMessageFactory unit tests")
class CampaignMessageFactoryTest {

    private static final String UNIFIED_TOPIC = "mercury-unified-message";

    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final UUID TEMPLATE_ID = UUID.randomUUID();

    private final CampaignMessageFactory factory = new CampaignMessageFactory(UNIFIED_TOPIC);

    @Test
    @DisplayName("topicFor returns expected topic for each channel")
    void topicForReturnsExpectedTopic() {
        assertEquals("mercury-email-messages", factory.topicFor("email"));
        assertEquals(UNIFIED_TOPIC, factory.topicFor("sms"));
        assertEquals(UNIFIED_TOPIC, factory.topicFor("telegram"));
        assertEquals(UNIFIED_TOPIC, factory.topicFor("whatsapp"));
        assertEquals(UNIFIED_TOPIC, factory.topicFor("push"));
    }

    @Test
    @DisplayName("topicFor is case-insensitive")
    void topicForIsCaseInsensitive() {
        assertEquals("mercury-email-messages", factory.topicFor("EMAIL"));
        assertEquals(UNIFIED_TOPIC, factory.topicFor("WhAtSaPp"));
    }

    @Test
    @DisplayName("topicFor throws for unknown channel")
    void topicForThrowsForUnknownChannel() {
        assertThrows(IllegalArgumentException.class, () -> factory.topicFor("fax"));
    }

    @Test
    @DisplayName("createMessage builds EmailMessageDocument with mapped fields")
    void createEmailMessageMapsFields() {
        UUID campaignId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RecipientTO recipient = new RecipientTO(
                "john@example.com",
                "John",
                Map.of("userId", userId)
        );

        Map<String, Object> params = Map.of(
                CampaignService.FROM_KEY, "noreply@example.com",
                CampaignService.SUBJECT_KEY, "Welcome",
                CampaignService.BODY_KEY, "Hello body"
        );

        Object message = factory.createMessage("email", campaignId, recipient, params, templateId);

        EmailMessageTO email = assertInstanceOf(EmailMessageTO.class, message);
        assertEquals(templateId, email.templateDefinedId());
        assertEquals(userId, email.userId());
        assertEquals("noreply@example.com", email.from());
        assertEquals("Welcome", email.subject());
        assertEquals("Hello body", email.body());
        assertNotNull(email.sendDate());
        assertEquals("john@example.com", email.to().getFirst().email());
        assertEquals("John", email.to().getFirst().name());
        assertTrue(email.cc().isEmpty());
    }

    private NotificationEventTO event(String channel, String identifier, Map<String, Object> customParams, Map<String, Object> params) {
        Object message = factory.createMessage(
                channel, CAMPAIGN_ID, new RecipientTO(identifier, "User", customParams), params, TEMPLATE_ID);
        return assertInstanceOf(NotificationEventTO.class, message);
    }

    @Test
    @DisplayName("createMessage fills the unified header, routing and content")
    void createMessageFillsUnifiedStructure() {
        UUID userId = UUID.randomUUID();

        NotificationEventTO sms = event("sms", "+15550001111", Map.of("userId", userId),
                Map.of(CampaignService.MESSAGE_KEY, "SMS body", CampaignService.SENDER_ID_KEY, "Mercury"));

        assertEquals(CAMPAIGN_ID.toString(), sms.header().campaignId());
        assertNotNull(UUID.fromString(sms.header().messageId()));
        assertNotNull(Instant.parse(sms.header().timestamp()));
        assertEquals("SMS", sms.routing().channel());
        assertEquals(userId.toString(), sms.routing().recipient_id());
        assertEquals("+15550001111", sms.routing().target_identifier());
        assertEquals(TEMPLATE_ID.toString(), sms.content().templateId());
        assertEquals("SMS body", sms.content().body());
        assertEquals("NONE", sms.content().mediatype());
        assertEquals("SMS body", sms.payload().context().get(CampaignService.MESSAGE_KEY));
        assertEquals("Mercury", sms.payload().delivery_instructions().get("sender_id"));
    }

    @Test
    @DisplayName("createMessage gives every recipient its own messageId")
    void createMessageUsesUniqueMessageIds() {
        Map<String, Object> params = Map.of(CampaignService.MESSAGE_KEY, "body");

        assertNotEquals(
                event("sms", "+15550001111", Map.of(), params).header().messageId(),
                event("sms", "+15550001111", Map.of(), params).header().messageId());
    }

    @Test
    @DisplayName("createMessage puts the Telegram parse mode in the delivery instructions")
    void createTelegramMessage() {
        NotificationEventTO telegram = event("telegram", "123456789", Map.of(),
                Map.of(CampaignService.MESSAGE_KEY, "TG body", CampaignService.PARSE_MODE_KEY, "HTML"));

        assertEquals("TELEGRAM", telegram.routing().channel());
        assertEquals("123456789", telegram.routing().target_identifier());
        assertEquals("HTML", telegram.payload().delivery_instructions().get("parse_mode"));
    }

    @Test
    @DisplayName("createMessage maps WhatsApp template data, media and a default message type")
    void createWhatsAppMessage() {
        NotificationEventTO whatsApp = event("whatsapp", "+15550002222", Map.of("userId", "not-a-uuid"), Map.of(
                CampaignService.MESSAGE_KEY, "WA body",
                "template_name", "welcome",
                "template_language", "es",
                "media_url", "https://cdn.example.com/a.jpg",
                "media_type", "IMAGE"));

        assertEquals("WHATSAPP", whatsApp.routing().channel());
        assertNull(whatsApp.routing().recipient_id());
        assertEquals("welcome", whatsApp.content().templateName());
        assertEquals("es", whatsApp.content().language());
        assertEquals("https://cdn.example.com/a.jpg", whatsApp.content().mediaUrl());
        assertEquals("IMAGE", whatsApp.content().mediatype());
        assertEquals("text", whatsApp.payload().delivery_instructions().get("message_type"));
    }

    @Test
    @DisplayName("createMessage passes the Push options as delivery instructions")
    void createPushMessage() {
        NotificationEventTO push = event("push", "device-token-123", Map.of(), Map.of(
                "title", "Push title", CampaignService.MESSAGE_KEY, "Push body", "badge", 3));

        assertEquals("PUSH", push.routing().channel());
        assertEquals("web", push.payload().delivery_instructions().get("platform"));
        assertEquals("Push title", push.payload().delivery_instructions().get("title"));
        assertEquals(3, push.payload().delivery_instructions().get("badge"));
        assertFalse(push.payload().delivery_instructions().containsKey("icon"));
    }

    @Test
    @DisplayName("createMessage resolves null userId when recipient customParams is invalid")
    void createMessageResolvesNullUserIdOnInvalidCustomParam() {
        UUID templateId = UUID.randomUUID();
        RecipientTO recipient = new RecipientTO(
                "john@example.com",
                "John",
                Map.of("userId", "not-a-uuid")
        );

        Object message = factory.createMessage(
                "email",
                UUID.randomUUID(),
                recipient,
                Map.of(CampaignService.FROM_KEY, "noreply@example.com"),
                templateId
        );

        EmailMessageTO email = assertInstanceOf(EmailMessageTO.class, message);
        assertNull(email.userId());
    }

    @Test
    @DisplayName("createMessage tolerates null params by producing null optional fields")
    void createMessageWithNullParams() {
        NotificationEventTO push = event("push", "device-1", Map.of(), null);

        assertEquals(Map.of(), push.payload().context());
        assertEquals(Map.of("platform", "web"), push.payload().delivery_instructions());
        assertNull(push.content().body());
    }
}
