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
    private final TemplateNameResolver names = mock(TemplateNameResolver.class);
    private final MailTrackingNSRepository tracking = mock(MailTrackingNSRepository.class);
    private final MessageNSRepository documents = mock(MessageNSRepository.class);
    private static final UUID APP = UUID.fromString("81398159-1785-46a5-15ae-f491fd1dc509");
    private static final String CALLER = APP.toString();
    private MessageIngressService service;

    @BeforeEach
    void setUp() {
        when(email.code()).thenReturn("email");
        when(email.validTarget(anyString())).thenReturn(true);
        when(sms.code()).thenReturn("sms");
        when(sms.validTarget(anyString())).thenReturn(true);
        when(catalogue.findByCode(anyString())).thenReturn(Optional.empty());
        var ownDefinition = mock(com.umdc.mercury.api.v1.to.TemplateDefinedTO.class);
        when(ownDefinition.applicationId()).thenReturn(APP);
        org.mockito.Mockito.lenient().when(templates.find(any())).thenReturn(ownDefinition);
        service = new MessageIngressService(new ChannelRegistry(List.of(email, sms)), catalogue, templates, names, tracking, documents);
    }

    private MessageRequest request(String channel, String target) {
        return new MessageRequest(channel, user, target, template, null, null, "es", "no-reply@zerobound.app", "Hola", null, null, Map.of("vc", "123456"), null, null, null);
    }

    private static ChannelTypeEntity row(Boolean enabled, Boolean active) {
        var entity = mock(ChannelTypeEntity.class);
        when(entity.getEnabled()).thenReturn(enabled);
        when(entity.getActive()).thenReturn(active);
        return entity;
    }

    @Test
    void aMessageIsQueuedByItsChannelsProviderAndAnsweredAtOnceAsOpened() {
        var accepted = service.accept(request("EMAIL", " ana@example.com "), CALLER, "key-1");

        assertFalse(accepted.replayed());
        assertEquals("OPENED", accepted.response().status());
        var queued = ArgumentCaptor.forClass(OutboundMessage.class);
        verify(email).queue(queued.capture());
        assertEquals("ana@example.com", queued.getValue().target());
        assertEquals(accepted.response().id(), queued.getValue().id());
        var claim = ArgumentCaptor.forClass(MailTrackingDocument.class);
        verify(tracking).insert(claim.capture());
        assertEquals(CALLER + ":key-1", claim.getValue().id());
        assertEquals(CALLER, claim.getValue().caller());
        verify(sms, never()).queue(any());
    }

    @Test
    void withoutAKeyTheClaimIsTheMessageIdItself() {
        var accepted = service.accept(request("sms", "+34600112233"), CALLER, null);

        var claim = ArgumentCaptor.forClass(MailTrackingDocument.class);
        verify(tracking).insert(claim.capture());
        assertEquals(accepted.response().id().toString(), claim.getValue().id());
    }

    @Test
    void anUnknownChannelIsRefusedAndTheAvailableOnesAreListed() {
        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("slack", "x"), CALLER, null));

        assertTrue(e.getMessage().contains("slack"));
        assertTrue(e.getMessage().contains("email, sms"));
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void aChannelTheCatalogueSwitchedOffIsRefusedButOneItDoesNotListKeepsWorking() {
        var smsRow = row(false, true);
        when(catalogue.findByCode("sms")).thenReturn(Optional.of(smsRow));

        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("sms", "+34600112233"), CALLER, null));

        assertTrue(e.getMessage().contains("disabled"));
        assertEquals(List.of("email"), service.channels());
        assertEquals("OPENED", service.accept(request("email", "a@b.co"), CALLER, null).response().status());
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

        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("sms", "600"), CALLER, null));

        assertTrue(e.getMessage().contains("sms"));
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void aFieldTheChannelNeedsIsReportedAsABadRequest() {
        when(email.missingField(any())).thenReturn("subject is required for email");

        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(request("email", "a@b.co"), CALLER, null));

        assertEquals("subject is required for email", e.getMessage());
        verify(email, never()).queue(any());
    }

    @Test
    void anUnknownTemplateIsRefusedAndNothingIsQueued() {
        doThrow(new IllegalArgumentException("Template not found")).when(templates).find(template);

        assertThrows(IllegalArgumentException.class, () -> service.accept(request("email", "a@b.co"), CALLER, null));

        verify(email, never()).queue(any());
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void aMessageWithoutATemplateDoesNotAskForOne() {
        service.accept(new MessageRequest("sms", user, "+34600112233", null, null, null, null, null, null, "hola", null, null, null, null, null), CALLER, null);

        verify(templates, never()).find(any());
    }

    private MessageRequest named(UUID app, String template, String language, UUID templateDefinedId) {
        return new MessageRequest("email", user, "a@b.co", templateDefinedId, template, app, language, "no-reply@zerobound.app", "Hola", null, null, null, null, null, null);
    }

    @Test
    void aTemplateNamedByNameAndLanguageIsResolvedByMercuryAndQueuedWithItsId() {
        var app = APP;
        var resolved = UUID.randomUUID();
        when(names.resolve(app, "zb_verification", "es")).thenReturn(resolved);

        service.accept(named(app, " zb_verification ", " es ", null), CALLER, null);

        var queued = ArgumentCaptor.forClass(OutboundMessage.class);
        verify(email).queue(queued.capture());
        assertEquals(resolved, queued.getValue().templateId());
        verify(templates, never()).find(any());
    }

    @Test
    void aMissingLanguageVersionIsRefusedWithNoFallbackAndNothingIsQueued() {
        var app = APP;
        when(names.resolve(app, "zb_verification", "en")).thenThrow(new IllegalArgumentException("The template 'zb_verification' has no 'en' version (available: es)"));

        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(named(app, "zb_verification", "en", null), CALLER, null));

        assertTrue(e.getMessage().contains("available: es"));
        verify(email, never()).queue(any());
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void namingATemplateNeedsTheApplicationAndTheLanguage() {
        assertThrows(IllegalArgumentException.class, () -> service.accept(named(null, "zb_verification", "es", null), null, null));
        assertThrows(IllegalArgumentException.class, () -> service.accept(named(APP, "zb_verification", " ", null), CALLER, null));
        assertThrows(IllegalArgumentException.class, () -> service.accept(named(APP, "zb_verification", null, null), CALLER, null));
    }

    @Test
    void anIdAndANameTogetherAreAmbiguousAndRefused() {
        var e = assertThrows(IllegalArgumentException.class, () -> service.accept(named(APP, "zb_verification", "es", UUID.randomUUID()), CALLER, null));

        assertTrue(e.getMessage().contains("not both"));
    }

    @Test
    void theCatalogueOfAnApplicationComesFromTheResolver() {
        var app = UUID.randomUUID();
        var entry = new TemplateNameResolver.Entry("zb_verification", List.of("en", "es"));
        when(names.catalogue(app)).thenReturn(List.of(entry));

        assertEquals(List.of(entry), service.templates(app, app.toString()));
    }

    @Test
    void theSameKeyAndRequestAnswersTheFirstMessageAsAReplay() {
        var req = request("email", "a@b.co");
        var first = UUID.randomUUID();
        // the first call stored the hash of the request as it was queued, with the caller's application filled in
        var stored = new MessageRequest(req.channel(), req.userId(), req.target(), req.templateDefinedId(), req.template(), APP, req.language(), req.from(),
                req.subject(), req.body(), req.sendDate(), req.params(), req.options(), req.mediaUrl(), req.mediaType());
        when(tracking.insert(any(MailTrackingDocument.class))).thenThrow(new DuplicateKeyException("dup"));
        when(tracking.findById(CALLER + ":k")).thenReturn(Optional.of(new MailTrackingDocument(CALLER + ":k", first, CALLER,
                MessageIngressService.hash(stored, "email"), DeliveryStatusType.SENT, Instant.now(), Instant.now())));

        var accepted = service.accept(req, CALLER, "k");

        assertTrue(accepted.replayed());
        assertEquals(first, accepted.response().id());
        assertEquals("SENT", accepted.response().status());
        verify(email, never()).queue(any());
    }

    @Test
    void theSameKeyWithAnotherRequestIsRefused() {
        when(tracking.insert(any(MailTrackingDocument.class))).thenThrow(new DuplicateKeyException("dup"));
        when(tracking.findById(CALLER + ":k")).thenReturn(Optional.of(new MailTrackingDocument(CALLER + ":k", UUID.randomUUID(), CALLER,
                "another-hash", DeliveryStatusType.OPENED, Instant.now(), Instant.now())));

        assertThrows(IllegalStateException.class, () -> service.accept(request("email", "a@b.co"), CALLER, "k"));
    }

    @Test
    void aKeyStillBeingProcessedAsksTheCallerToRetry() {
        when(tracking.insert(any(MailTrackingDocument.class))).thenThrow(new DuplicateKeyException("dup"));
        when(tracking.findById(anyString())).thenReturn(Optional.empty());

        var e = assertThrows(IllegalStateException.class, () -> service.accept(request("email", "a@b.co"), CALLER, "k"));

        assertTrue(e.getMessage().contains("retry"));
    }

    @Test
    void whenTheProviderFailsTheClaimIsReleasedSoTheSameKeyCanBeRetried() {
        doThrow(new IllegalStateException("mongo down")).when(email).queue(any());

        assertThrows(IllegalStateException.class, () -> service.accept(request("email", "a@b.co"), CALLER, "k"));

        verify(tracking).deleteById(CALLER + ":k");
    }

    @Test
    void theHashIgnoresParameterOrderAndTargetCaseButNotTheChannelOrTheBody() {
        var a = new MessageRequest("email", user, "A@B.co", template, null, null, "es", "f", "s", "b", null, Map.of("x", 1, "y", List.of(1, 2)), null, null, null);
        var b = new MessageRequest("email", user, "a@b.co", template, null, null, "es", "f", "s", "b", null, Map.of("y", List.of(1, 2), "x", 1), null, null, null);
        var otherBody = new MessageRequest("email", user, "a@b.co", template, null, null, "es", "f", "s", "other", null, Map.of("x", 1, "y", List.of(1, 2)), null, null, null);

        assertEquals(MessageIngressService.hash(a, "email"), MessageIngressService.hash(b, "email"));
        assertNotEquals(MessageIngressService.hash(a, "email"), MessageIngressService.hash(otherBody, "email"));
        assertNotEquals(MessageIngressService.hash(a, "email"), MessageIngressService.hash(a, "sms"));
    }

    @Test
    void theStateIsTheChannelsOwnDocumentWhenItHasOneElseTheTrackingRow() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        when(tracking.findByMessageId(id)).thenReturn(Optional.of(new MailTrackingDocument("c", id, CALLER, "h", DeliveryStatusType.OPENED, now, now)));
        assertEquals(DeliveryStatusType.OPENED, service.status(id, CALLER).status());

        var doc = new SmsMessageDocument();
        doc.setDeliveryStatus(DeliveryStatusType.SENT);
        when(documents.findById(id.toString())).thenReturn(Optional.of(doc));
        var status = service.status(id, CALLER);

        assertEquals(DeliveryStatusType.SENT, status.status());
        assertEquals(id, status.id());
    }

    @Test
    void anotherCallersMessageOrAnUnknownIdIsNotFound() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        when(tracking.findByMessageId(id)).thenReturn(Optional.of(new MailTrackingDocument("c", id, "someone-else", "h", DeliveryStatusType.OPENED, now, now)));

        assertThrows(MailNotFoundException.class, () -> service.status(id, CALLER));
        assertThrows(MailNotFoundException.class, () -> service.status(UUID.randomUUID(), CALLER));
    }

    @Test
    void theRequestNeverPrintsItsTargetSubjectOrParameters() {
        var text = request("email", "ana@example.com").toString();

        assertFalse(text.contains("ana@example.com"));
        assertFalse(text.contains("Hola"));
        assertFalse(text.contains("123456"));
    }

    @Test
    void aCallerWithNoApplicationInTheRequestGetsItsOwn() {
        var resolved = UUID.randomUUID();
        when(names.resolve(APP, "zb_verification", "es")).thenReturn(resolved);

        service.accept(named(null, "zb_verification", "es", null), CALLER, null);

        var queued = ArgumentCaptor.forClass(OutboundMessage.class);
        verify(email).queue(queued.capture());
        assertEquals(resolved, queued.getValue().templateId());
    }

    @Test
    void aCallerNamingAnotherApplicationIsRefusedAndNothingIsQueued() {
        assertThrows(com.umdc.mercury.api.v1.exception.ForbiddenException.class,
                () -> service.accept(named(UUID.randomUUID(), "zb_verification", "es", null), CALLER, null));

        verify(email, never()).queue(any());
        verify(tracking, never()).insert(any(MailTrackingDocument.class));
    }

    @Test
    void aTemplateDefinitionOfAnotherApplicationIsRefused() {
        var id = UUID.randomUUID();
        var foreign = mock(com.umdc.mercury.api.v1.to.TemplateDefinedTO.class);
        when(foreign.applicationId()).thenReturn(UUID.randomUUID());
        when(templates.find(id)).thenReturn(foreign);

        assertThrows(com.umdc.mercury.api.v1.exception.ForbiddenException.class,
                () -> service.accept(named(null, null, null, id), CALLER, null));
        verify(email, never()).queue(any());
    }

    @Test
    void aTemplateDefinitionOfTheCallersApplicationIsQueued() {
        var id = UUID.randomUUID();
        var own = mock(com.umdc.mercury.api.v1.to.TemplateDefinedTO.class);
        when(own.applicationId()).thenReturn(APP);
        when(templates.find(id)).thenReturn(own);

        service.accept(named(null, null, null, id), CALLER, null);

        verify(email).queue(any());
    }

    @Test
    void aCallerThatIsNotAnApplicationIdIsRefused() {
        assertThrows(com.umdc.mercury.api.v1.exception.ForbiddenException.class,
                () -> service.accept(request("email", "a@b.co"), "zerobound", null));
    }

    @Test
    void theCatalogueOfAnotherApplicationIsRefused() {
        assertThrows(com.umdc.mercury.api.v1.exception.ForbiddenException.class, () -> service.templates(UUID.randomUUID(), CALLER));
    }
}
