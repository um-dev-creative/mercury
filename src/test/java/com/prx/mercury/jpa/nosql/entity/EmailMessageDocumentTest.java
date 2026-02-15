package com.prx.mercury.jpa.nosql.entity;

import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.constant.DeliveryStatusType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmailMessageDocumentTest {

    @Test
    @DisplayName("Create EmailMessageDocument with valid data")
    void createEmailMessageDocumentWithValidData() {
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

        assertNotNull(emailMessageDocument);
        assertEquals(id, emailMessageDocument.id());
        assertEquals(messageId, emailMessageDocument.messageId());
        assertEquals(templateDefinedId, emailMessageDocument.templateDefinedId());
        assertEquals(userId, emailMessageDocument.userId());
        assertEquals(from, emailMessageDocument.from());
        assertEquals(to, emailMessageDocument.to());
        assertEquals(cc, emailMessageDocument.cc());
        assertEquals(subject, emailMessageDocument.subject());
        assertEquals(body, emailMessageDocument.body());
        assertEquals(sendDate, emailMessageDocument.sendDate());
        assertEquals(params, emailMessageDocument.params());
        assertEquals(deliveryStatus, emailMessageDocument.deliveryStatus());
    }

    @Test
    @DisplayName("Create EmailMessageDocument with null values")
    void createEmailMessageDocumentWithNullValues() {
        String id = UUID.randomUUID().toString();
        UUID messageId = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = null;
        List<EmailContact> to = null;
        List<EmailContact> cc = null;
        String subject = null;
        String body = null;
        LocalDateTime sendDate = null;
        Map<String, Object> params = null;
        DeliveryStatusType deliveryStatus = null;

        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                id, messageId, templateDefinedId, userId, from, to, cc, subject, body, sendDate, params, deliveryStatus
        );

        assertNotNull(emailMessageDocument);
        assertEquals(id, emailMessageDocument.id());
        assertEquals(messageId, emailMessageDocument.messageId());
        assertEquals(templateDefinedId, emailMessageDocument.templateDefinedId());
        assertEquals(userId, emailMessageDocument.userId());
        assertNull(emailMessageDocument.from());
        assertNull(emailMessageDocument.to());
        assertNull(emailMessageDocument.cc());
        assertNull(emailMessageDocument.subject());
        assertNull(emailMessageDocument.body());
        assertNull(emailMessageDocument.sendDate());
        assertNull(emailMessageDocument.params());
        assertNull(emailMessageDocument.deliveryStatus());
    }

    @Test
    @DisplayName("EmailMessageDocument toString method")
    void emailMessageDocumentToString() {
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

        String expectedString = "MessageValueDTO{" +
                "id=" + id +
                ", templateDefinedId=" + templateDefinedId +
                ", userId=" + userId +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", cc='" + cc + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", sendDate='" + sendDate + '\'' +
                ", params='" + params + '\'' +
                ", deliveryStatus='" + deliveryStatus + '\'' +
                '}';

        assertEquals(expectedString, emailMessageDocument.toString());
    }
}
