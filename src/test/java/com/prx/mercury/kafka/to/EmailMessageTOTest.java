package com.prx.mercury.kafka.to;

import com.prx.mercury.api.v1.to.EmailContact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmailMessageTOTest {

    @Test
    @DisplayName("Create EmailMessageTO with valid data")
    void createEmailMessageTOWithValidData() {
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = "test@example.com";
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "To Name", "To Alias"));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", "Cc Name", "Cc Alias"));
        String subject = "Test Subject";
        String body = "Test Body";
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = Map.of("key", "value");

        EmailMessageTO emailMessageTO = new EmailMessageTO(
                templateDefinedId, userId, from, to, cc, subject, body, sendDate, params
        );

        assertNotNull(emailMessageTO);
        assertEquals(templateDefinedId, emailMessageTO.templateDefinedId());
        assertEquals(userId, emailMessageTO.userId());
        assertEquals(from, emailMessageTO.from());
        assertEquals(to, emailMessageTO.to());
        assertEquals(cc, emailMessageTO.cc());
        assertEquals(subject, emailMessageTO.subject());
        assertEquals(body, emailMessageTO.body());
        assertEquals(sendDate, emailMessageTO.sendDate());
        assertEquals(params, emailMessageTO.params());
    }

    @Test
    @DisplayName("Create EmailMessageTO with null values")
    void createEmailMessageTOWithNullValues() {
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = null;
        List<EmailContact> to = null;
        List<EmailContact> cc = null;
        String subject = null;
        String body = null;
        LocalDateTime sendDate = null;
        Map<String, Object> params = null;

        EmailMessageTO emailMessageTO = new EmailMessageTO(
                templateDefinedId, userId, from, to, cc, subject, body, sendDate, params
        );

        assertNotNull(emailMessageTO);
        assertEquals(templateDefinedId, emailMessageTO.templateDefinedId());
        assertEquals(userId, emailMessageTO.userId());
        assertNull(emailMessageTO.from());
        assertNull(emailMessageTO.to());
        assertNull(emailMessageTO.cc());
        assertNull(emailMessageTO.subject());
        assertNull(emailMessageTO.body());
        assertNull(emailMessageTO.sendDate());
        assertNull(emailMessageTO.params());
    }

    @Test
    @DisplayName("EmailMessageTO toString method")
    void emailMessageTOToString() {
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String from = "test@example.com";
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "To Name", "To Alias"));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", "Cc Name", "Cc Alias"));
        String subject = "Test Subject";
        String body = "Test Body";
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = Map.of("key", "value");

        EmailMessageTO emailMessageTO = new EmailMessageTO(
                templateDefinedId, userId, from, to, cc, subject, body, sendDate, params
        );

        String expectedString = "EmailMessageTO{" +
                "templateDefinedId=" + templateDefinedId +
                ", userId=" + userId +
                ", from='" + from + '\'' +
                ", to=" + to +
                ", cc=" + cc +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", sendDate=" + sendDate +
                ", params=" + params +
                '}';

        assertEquals(expectedString, emailMessageTO.toString());
    }
}
