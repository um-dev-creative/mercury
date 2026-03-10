package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.RecipientTO;
import com.prx.mercury.constant.ChannelType;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.EmailMessageDocument;
import com.prx.mercury.jpa.nosql.document.SmsMessageDocument;
import com.prx.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.prx.mercury.kafka.to.PushNotificationMessageTO;
import com.prx.mercury.kafka.to.WhatsAppMessageTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CampaignMessageFactory unit tests")
class CampaignMessageFactoryTest {

    private final CampaignMessageFactory factory = new CampaignMessageFactory();

    @Test
    @DisplayName("topicFor returns expected topic for each channel")
    void topicForReturnsExpectedTopic() {
        assertEquals("mercury-email-messages", factory.topicFor("email"));
        assertEquals("mercury-sms-messages", factory.topicFor("sms"));
        assertEquals("mercury-telegram-messages", factory.topicFor("telegram"));
        assertEquals("mercury-whatsapp-messages", factory.topicFor("whatsapp"));
        assertEquals("mercury-push-messages", factory.topicFor("push"));
    }

    @Test
    @DisplayName("topicFor is case-insensitive")
    void topicForIsCaseInsensitive() {
        assertEquals("mercury-email-messages", factory.topicFor("EMAIL"));
        assertEquals("mercury-whatsapp-messages", factory.topicFor("WhAtSaPp"));
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

        EmailMessageDocument email = assertInstanceOf(EmailMessageDocument.class, message);
        assertNull(email.id());
        assertNotNull(email.messageId());
        assertEquals(templateId, email.templateDefinedId());
        assertEquals(userId, email.userId());
        assertEquals("noreply@example.com", email.from());
        assertEquals("Welcome", email.subject());
        assertEquals("Hello body", email.body());
        assertEquals(DeliveryStatusType.OPENED, email.deliveryStatus());
        assertNotNull(email.sendDate());
        assertEquals("john@example.com", email.to().getFirst().email());
        assertEquals("John", email.to().getFirst().name());
        assertTrue(email.cc().isEmpty());
    }

    @Test
    @DisplayName("createMessage builds SMS with expected defaults and mappings")
    void createSmsMessageMapsFields() {
        UUID campaignId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        RecipientTO recipient = new RecipientTO("+15550001111", "Phone User", Map.of());
        Map<String, Object> params = Map.of(
                CampaignService.MESSAGE_KEY, "SMS body",
                CampaignService.SENDER_ID_KEY, "Mercury"
        );

        Object message = factory.createMessage("sms", campaignId, recipient, params, templateId);

        SmsMessageDocument sms = assertInstanceOf(SmsMessageDocument.class, message);
        assertEquals(campaignId, sms.getCampaignId());
        assertEquals(ChannelType.SMS, sms.getChannelType());
        assertEquals(DeliveryStatusType.OPENED, sms.getDeliveryStatus());
        assertEquals(templateId, sms.getTemplateDefinedId());
        assertEquals("+15550001111", sms.getPhoneNumber());
        assertEquals("SMS body", sms.getMessage());
        assertEquals("Mercury", sms.getSenderId());
        assertNotNull(sms.getSendDate());
        assertNull(sms.getUserId());
    }

    @Test
    @DisplayName("createMessage builds Telegram and parses numeric chatId")
    void createTelegramMessageWithNumericChatId() {
        UUID campaignId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        RecipientTO recipient = new RecipientTO("123456789", "TG User", Map.of());
        Map<String, Object> params = Map.of(
                CampaignService.MESSAGE_KEY, "TG body",
                CampaignService.PARSE_MODE_KEY, "HTML"
        );

        Object message = factory.createMessage("telegram", campaignId, recipient, params, templateId);

        TelegramMessageDocument tg = assertInstanceOf(TelegramMessageDocument.class, message);
        assertEquals(campaignId, tg.getCampaignId());
        assertEquals(ChannelType.TELEGRAM, tg.getChannelType());
        assertEquals(DeliveryStatusType.OPENED, tg.getDeliveryStatus());
        assertEquals(templateId, tg.getTemplateDefinedId());
        assertEquals(123456789L, tg.getChatId());
        assertEquals("TG body", tg.getMessage());
        assertEquals("HTML", tg.getParseMode());
        assertNotNull(tg.getSendDate());
    }

