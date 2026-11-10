package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.exception.MailNotFoundException;
import com.umdc.mercury.api.v1.service.TemplateDefinedService;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.MailTrackingDocument;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.MailTrackingNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.jpa.sql.entity.ChannelTypeEntity;
import com.umdc.mercury.jpa.sql.repository.ChannelTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageIngressServiceTest {

    private final UUID user = UUID.randomUUID();
    private final UUID template = UUID.randomUUID();
    private final ChannelProvider email = mock(ChannelProvider.class);
    private final ChannelProvider sms = mock(ChannelProvider.class);
    private final ChannelTypeRepository catalogue = mock(ChannelTypeRepository.class);
    private final TemplateDefinedService templates = mock(TemplateDefinedService.class);
    private final MailTrackingNSRepository tracking = mock(MailTrackingNSRepository.class);
    private final MessageNSRepository documents = mock(MessageNSRepository.class);
    private MessageIngressService service;

    @BeforeEach
    void setUp() {
        when(email.code()).thenReturn("email");
        when(email.validTarget(anyString())).thenReturn(true);
        when(sms.code()).thenReturn("sms");
        when(sms.validTarget(anyString())).thenReturn(true);
        when(catalogue.findByCode(anyString())).thenReturn(Optional.empty());
        service = new MessageIngressService(new ChannelRegistry(List.of(email, sms)), catalogue, templates, tracking, documents);
    }

    private MessageRequest request(String channel, String target) {
        return new MessageRequest(channel, user, target, template, "es", "no-reply@zerobound.app", "Hola", null, null, Map.of("vc", "123456"), null, null, null);
    }

    private static ChannelTypeEntity row(Boolean enabled, Boolean active) {
        var entity = mock(ChannelTypeEntity.class);
        when(entity.getEnabled()).thenReturn(enabled);
        when(entity.getActive()).thenReturn(active);
        return entity;
    }

    @Test
    void aMessageIsQueuedByItsChannelsProviderAndAnsweredAtOnceAsOpened() {
        var accepted = service.accept(request("EMAIL", " ana@example.com "), "zerobound", "key-1");

        assertFalse(accepted.replayed());
        assertEquals("OPENED", accepted.response().status());
        var queued = ArgumentCaptor.forClass(OutboundMessage.class);
        verify(email).queue(queued.capture());
        assertEquals("ana@example.com", queued.getValue().target());
        assertEquals(accepted.response().id(), queued.getValue().id());
        var claim = ArgumentCaptor.forClass(MailTrackingDocument.class);
        verify(tracking).insert(claim.capture());
        assertEquals("zerobound:key-1", claim.getValue().id());
        assertEquals("zerobound", claim.getValue().caller());
        verify(sms, never()).queue(any());
    }

    @Test
    void withoutAKeyTheClaimIsTheMessageIdItself() {
        var accepted = service.accept(request("sms", "+34600112233"), "zerobound", null);

        var claim = ArgumentCaptor.forClass(MailTrackingDocument.class);
        verify(tracking).insert(claim.capture());
        assertEquals(accepted.response().id().toString(), claim.getValue().id());
    }

    @Test
    void anUnknownChannelIsRefusedAndTheAvailableOnesAreListed() {
        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("slack", "x"), "zerobound", null));

        assertTrue(e.getMessage().contains("slack"));
        assertTrue(e.getMessage().contains("email, sms"));
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void aChannelTheCatalogueSwitchedOffIsRefusedButOneItDoesNotListKeepsWorking() {
        var smsRow = row(false, true);
        when(catalogue.findByCode("sms")).thenReturn(Optional.of(smsRow));

        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("sms", "+34600112233"), "zerobound", null));

        assertTrue(e.getMessage().contains("disabled"));
        assertEquals(List.of("email"), service.channels());
        assertEquals("OPENED", service.accept(request("email", "a@b.co"), "zerobound", null).response().status());
    }

    @Test
    void aCatalogueRowThatIsEnabledAndActiveOrWithoutAnActiveFlagKeepsTheChannelOn() {
        var smsRow = row(true, null);
        when(catalogue.findByCode("sms")).thenReturn(Optional.of(smsRow));
        var emailRow = row(true, true);
        when(catalogue.findByCode("email")).thenReturn(Optional.of(emailRow));

        assertEquals(List.of("email", "sms"), service.channels());
    }

    @Test
    void aCatalogueRowThatIsInactiveSwitchesTheChannelOff() {
        var emailRow = row(true, false);
        when(catalogue.findByCode("email")).thenReturn(Optional.of(emailRow));

        assertEquals(List.of("sms"), service.channels());
    }

    @Test
    void anInvalidTargetIsRefusedBeforeAnythingIsClaimed() {
        when(sms.validTarget("600")).thenReturn(false);

        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("sms", "600"), "zerobound", null));

        assertTrue(e.getMessage().contains("sms"));
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void aFieldTheChannelNeedsIsReportedAsABadRequest() {
        when(email.missingField(any())).thenReturn("subject is required for email");

        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("email", "a@b.co"), "zerobound", null));

        assertEquals("subject is required for email", e.getMessage());
        verify(email, never()).queue(any());
    }

    @Test
    void anUnknownTemplateIsRefusedAndNothingIsQueued() {
        doThrow(new IllegalArgumentException("Template not found")).when(templates).find(template);

        assertThrows(IllegalArgumentException.class, () -> service.accept(request("email", "a@b.co"), "zerobound", null));

        verify(email, never()).queue(any());
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void aMessageWithoutATemplateDoesNotAskForOne() {
        service.accept(new MessageRequest("sms", user, "+34600112233", null, null, null, null, "hola", null, null, null, null, null), "zerobound", null);

        verify(templates, never()).find(any());
    }

    @Test
    void theSameKeyAndRequestAnswersTheFirstMessageAsAReplay() {
        var req = request("email", "a@b.co");
        var first = UUID.randomUUID();
        when(tracking.insert(any(MailTrackingDocument.class))).thenThrow(new DuplicateKeyException("dup"));
        when(tracking.findById("zerobound:k")).thenReturn(Optional.of(new MailTrackingDocument("zerobound:k", first, "zerobound",
                MessageIngressService.hash(req, "email"), DeliveryStatusType.SENT, Instant.now(), Instant.now())));

        var accepted = service.accept(req, "zerobound", "k");

        assertTrue(accepted.replayed());
        assertEquals(first, accepted.response().id());
        assertEquals("SENT", accepted.response().status());
        verify(email, never()).queue(any());
    }

    @Test
    void theSameKeyWithAnotherRequestIsRefused() {
        when(tracking.insert(any(MailTrackingDocument.class))).thenThrow(new DuplicateKeyException("dup"));
        when(tracking.findById("zerobound:k")).thenReturn(Optional.of(new MailTrackingDocument("zerobound:k", UUID.randomUUID(), "zerobound",
                "another-hash", DeliveryStatusType.OPENED, Instant.now(), Instant.now())));

        assertThrows(IllegalStateException.class, () -> service.accept(request("email", "a@b.co"), "zerobound", "k"));
    }

    @Test
    void aKeyStillBeingProcessedAsksTheCallerToRetry() {
        when(tracking.insert(any(MailTrackingDocument.class))).thenThrow(new DuplicateKeyException("dup"));
        when(tracking.findById(anyString())).thenReturn(Optional.empty());

        var e = assertThrows(IllegalStateException.class, () -> service.accept(request("email", "a@b.co"), "zerobound", "k"));

        assertTrue(e.getMessage().contains("retry"));
    }

    @Test
    void whenTheProviderFailsTheClaimIsReleasedSoTheSameKeyCanBeRetried() {
        doThrow(new IllegalStateException("mongo down")).when(email).queue(any());

        assertThrows(IllegalStateException.class, () -> service.accept(request("email", "a@b.co"), "zerobound", "k"));

        verify(tracking).deleteById("zerobound:k");
    }

    @Test
    void theHashIgnoresParameterOrderAndTargetCaseButNotTheChannelOrTheBody() {
        var a = new MessageRequest("email", user, "A@B.co", template, "es", "f", "s", "b", null, Map.of("x", 1, "y", List.of(1, 2)), null, null, null);
        var b = new MessageRequest("email", user, "a@b.co", template, "es", "f", "s", "b", null, Map.of("y", List.of(1, 2), "x", 1), null, null, null);
        var otherBody = new MessageRequest("email", user, "a@b.co", template, "es", "f", "s", "other", null, Map.of("x", 1, "y", List.of(1, 2)), null, null, null);

        assertEquals(MessageIngressService.hash(a, "email"), MessageIngressService.hash(b, "email"));
        assertNotEquals(MessageIngressService.hash(a, "email"), MessageIngressService.hash(otherBody, "email"));
        assertNotEquals(MessageIngressService.hash(a, "email"), MessageIngressService.hash(a, "sms"));
    }

    @Test
    void theStateIsTheChannelsOwnDocumentWhenItHasOneElseTheTrackingRow() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        when(tracking.findByMessageId(id)).thenReturn(Optional.of(new MailTrackingDocument("c", id, "zerobound", "h", DeliveryStatusType.OPENED, now, now)));
        assertEquals(DeliveryStatusType.OPENED, service.status(id, "zerobound").status());

        var doc = new SmsMessageDocument();
        doc.setDeliveryStatus(DeliveryStatusType.SENT);
        when(documents.findById(id.toString())).thenReturn(Optional.of(doc));
        var status = service.status(id, "zerobound");

        assertEquals(DeliveryStatusType.SENT, status.status());
        assertEquals(id, status.id());
    }

    @Test
    void anotherCallersMessageOrAnUnknownIdIsNotFound() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        when(tracking.findByMessageId(id)).thenReturn(Optional.of(new MailTrackingDocument("c", id, "someone-else", "h", DeliveryStatusType.OPENED, now, now)));

        assertThrows(MailNotFoundException.class, () -> service.status(id, "zerobound"));
        assertThrows(MailNotFoundException.class, () -> service.status(UUID.randomUUID(), "zerobound"));
    }

    @Test
    void theRequestNeverPrintsItsTargetSubjectOrParameters() {
        var text = request("email", "ana@example.com").toString();

        assertFalse(text.contains("ana@example.com"));
        assertFalse(text.contains("Hola"));
        assertFalse(text.contains("123456"));
    }
}
