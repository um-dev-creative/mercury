package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailServiceTest {

    private final EmailService emailService = new EmailService() {
    };

    @Test
    @DisplayName("sendMail should return not implemented")
    void sendMailShouldReturnNotImplemented() {
        var response = emailService.sendMail(null);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("sendEmail should throw UnsupportedOperationException")
    void sendEmailShouldThrowUnsupportedOperationException() {
        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                "2345sd67f890",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "from",
                null,
                null,
                "subject",
                "body",
                LocalDateTime.now(),
                new HashMap<>(),
                DeliveryStatusType.PENDING
        );
        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                UUID.randomUUID()
        );
        assertThrows(UnsupportedOperationException.class, () -> emailService.sendEmail(emailMessageDocument, templateDefinedTO));
    }

    @Test
    @DisplayName("updateEmailStatus should throw UnsupportedOperationException")
    void updateEmailStatusShouldThrowUnsupportedOperationException() {
        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                "2345sd67f890",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "from",
                null,
                null,
                "subject",
                "body",
                LocalDateTime.now(),
                new HashMap<>(),
                DeliveryStatusType.PENDING
        );
        assertThrows(UnsupportedOperationException.class, () -> emailService.updateEmailStatus(emailMessageDocument));
    }

    @Test
    @DisplayName("findByDeliveryStatus should throw UnsupportedOperationException")
    void findByDeliveryStatusShouldThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> emailService.findByDeliveryStatus(DeliveryStatusType.SENT));
    }

    @Test
    @DisplayName("delete should throw UnsupportedOperationException")
    void deleteShouldThrowUnsupportedOperationException() {
        EmailMessageDocument emailMessageDocument = new EmailMessageDocument(
                "2345sd67f890",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "from",
                null,
                null,
                "subject",
                "body",
                LocalDateTime.now(),
                new HashMap<>(),
                DeliveryStatusType.PENDING
        );
        assertThrows(UnsupportedOperationException.class, () -> emailService.delete(emailMessageDocument));
    }
}