    @Test
    @DisplayName("createMessage builds Telegram and sets null chatId for invalid identifier")
    void createTelegramMessageWithInvalidChatId() {
        RecipientTO recipient = new RecipientTO("invalid-chat-id", "TG User", Map.of());

        Object message = factory.createMessage(
                "telegram",
                UUID.randomUUID(),
                recipient,
                Map.of(CampaignService.MESSAGE_KEY, "TG body"),
                UUID.randomUUID()
        );

        TelegramMessageDocument tg = assertInstanceOf(TelegramMessageDocument.class, message);
        assertNull(tg.getChatId());
    }

    @Test
    @DisplayName("createMessage builds WhatsApp with defaults and recipient userId from String")
    void createWhatsAppMessageWithDefaultsAndStringUserId() {
        UUID campaignId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RecipientTO recipient = new RecipientTO(
                "+15550002222",
                "WA User",
                Map.of("userId", userId.toString())
        );

        Object message = factory.createMessage(
                "whatsapp",
                campaignId,
                recipient,
                Map.of(CampaignService.MESSAGE_KEY, "WA body"),
                templateId
        );

        WhatsAppMessageTO wa = assertInstanceOf(WhatsAppMessageTO.class, message);
        assertEquals(templateId, wa.templateDefinedId());
        assertEquals(userId, wa.userId());
        assertEquals("+15550002222", wa.phoneNumber());
        assertEquals("text", wa.messageType());
        assertEquals("WA body", wa.content());
        assertNull(wa.templateName());
        assertNull(wa.templateLanguage());
        assertEquals(campaignId, wa.campaignId());
        assertNotNull(wa.sendDate());
    }

    @Test
    @DisplayName("createMessage builds Push notification and applies defaults")
    void createPushMessageWithDefaults() {
        UUID campaignId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        RecipientTO recipient = new RecipientTO("device-token-123", "Push User", Map.of());
        Map<String, Object> params = Map.of(
                "title", "Push title",
                CampaignService.MESSAGE_KEY, "Push body"
        );

        Object message = factory.createMessage("push", campaignId, recipient, params, templateId);

        PushNotificationMessageTO push = assertInstanceOf(PushNotificationMessageTO.class, message);
        assertEquals(templateId, push.templateDefinedId());
        assertNull(push.userId());
        assertEquals("device-token-123", push.deviceToken());
        assertEquals("web", push.platform());
        assertEquals("Push title", push.title());
        assertEquals("Push body", push.body());
        assertEquals("normal", push.priority());
        assertEquals(campaignId, push.campaignId());
        assertNotNull(push.sendDate());
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

        EmailMessageDocument email = assertInstanceOf(EmailMessageDocument.class, message);
        assertNull(email.userId());
    }

    @Test
    @DisplayName("createMessage tolerates null params by producing null optional fields")
    void createMessageWithNullParams() {
        Object message = factory.createMessage(
                "push",
                UUID.randomUUID(),
                new RecipientTO("device-1", "Device", Map.of()),
                null,
                UUID.randomUUID()
        );

        PushNotificationMessageTO push = assertInstanceOf(PushNotificationMessageTO.class, message);
        assertEquals("web", push.platform());
        assertNull(push.title());
        assertNull(push.body());
    }

    @Test
    @DisplayName("createMessage timestamps are generated close to now")
    void createMessageSetsCurrentTimestamp() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Object message = factory.createMessage(
                "sms",
                UUID.randomUUID(),
                new RecipientTO("+123", "User", Map.of()),
                Map.of(CampaignService.MESSAGE_KEY, "body"),
                UUID.randomUUID()
        );
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        SmsMessageDocument sms = assertInstanceOf(SmsMessageDocument.class, message);
        assertNotNull(sms.getSendDate());
        assertTrue(!sms.getSendDate().isBefore(before) && !sms.getSendDate().isAfter(after));
    }
}

