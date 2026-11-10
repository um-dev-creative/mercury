package com.umdc.mercury.ingress;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelProvidersTest {

    private final UUID id = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final UUID template = UUID.randomUUID();
    private final NotificationEventConsumerService consumer = mock(NotificationEventConsumerService.class);
    private final MessageNSRepository documents = mock(MessageNSRepository.class);
    private final EmailMessageNSRepository emails = mock(EmailMessageNSRepository.class);

    private OutboundMessage message(String channel, String target) {
        return new OutboundMessage(id, channel, user, target, template, "es", "no-reply@zerobound.app", "Hola", "cuerpo", null, Map.of("vc", "123456"), Map.of("sender_id", "ZB"), null, null);
    }

    @Test
    void emailAcceptsAddressesAndNothingElse() {
        var email = new EmailChannelProvider(emails);

        assertEquals("email", email.code());
        assertTrue(email.validTarget(" ana@example.com "));
        assertFalse(email.validTarget("ana"));
        assertFalse(email.validTarget("+34600112233"));
        assertFalse(email.validTarget(null));
    }

    @Test
    void emailNeedsATemplateASenderAndASubject() {
        var email = new EmailChannelProvider(emails);

        assertNull(email.missingField(message("email", "ana@example.com")));
        assertEquals("templateDefinedId is required for email", email.missingField(new OutboundMessage(id, "email", user, "a@b.co", null, "es", "x@y.co", "s", "b", null, null, null, null, null)));
        assertEquals("from is required for email", email.missingField(new OutboundMessage(id, "email", user, "a@b.co", template, "es", " ", "s", "b", null, null, null, null, null)));
        assertEquals("subject is required for email", email.missingField(new OutboundMessage(id, "email", user, "a@b.co", template, "es", "x@y.co", null, "b", null, null, null, null, null)));
    }

    @Test
    void emailQueuesTheSameDocumentThePostMailEndpointDoes() {
        new EmailChannelProvider(emails).queue(message("email", " ana@example.com "));

        var saved = ArgumentCaptor.forClass(EmailMessageDocument.class);
        verify(emails).save(saved.capture());
        var doc = saved.getValue();
        assertEquals(id, doc.messageId());
        assertEquals(template, doc.templateDefinedId());
        assertEquals(user, doc.userId());
        assertEquals("ana@example.com", doc.to().getFirst().email());
        assertTrue(doc.cc().isEmpty());
        assertEquals("Hola", doc.subject());
        assertEquals(DeliveryStatusType.OPENED, doc.deliveryStatus());
        assertEquals("123456", doc.params().get("vc"));
    }

    @Test
    void emailWithoutABodyQueuesAPlaceholderBecauseTheTemplateRendersTheRealOne() {
        new EmailChannelProvider(emails).queue(new OutboundMessage(id, "email", user, "a@b.co", template, "es", "x@y.co", "s", null, null, null, null, null, null));

        var saved = ArgumentCaptor.forClass(EmailMessageDocument.class);
        verify(emails).save(saved.capture());
        assertEquals("email", saved.getValue().body());
    }

    @Test
    void phoneChannelsTakeE164NumbersTelegramANumericChatIdPushAnyToken() {
        var sms = new SmsChannelProvider(consumer, documents);
        var whatsapp = new WhatsAppChannelProvider(consumer, documents);
        var telegram = new TelegramChannelProvider(consumer, documents);
        var push = new PushChannelProvider(consumer, documents);

        assertEquals("sms", sms.code());
        assertEquals("whatsapp", whatsapp.code());
        assertEquals("telegram", telegram.code());
        assertEquals("push", push.code());
        for (var phone : new ChannelProvider[]{sms, whatsapp}) {
            assertTrue(phone.validTarget("+34600112233"));
            assertFalse(phone.validTarget("600112233"));
            assertFalse(phone.validTarget(null));
        }
        assertTrue(telegram.validTarget("-100123456789"));
        assertFalse(telegram.validTarget("@ana"));
        assertFalse(telegram.validTarget(null));
        assertTrue(push.validTarget("fcm-device-token"));
        assertFalse(push.validTarget(" "));
        assertFalse(push.validTarget(null));
    }

    @Test
    void aDocumentChannelBuildsTheUnifiedTopicEventAndUsesTheSameConsumer() {
        when(documents.existsById(id.toString())).thenReturn(true);
        var sms = new SmsChannelProvider(consumer, documents);
        var when = LocalDateTime.of(2026, 11, 10, 7, 0, 0);

        sms.queue(new OutboundMessage(id, "sms", user, " +34600112233 ", template, "es", null, null, "hola", when, Map.of("a", 1), Map.of("sender_id", "ZB"), "http://x/i.png", "IMAGE"));

        var event = ArgumentCaptor.forClass(NotificationEventTO.class);
        verify(consumer).process(event.capture());
        var e = event.getValue();
        assertEquals(id.toString(), e.header().messageId());
        assertEquals("2026-11-10T07:00:00Z", e.header().timestamp());
        assertEquals("sms", e.routing().channel());
        assertEquals(user.toString(), e.routing().recipient_id());
        assertEquals("+34600112233", e.routing().target_identifier());
        assertEquals(template.toString(), e.content().templateId());
        assertEquals("hola", e.content().body());
        assertEquals("IMAGE", e.content().mediatype());
        assertEquals(Map.of("a", 1), e.payload().context());
        assertEquals("ZB", e.payload().delivery_instructions().get("sender_id"));
    }

    @Test
    void aDocumentChannelWithoutOptionsOrMediaGetsEmptyMapsAndNoMedia() {
        when(documents.existsById(id.toString())).thenReturn(true);
        new PushChannelProvider(consumer, documents).queue(new OutboundMessage(id, "push", null, "token", null, null, null, null, "hi", null, null, null, null, null));

        var event = ArgumentCaptor.forClass(NotificationEventTO.class);
        verify(consumer).process(event.capture());
        assertEquals("NONE", event.getValue().content().mediatype());
        assertTrue(event.getValue().payload().context().isEmpty());
        assertTrue(event.getValue().payload().delivery_instructions().isEmpty());
        assertNull(event.getValue().routing().recipient_id());
        assertTrue(isInstant(event.getValue().header().timestamp()));
    }

    private static boolean isInstant(String text) {
        return java.time.Instant.parse(text) != null;
    }

    @Test
    void whenTheConsumerDiscardedTheMessageTheHttpCallerIsToldInsteadOfGettingASilentOpened() {
        when(documents.existsById(any())).thenReturn(false);

        var e = assertThrows(IllegalArgumentException.class, () -> new TelegramChannelProvider(consumer, documents).queue(message("telegram", "123")));

        assertTrue(e.getMessage().contains("telegram"));
    }

    @Test
    void theMessageNeverPrintsItsTargetBodyOrParameters() {
        var text = message("sms", "+34600112233").toString();

        assertFalse(text.contains("+34600112233"));
        assertFalse(text.contains("cuerpo"));
        assertFalse(text.contains("123456"));
    }
}
