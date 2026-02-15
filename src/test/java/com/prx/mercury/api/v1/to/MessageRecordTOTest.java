package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageRecordTOTest {

    @Test
    @DisplayName("Create MessageRecordTO with valid data")
    void createMessageRecordTOWithValidData() {
        UUID id = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        String from = "test@example.com";
        String content = "Test content";
        String subject = "Test subject";
        LocalDateTime now = LocalDateTime.now();
        UUID messageStatusTypeId = UUID.randomUUID();
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "nameTo", "aliasTo"));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", "nameCC", "aliasCC"));

        MessageRecordTO messageRecordTO = new MessageRecordTO(
                id, templateDefinedId, from, content, subject, now, now, messageStatusTypeId, to, cc
        );

        assertNotNull(messageRecordTO);
        assertEquals(id, messageRecordTO.id());
        assertEquals(templateDefinedId, messageRecordTO.templateDefinedId());
        assertEquals(from, messageRecordTO.from());
        assertEquals(content, messageRecordTO.content());
        assertEquals(subject, messageRecordTO.subject());
        assertEquals(now, messageRecordTO.createdAt());
        assertEquals(now, messageRecordTO.updatedAt());
        assertEquals(messageStatusTypeId, messageRecordTO.messageStatusTypeId());
        assertEquals(to, messageRecordTO.to());
        assertEquals(cc, messageRecordTO.cc());
    }

    @Test
    @DisplayName("Create MessageRecordTO with null values")
    void createMessageRecordTOWithNullValues() {
        UUID id = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        String from = null;
        String content = null;
        String subject = null;
        LocalDateTime now = LocalDateTime.now();
        UUID messageStatusTypeId = UUID.randomUUID();
        List<EmailContact> to = null;
        List<EmailContact> cc = null;

        MessageRecordTO messageRecordTO = new MessageRecordTO(
                id, templateDefinedId, from, content, subject, now, now, messageStatusTypeId, to, cc
        );

        assertNotNull(messageRecordTO);
        assertEquals(id, messageRecordTO.id());
        assertEquals(templateDefinedId, messageRecordTO.templateDefinedId());
        assertNull(messageRecordTO.from());
        assertNull(messageRecordTO.content());
        assertNull(messageRecordTO.subject());
        assertEquals(now, messageRecordTO.createdAt());
        assertEquals(now, messageRecordTO.updatedAt());
        assertEquals(messageStatusTypeId, messageRecordTO.messageStatusTypeId());
        assertNull(messageRecordTO.to());
        assertNull(messageRecordTO.cc());
    }

    @Test
    @DisplayName("MessageRecordTO toString method")
    void messageRecordTOToString() {
        UUID id = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        String from = "test@example.com";
        String content = "Test content";
        String subject = "Test subject";
        LocalDateTime now = LocalDateTime.now();
        UUID messageStatusTypeId = UUID.randomUUID();
        List<EmailContact> to = List.of(new EmailContact("to@example.com", null, null));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", null, null));

        MessageRecordTO messageRecordTO = new MessageRecordTO(
                id, templateDefinedId, from, content, subject, now, now, messageStatusTypeId, to, cc
        );

        String expectedString = "MessageRecordTO{" +
                "id=" + id +
                ", templateDefinedId=" + templateDefinedId +
                ", from='" + from + '\'' +
                ", content='" + content + '\'' +
                ", subject='" + subject + '\'' +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", messageStatusTypeId=" + messageStatusTypeId +
                ", to=" + to +
                ", cc=" + cc +
                '}';

        assertEquals(expectedString, messageRecordTO.toString());
    }
}
