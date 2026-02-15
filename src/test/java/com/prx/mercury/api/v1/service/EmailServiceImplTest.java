package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.api.v1.to.TemplateTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
import com.prx.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class EmailServiceImplTest {

    @Mock
    JavaMailSender javaMailSender;

    @Mock
    Configuration freemarkerConfig;

    @Mock
    EmailMessageNSRepository emailMessageNSRepository;

    @InjectMocks
    EmailServiceImpl emailServiceImpl;

    @Test
    @DisplayName("Find by delivery status with valid status")
    void findByDeliveryStatusWithValidStatus() {
        String id = UUID.randomUUID().toString();
        UUID messageId = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = "test@example.com";
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "To Name", "To Type"));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", "Cc Name", "Cc Type"));
        String subject = "Test Subject";
        String body = "Test Body";
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = Map.of("key", "value");
        DeliveryStatusType deliveryStatus = DeliveryStatusType.SENT;

        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                id, messageId, templateDefinedId, userId, from, to, cc, subject, body, sendDate, params, deliveryStatus
        );
        List<EmailMessageDocument> expectedDocuments = List.of(emailMessageDocument);

        when(emailMessageNSRepository.findByDeliveryStatus(any(DeliveryStatusType.class))).thenReturn(expectedDocuments);

        List<EmailMessageDocument> result = emailServiceImpl.findByDeliveryStatus(deliveryStatus);

        assertNotNull(result);
        assertEquals(expectedDocuments, result);
        verify(emailMessageNSRepository).findByDeliveryStatus(deliveryStatus);
    }

    @Test
    @DisplayName("Find by delivery status with null status")
    void findByDeliveryStatusWithNullStatus() {
        assertThrows(IllegalArgumentException.class, () -> emailServiceImpl.findByDeliveryStatus(null));
    }

    @Test
    @DisplayName("Update email status with valid document")
    void updateEmailStatusWithValidDocument() {
        String id = UUID.randomUUID().toString();
        UUID messageId = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = "test@example.com";
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "To Name", "To Type"));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", "Cc Name", "Cc Type"));
        String subject = "Test Subject";
        String body = "Test Body";
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = Map.of("key", "value");
        DeliveryStatusType deliveryStatus = DeliveryStatusType.SENT;

        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                id, messageId, templateDefinedId, userId, from, to, cc, subject, body, sendDate, params, deliveryStatus
        );

        emailServiceImpl.updateEmailStatus(emailMessageDocument);

        verify(emailMessageNSRepository).save(emailMessageDocument);
    }

    @Test
    @DisplayName("Update email status with null document")
    void updateEmailStatusWithNullDocument() {
        assertThrows(IllegalArgumentException.class, () -> emailServiceImpl.updateEmailStatus(null));
    }

    @Test
    @DisplayName("Delete email with valid document")
    void deleteEmailWithValidDocument() {
        String id = UUID.randomUUID().toString();
        UUID messageId = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = "test@example.com";
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "To Name", "To Type"));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", "Cc Name", "Cc Type"));
        String subject = "Test Subject";
        String body = "Test Body";
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = Map.of("key", "value");
        DeliveryStatusType deliveryStatus = DeliveryStatusType.SENT;

        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                id, messageId, templateDefinedId, userId, from, to, cc, subject, body, sendDate, params, deliveryStatus
        );

        emailServiceImpl.delete(emailMessageDocument);

        verify(emailMessageNSRepository).delete(emailMessageDocument);
    }

    @Test
    @DisplayName("sendEmail throws when templateDefined is null")
    void sendEmailTemplateNullThrows() {
        String id = UUID.randomUUID().toString();
        UUID messageId = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = "test@example.com";
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "To Name", "To Type"));
        List<EmailContact> cc = Collections.emptyList();
        String subject = "Test Subject";
        String body = "Test Body";
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = Map.of("key", "value");
        DeliveryStatusType deliveryStatus = DeliveryStatusType.PENDING;

        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                id, messageId, templateDefinedId, userId, from, to, cc, subject, body, sendDate, params, deliveryStatus
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> emailServiceImpl.sendEmail(emailMessageDocument, null));
        assertEquals("Template defined not found", ex.getMessage());
    }

    @Test
    @DisplayName("sendEmail returns SENT when mailSender sends successfully")
    void sendEmailSuccess() throws Exception {
        // prepare freemarker template
        Template tpl = new Template("tpl", new StringReader("Hello ${name}"), new Configuration(Configuration.VERSION_2_3_31));
        when(freemarkerConfig.getTemplate("template.ftl")).thenReturn(tpl);

        // mail sender does nothing (success)
        doNothing().when(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        List<EmailContact> to = List.of(new EmailContact("to@example.com", "To", null));
        List<EmailContact> cc = Collections.emptyList();
        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                "1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "from@x.com", to, cc, "sub", "", LocalDateTime.now(), Map.of("name", "Bob"), DeliveryStatusType.PENDING
        );
        TemplateTO templateTO = new TemplateTO(null, null, "template.ftl", null, null, null, null, null, true);
        TemplateDefinedTO tdef = new TemplateDefinedTO(null, templateTO, null, null, null, null, null, true, null);

        var result = emailServiceImpl.sendEmail(emailMessageDocument, tdef);

        assertNotNull(result);
        assertEquals(DeliveryStatusType.SENT, result.deliveryStatus());
        verify(javaMailSender, times(1)).send(any(jakarta.mail.internet.MimeMessage.class));
    }

}
