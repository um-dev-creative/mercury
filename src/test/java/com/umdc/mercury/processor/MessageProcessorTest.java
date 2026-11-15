package com.umdc.mercury.processor;

import com.umdc.mercury.api.v1.service.*;
import com.umdc.mercury.api.v1.to.*;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.mapper.MessageRecordMapper;
import com.umdc.mercury.processor.MessageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Mock
    private EmailService emailService;

    @Mock
    private MessageRecordService messageRecordService;

    @Mock
    private TemplateDefinedService templateDefinedService;

    @Mock
    private MessageRecordMapper messageRecordMapper;

    @Mock
    private MessageStatusTypeService messageStatusTypeService;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private com.umdc.mercury.api.v1.service.CampaignMetricsService campaignMetricsService;

    @InjectMocks
    private MessageProcessor messageProcessor;

    private UUID verificationCodeTemplateId;

    @BeforeEach
    void setUp() {
        verificationCodeTemplateId = UUID.randomUUID();
        // Set the private field annotated with @Value in MessageProcessor
        ReflectionTestUtils.setField(messageProcessor, "verificationCodeId", verificationCodeTemplateId);
    }

    @Test
    @DisplayName("processMessage should send emails and update status for opened messages")
    void processMessage_sendsEmailsAndUpdatesStatus() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        EmailMessageDocument doc = new EmailMessageDocument(
                "id-1",
                UUID.randomUUID(),
                templateId,
                UUID.randomUUID(),
                "from@example.com",
                null,
                null,
                "subject",
                "body",
                LocalDateTime.now(),
                Map.of(),
                DeliveryStatusType.OPENED
        );

        TemplateTypeTO templateTypeTO = new TemplateTypeTO(UUID.randomUUID(), "name", "desc", LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateTO templateTO = new TemplateTO(UUID.randomUUID(), "desc", "loc", "fmt", templateTypeTO, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(UUID.randomUUID(), templateTO, UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID());

        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(doc));
        when(templateDefinedService.find(templateId)).thenReturn(templateDefinedTO);
        when(emailService.sendEmail(eq(doc), eq(templateDefinedTO))).thenReturn(doc);

        // Act
        messageProcessor.processMessage();

        // Assert
        verify(emailService).findByDeliveryStatus(DeliveryStatusType.OPENED);
        verify(templateDefinedService).find(templateId);
        verify(emailService).sendEmail(doc, templateDefinedTO);
        verify(emailService).updateEmailStatus(doc);
    }

    @Test
    @DisplayName("updateMessageStatus skips a SENT document whose template definition is gone and still processes the others")
    void updateMessageStatus_skipsOrphanDocument() {
        UUID missing = UUID.randomUUID();
        UUID present = UUID.randomUUID();
        EmailMessageDocument orphan = new EmailMessageDocument("o", UUID.randomUUID(), missing, UUID.randomUUID(), "f@e.com", null, null, "s", "b",
                LocalDateTime.now(), Map.of(), DeliveryStatusType.SENT);
        EmailMessageDocument good = new EmailMessageDocument("g", UUID.randomUUID(), present, UUID.randomUUID(), "f@e.com", null, null, "s", "b",
                LocalDateTime.now(), Map.of(), DeliveryStatusType.SENT);
        TemplateTypeTO type = new TemplateTypeTO(UUID.randomUUID(), "n", "d", LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "d", "l", "f", type, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateDefinedTO defined = new TemplateDefinedTO(UUID.randomUUID(), template, UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID());
        when(emailService.findByDeliveryStatus(DeliveryStatusType.SENT)).thenReturn(List.of(orphan, good));
        when(messageStatusTypeService.findByName(anyString())).thenReturn(new MessageStatusTypeTO(UUID.randomUUID(), "SENT", "", LocalDateTime.now(), LocalDateTime.now(), true));
        when(templateDefinedService.find(missing)).thenThrow(new IllegalArgumentException("Template defined not found"));
        when(templateDefinedService.find(present)).thenReturn(defined);
        MessageRecordTO record = new MessageRecordTO(UUID.randomUUID(), present, "f@e.com", "c", "s", LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID(), List.of(), List.of());
        when(messageRecordMapper.toMessageRecordTO(eq(good), any())).thenReturn(record);
        when(messageRecordService.create(any(MessageRecordTO.class))).thenReturn(record);

        messageProcessor.updateMessageStatus();

        verify(emailService).delete(good);
        verify(emailService, org.mockito.Mockito.never()).delete(orphan);
        verify(messageRecordService, org.mockito.Mockito.times(1)).create(any(MessageRecordTO.class));
    }

    @Test
    @DisplayName("updateMessageStatus should create message record, verification code when template matches, and delete the document")
    void updateMessageStatus_createsRecordAndVerificationCodeAndDeletes() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        EmailMessageDocument doc = new EmailMessageDocument(
                "id-2",
                UUID.randomUUID(),
                templateId,
                userId,
                "from@example.com",
                null,
                null,
                "subject",
                "body",
                LocalDateTime.now(),
                Map.of("vc", "123456"),
                DeliveryStatusType.SENT
        );

        // make templateDefinedTO have a template type id equal to verificationCodeTemplateId
        TemplateTypeTO templateTypeTO = new TemplateTypeTO(verificationCodeTemplateId, "verification", "vdesc", LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateTO templateTO = new TemplateTO(UUID.randomUUID(), "desc", "loc", "fmt", templateTypeTO, applicationId, LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(UUID.randomUUID(), templateTO, UUID.randomUUID(), applicationId, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID());

        when(emailService.findByDeliveryStatus(DeliveryStatusType.SENT)).thenReturn(List.of(doc));
        when(messageStatusTypeService.findByName(anyString())).thenReturn(new MessageStatusTypeTO(UUID.randomUUID(), "SENT", "", LocalDateTime.now(), LocalDateTime.now(), true));
        when(templateDefinedService.find(templateId)).thenReturn(templateDefinedTO);

        MessageRecordTO recordTO = new MessageRecordTO(UUID.randomUUID(), templateId, "from@example.com", "content", "subject", LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID(), List.of(), List.of());
        when(messageRecordMapper.toMessageRecordTO(eq(doc), any())).thenReturn(recordTO);
        when(messageRecordService.create(any(MessageRecordTO.class))).thenReturn(recordTO);

        // Act
        messageProcessor.updateMessageStatus();

        // Assert
        verify(emailService).findByDeliveryStatus(DeliveryStatusType.SENT);
        verify(messageStatusTypeService).findByName(DeliveryStatusType.SENT.name());
        verify(templateDefinedService).find(templateId);
        verify(messageRecordService).create(any(MessageRecordTO.class));

        // Capture verification code created
        ArgumentCaptor<VerificationCodeTO> vcCaptor = ArgumentCaptor.forClass(VerificationCodeTO.class);
        verify(verificationCodeService).create(vcCaptor.capture());
        VerificationCodeTO createdVc = vcCaptor.getValue();
        assertEquals("123456", createdVc.verificationCode());
        assertEquals(userId, createdVc.userId());
        assertEquals(applicationId, createdVc.applicationId());
        assertEquals(recordTO.id(), createdVc.messageRecordId());

        verify(emailService).delete(doc);
    }

    @Test
    @DisplayName("getVerificationCodeTO (private) should build a VerificationCodeTO with expected values")
    void getVerificationCodeTO_buildsCorrectVerificationCodeTO() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        EmailMessageDocument doc = new EmailMessageDocument(
                "id-3",
                UUID.randomUUID(),
                templateId,
                userId,
                "from@example.com",
                null,
                null,
                "subject",
                "body",
                LocalDateTime.now(),
                Map.of("vc", "654321"),
                DeliveryStatusType.SENT
        );

        TemplateTypeTO templateTypeTO = new TemplateTypeTO(verificationCodeTemplateId, "verification", "vdesc", LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateTO templateTO = new TemplateTO(UUID.randomUUID(), "desc", "loc", "fmt", templateTypeTO, applicationId, LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(UUID.randomUUID(), templateTO, UUID.randomUUID(), applicationId, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID());

        MessageRecordTO messageRecordTO = new MessageRecordTO(UUID.randomUUID(), templateId, "from@example.com", "content", "subject", LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID(), List.of(), List.of());

        // Use reflection to invoke private method
        VerificationCodeTO result = ReflectionTestUtils.invokeMethod(messageProcessor, "getVerificationCodeTO", doc, templateDefinedTO, messageRecordTO);

        assertNotNull(result);
        assertEquals("654321", result.verificationCode());
        assertEquals(userId, result.userId());
        assertEquals(applicationId, result.applicationId());
        assertEquals(messageRecordTO.id(), result.messageRecordId());
        assertFalse(result.isVerified());
    }

    private TemplateDefinedTO definitionOfType(UUID typeId, String typeName, UUID applicationId) {
        TemplateTypeTO type = new TemplateTypeTO(typeId, typeName, "d", LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "desc", "loc", "fmt", type, applicationId, LocalDateTime.now(), LocalDateTime.now(), true);
        return new TemplateDefinedTO(UUID.randomUUID(), template, UUID.randomUUID(), applicationId, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID());
    }

    private void reconcile(Map<String, Object> params, LocalDateTime sendDate, TemplateDefinedTO definition, UUID templateId, EmailMessageDocument[] out) {
        EmailMessageDocument doc = new EmailMessageDocument("id-x", UUID.randomUUID(), templateId, UUID.randomUUID(), "from@example.com", null, null,
                "subject", "body", sendDate, params, DeliveryStatusType.SENT);
        out[0] = doc;
        when(emailService.findByDeliveryStatus(DeliveryStatusType.SENT)).thenReturn(List.of(doc));
        when(messageStatusTypeService.findByName(anyString())).thenReturn(new MessageStatusTypeTO(UUID.randomUUID(), "SENT", "", LocalDateTime.now(), LocalDateTime.now(), true));
        when(templateDefinedService.find(templateId)).thenReturn(definition);
        MessageRecordTO recordTO = new MessageRecordTO(UUID.randomUUID(), templateId, "from@example.com", "content", "subject", LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID(), List.of(), List.of());
        when(messageRecordMapper.toMessageRecordTO(eq(doc), any())).thenReturn(recordTO);
        when(messageRecordService.create(any(MessageRecordTO.class))).thenReturn(recordTO);
        messageProcessor.updateMessageStatus();
    }

    @Test
    @DisplayName("a template type named VERIFICATION_CODE stores the code even when its id is not the configured one")
    void updateMessageStatus_storesTheCodeWhenTheTypeIsNamedVerificationCode() {
        UUID templateId = UUID.randomUUID();
        var out = new EmailMessageDocument[1];
        reconcile(Map.of("vc", "111222"), LocalDateTime.now(), definitionOfType(UUID.randomUUID(), " verification-code ", UUID.randomUUID()), templateId, out);

        verify(verificationCodeService).create(any(VerificationCodeTO.class));
        verify(emailService).delete(out[0]);
    }

    @Test
    @DisplayName("another template type stores no code, and the message is still recorded and removed")
    void updateMessageStatus_otherTypeStoresNoCode() {
        UUID templateId = UUID.randomUUID();
        var out = new EmailMessageDocument[1];
        reconcile(Map.of("vc", "111222"), LocalDateTime.now(), definitionOfType(UUID.randomUUID(), "NOTIFICATION", UUID.randomUUID()), templateId, out);

        verify(verificationCodeService, never()).create(any());
        verify(messageRecordService).create(any(MessageRecordTO.class));
        verify(emailService).delete(out[0]);
    }

    @Test
    @DisplayName("a verification message without the vc parameter stores no code but does not abort the run")
    void updateMessageStatus_missingCodeParameterStoresNothing() {
        UUID templateId = UUID.randomUUID();
        var out = new EmailMessageDocument[1];
        reconcile(Map.of("other", "x"), LocalDateTime.now(), definitionOfType(verificationCodeTemplateId, "VERIFICATION_CODE", UUID.randomUUID()), templateId, out);

        verify(verificationCodeService, never()).create(any());
        verify(emailService).delete(out[0]);
    }

    @Test
    @DisplayName("a message with no sendDate still stores its code (it lives from now)")
    void updateMessageStatus_noSendDateStillStoresTheCode() {
        UUID templateId = UUID.randomUUID();
        var out = new EmailMessageDocument[1];
        reconcile(Map.of("vc", "333444"), null, definitionOfType(verificationCodeTemplateId, "VERIFICATION_CODE", UUID.randomUUID()), templateId, out);

        ArgumentCaptor<VerificationCodeTO> captor = ArgumentCaptor.forClass(VerificationCodeTO.class);
        verify(verificationCodeService).create(captor.capture());
        assertNotNull(captor.getValue().expiredAt());
        assertEquals("333444", captor.getValue().verificationCode());
    }

    private EmailMessageDocument opened(UUID templateId, DeliveryStatusType status) {
        return new EmailMessageDocument("id-p", UUID.randomUUID(), templateId, UUID.randomUUID(), "from@example.com", null, null, "subject", "body",
                LocalDateTime.now(), Map.of("vc", "555666"), status);
    }

    @Test
    @DisplayName("a verification e-mail that was just sent stores its code at once, without waiting for the periodic reconciliation")
    void processMessage_storesTheVerificationCodeRightAfterSending() {
        UUID templateId = UUID.randomUUID();
        var definition = definitionOfType(verificationCodeTemplateId, "VERIFICATION_CODE", UUID.randomUUID());
        var opened = opened(templateId, DeliveryStatusType.OPENED);
        var sent = opened(templateId, DeliveryStatusType.SENT);
        MessageRecordTO recordTO = new MessageRecordTO(UUID.randomUUID(), templateId, "from@example.com", "content", "subject", LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID(), List.of(), List.of());
        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(opened));
        when(templateDefinedService.find(templateId)).thenReturn(definition);
        when(emailService.sendEmail(opened, definition)).thenReturn(sent);
        when(messageStatusTypeService.findByName(anyString())).thenReturn(new MessageStatusTypeTO(UUID.randomUUID(), "SENT", "", LocalDateTime.now(), LocalDateTime.now(), true));
        when(messageRecordMapper.toMessageRecordTO(eq(sent), any())).thenReturn(recordTO);
        when(messageRecordService.create(any(MessageRecordTO.class))).thenReturn(recordTO);

        messageProcessor.processMessage();

        verify(verificationCodeService).create(any(VerificationCodeTO.class));
        verify(emailService).delete(sent);
    }

    @Test
    @DisplayName("an ordinary e-mail just sent is left for the periodic reconciliation")
    void processMessage_anOrdinaryMailWaitsForTheReconciliation() {
        UUID templateId = UUID.randomUUID();
        var definition = definitionOfType(UUID.randomUUID(), "NOTIFICATION", UUID.randomUUID());
        var opened = opened(templateId, DeliveryStatusType.OPENED);
        var sent = opened(templateId, DeliveryStatusType.SENT);
        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(opened));
        when(templateDefinedService.find(templateId)).thenReturn(definition);
        when(emailService.sendEmail(opened, definition)).thenReturn(sent);

        messageProcessor.processMessage();

        verify(emailService).updateEmailStatus(sent);
        verify(messageRecordService, never()).create(any());
        verify(emailService, never()).delete(any());
    }

    @Test
    @DisplayName("a verification e-mail that failed to send stores no code")
    void processMessage_aFailedVerificationMailStoresNoCode() {
        UUID templateId = UUID.randomUUID();
        var definition = definitionOfType(verificationCodeTemplateId, "VERIFICATION_CODE", UUID.randomUUID());
        var opened = opened(templateId, DeliveryStatusType.OPENED);
        var failed = opened(templateId, DeliveryStatusType.FAILED);
        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(opened));
        when(templateDefinedService.find(templateId)).thenReturn(definition);
        when(emailService.sendEmail(opened, definition)).thenReturn(failed);

        messageProcessor.processMessage();

        verify(verificationCodeService, never()).create(any());
    }

    @Test
    @DisplayName("an e-mail of a campaign advances the campaign counters with the status it reached")
    void processMessage_recordsTheCampaignOutcome() {
        UUID templateId = UUID.randomUUID();
        UUID campaign = UUID.randomUUID();
        var definition = definitionOfType(UUID.randomUUID(), "NOTIFICATION", UUID.randomUUID());
        var opened = new EmailMessageDocument("c", UUID.randomUUID(), templateId, UUID.randomUUID(), "f@e.com", null, null, "s", "b", LocalDateTime.now(), Map.of(), DeliveryStatusType.OPENED, campaign);
        var sent = new EmailMessageDocument("c", opened.messageId(), templateId, opened.userId(), "f@e.com", null, null, "s", "b", LocalDateTime.now(), Map.of(), DeliveryStatusType.SENT, campaign);
        var failed = new EmailMessageDocument("c", opened.messageId(), templateId, opened.userId(), "f@e.com", null, null, "s", "b", LocalDateTime.now(), Map.of(), DeliveryStatusType.FAILED, campaign);
        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(opened));
        when(templateDefinedService.find(templateId)).thenReturn(definition);
        when(emailService.sendEmail(opened, definition)).thenReturn(sent, failed);

        messageProcessor.processMessage();
        messageProcessor.processMessage();

        verify(campaignMetricsService).recordOutcome(campaign, DeliveryStatusType.SENT);
        verify(campaignMetricsService).recordOutcome(campaign, DeliveryStatusType.FAILED);
    }

    @Test
    @DisplayName("an e-mail outside a campaign reaches the metrics service with no campaign (which ignores it)")
    void processMessage_noCampaignIsPassedAsNull() {
        UUID templateId = UUID.randomUUID();
        var definition = definitionOfType(UUID.randomUUID(), "NOTIFICATION", UUID.randomUUID());
        var opened = opened(templateId, DeliveryStatusType.OPENED);
        var sent = opened(templateId, DeliveryStatusType.SENT);
        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(opened));
        when(templateDefinedService.find(templateId)).thenReturn(definition);
        when(emailService.sendEmail(opened, definition)).thenReturn(sent);

        messageProcessor.processMessage();

        verify(campaignMetricsService).recordOutcome(null, DeliveryStatusType.SENT);
    }

    @Test
    @DisplayName("a document with no template definition is skipped and does not stop the e-mails behind it")
    void processMessage_skipsADocumentWithoutTemplateAndContinues() {
        UUID templateId = UUID.randomUUID();
        var definition = definitionOfType(UUID.randomUUID(), "NOTIFICATION", UUID.randomUUID());
        var malformed = new EmailMessageDocument("bad", UUID.randomUUID(), null, UUID.randomUUID(), null, null, null, null, null, null, null, DeliveryStatusType.OPENED);
        var good = opened(templateId, DeliveryStatusType.OPENED);
        var sent = opened(templateId, DeliveryStatusType.SENT);
        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(malformed, good));
        when(templateDefinedService.find(templateId)).thenReturn(definition);
        when(emailService.sendEmail(good, definition)).thenReturn(sent);

        messageProcessor.processMessage();

        verify(emailService).updateEmailStatus(sent);
        verify(templateDefinedService, never()).find(null);
    }

    @Test
    @DisplayName("a SENT document with no template definition is skipped by the reconciliation")
    void updateMessageStatus_skipsADocumentWithoutTemplate() {
        var malformed = new EmailMessageDocument("bad", UUID.randomUUID(), null, UUID.randomUUID(), null, null, null, null, null, null, null, DeliveryStatusType.SENT);
        when(emailService.findByDeliveryStatus(DeliveryStatusType.SENT)).thenReturn(List.of(malformed));

        messageProcessor.updateMessageStatus();

        verify(messageRecordService, never()).create(any());
        verify(emailService, never()).delete(any());
    }

    @Test
    @DisplayName("an SMTP failure leaves that e-mail OPENED for the next run and does not stop the others")
    void processMessage_anSmtpFailureDoesNotStopTheRun() {
        UUID failingTemplate = UUID.randomUUID();
        UUID okTemplate = UUID.randomUUID();
        var definition = definitionOfType(UUID.randomUUID(), "NOTIFICATION", UUID.randomUUID());
        var failing = opened(failingTemplate, DeliveryStatusType.OPENED);
        var ok = opened(okTemplate, DeliveryStatusType.OPENED);
        var sent = opened(okTemplate, DeliveryStatusType.SENT);
        when(emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(failing, ok));
        when(templateDefinedService.find(any(UUID.class))).thenReturn(definition);
        when(emailService.sendEmail(failing, definition)).thenThrow(new org.springframework.mail.MailSendException("Read timed out"));
        when(emailService.sendEmail(ok, definition)).thenReturn(sent);

        messageProcessor.processMessage();

        verify(emailService, never()).updateEmailStatus(failing);
        verify(emailService).updateEmailStatus(sent);
    }

    @Test
    @DisplayName("the SMTP failure log carries the root cause and a hint for Gmail's login refusal")
    void describeSmtpFailure_showsTheRootCauseAndHint() {
        var root = new jakarta.mail.AuthenticationFailedException("535 5.7.8 Username and Password not accepted");
        var failure = new org.springframework.mail.MailAuthenticationException("Authentication failed", root);

        var text = MessageProcessor.describeSmtpFailure(failure);

        assertTrue(text.startsWith("MailAuthenticationException"));
        assertTrue(text.contains("AuthenticationFailedException: 535 5.7.8 Username and Password not accepted"));
        assertTrue(text.contains("app password"));
    }

    @Test
    @DisplayName("each Gmail reply gets its own hint and an unknown one gets none")
    void describeSmtpFailure_hintsPerReply() {
        assertTrue(MessageProcessor.describeSmtpFailure(new org.springframework.mail.MailSendException("550 5.4.5 Daily user sending quota exceeded")).contains("daily sending limit"));
        assertTrue(MessageProcessor.describeSmtpFailure(new org.springframework.mail.MailSendException("421 4.7.0 Try again later")).contains("rate limiting"));
        assertTrue(MessageProcessor.describeSmtpFailure(new org.springframework.mail.MailSendException("534-5.7.9 Application-specific password required")).contains("suspicious"));
        assertTrue(MessageProcessor.describeSmtpFailure(new org.springframework.mail.MailSendException("Read timed out")).contains("unreachable"));
        var unknown = MessageProcessor.describeSmtpFailure(new org.springframework.mail.MailSendException("something odd"));
        assertFalse(unknown.contains("->"));
    }

    @Test
    @DisplayName("a long multi-line SMTP failure is collapsed to one bounded line and a cause loop ends")
    void describeSmtpFailure_boundedAndOneLine() {
        var loopA = new RuntimeException("a\n\n   b");
        var text = MessageProcessor.describeSmtpFailure(new org.springframework.mail.MailSendException("x".repeat(2000), loopA));

        assertFalse(text.contains("\n"));
        assertTrue(text.length() < 700);
        assertTrue(text.endsWith("..."));
        assertTrue(MessageProcessor.describeSmtpFailure(new org.springframework.mail.MailSendException(null, new IllegalStateException())).contains("IllegalStateException"));
    }

    @Test
    @DisplayName("a template without a type is not a verification code")
    void isVerificationCode_noTypeIsFalse() {
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "desc", "loc", "fmt", null, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateDefinedTO definition = new TemplateDefinedTO(UUID.randomUUID(), template, UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID());

        assertFalse(messageProcessor.isVerificationCode(definition));
    }

    @Test
    @DisplayName("expiryOf follows expiresInMinutes, never beyond two days, and falls back to two days")
    void expiryOf_followsTheSendersMinutesWithinTheCeiling() {
        assertEquals(java.time.Duration.ofMinutes(15), MessageProcessor.expiryOf(Map.of("expiresInMinutes", "15")));
        assertEquals(java.time.Duration.ofMinutes(10), MessageProcessor.expiryOf(Map.of("expiresInMinutes", 10)));
        assertEquals(java.time.Duration.ofMinutes(20), MessageProcessor.expiryOf(Map.of("expiresInMinutes", " 20 ")));
        assertEquals(MessageProcessor.MAX_CODE_LIFETIME, MessageProcessor.expiryOf(Map.of("expiresInMinutes", "999999")));
        assertEquals(MessageProcessor.MAX_CODE_LIFETIME, MessageProcessor.expiryOf(Map.of("expiresInMinutes", "0")));
        assertEquals(MessageProcessor.MAX_CODE_LIFETIME, MessageProcessor.expiryOf(Map.of("expiresInMinutes", "-5")));
        assertEquals(MessageProcessor.MAX_CODE_LIFETIME, MessageProcessor.expiryOf(Map.of("expiresInMinutes", "soon")));
        assertEquals(MessageProcessor.MAX_CODE_LIFETIME, MessageProcessor.expiryOf(Map.of("vc", "123456")));
        assertEquals(MessageProcessor.MAX_CODE_LIFETIME, MessageProcessor.expiryOf(null));
        assertEquals(java.time.Duration.ofDays(2), MessageProcessor.MAX_CODE_LIFETIME);
    }

    @Test
    @DisplayName("the stored code expires when the e-mail says it does")
    void getVerificationCodeTO_expiresAtTheMinutesTheEmailPromises() {
        UUID applicationId = UUID.randomUUID();
        LocalDateTime sent = LocalDateTime.of(2026, 10, 18, 12, 0);
        EmailMessageDocument doc = new EmailMessageDocument("id-4", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "from@example.com",
                null, null, "subject", "body", sent, Map.of("vc", "111222", "expiresInMinutes", "15"), DeliveryStatusType.SENT);
        TemplateTypeTO templateTypeTO = new TemplateTypeTO(verificationCodeTemplateId, "verification", "vdesc", LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateTO templateTO = new TemplateTO(UUID.randomUUID(), "desc", "loc", "fmt", templateTypeTO, applicationId, LocalDateTime.now(), LocalDateTime.now(), true);
        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(UUID.randomUUID(), templateTO, UUID.randomUUID(), applicationId, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID());
        MessageRecordTO messageRecordTO = new MessageRecordTO(UUID.randomUUID(), UUID.randomUUID(), "from@example.com", "content", "subject", LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID(), List.of(), List.of());

        VerificationCodeTO result = ReflectionTestUtils.invokeMethod(messageProcessor, "getVerificationCodeTO", doc, templateDefinedTO, messageRecordTO);

        assertNotNull(result);
        assertEquals(sent.plusMinutes(15), result.expiredAt());
        assertEquals(3, result.maxAttempts());
    }
}
