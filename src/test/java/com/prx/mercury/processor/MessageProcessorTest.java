package com.prx.mercury.processor;

import com.prx.mercury.api.v1.service.*;
import com.prx.mercury.api.v1.to.MessageRecordTO;
import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.api.v1.to.TemplateTO;
import com.prx.mercury.api.v1.to.TemplateTypeTO;
import com.prx.mercury.api.v1.to.VerificationCodeTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.EmailMessageDocument;
import com.prx.mercury.mapper.MessageRecordMapper;
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
        when(messageStatusTypeService.findByName(anyString())).thenReturn(new com.prx.mercury.api.v1.to.MessageStatusTypeTO(UUID.randomUUID(), "SENT", "", LocalDateTime.now(), LocalDateTime.now(), true));
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
}
