package com.prx.mercury.jpa.sql.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageRecordEntityTest {

    @Test
    @DisplayName("Create MessageRecordEntity with valid data")
    void createMessageRecordEntityWithValidData() {
        UUID id = UUID.randomUUID();
        String sender = "sender@example.com";
        String to = "to@example.com";
        String cc = "cc@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        LocalDateTime now = LocalDateTime.now();
        MessageStatusTypeEntity messageStatusType = new MessageStatusTypeEntity();
        TemplateDefinedEntity templateDefined = new TemplateDefinedEntity();

        MessageRecordEntity messageRecordEntity = new MessageRecordEntity();
        messageRecordEntity.setId(id);
        messageRecordEntity.setSender(sender);
        messageRecordEntity.setTo(to);
        messageRecordEntity.setCc(cc);
        messageRecordEntity.setSubject(subject);
        messageRecordEntity.setContent(content);
        messageRecordEntity.setCreatedAt(now);
        messageRecordEntity.setUpdatedAt(now);
        messageRecordEntity.setMessageStatusType(messageStatusType);
        messageRecordEntity.setTemplateDefined(templateDefined);

        assertNotNull(messageRecordEntity);
        assertEquals(id, messageRecordEntity.getId());
        assertEquals(sender, messageRecordEntity.getSender());
        assertEquals(to, messageRecordEntity.getTo());
        assertEquals(cc, messageRecordEntity.getCc());
        assertEquals(subject, messageRecordEntity.getSubject());
        assertEquals(content, messageRecordEntity.getContent());
        assertEquals(now, messageRecordEntity.getCreatedAt());
        assertEquals(now, messageRecordEntity.getUpdatedAt());
        assertEquals(messageStatusType, messageRecordEntity.getMessageStatusType());
        assertEquals(templateDefined, messageRecordEntity.getTemplateDefined());
    }

    @Test
    @DisplayName("Create MessageRecordEntity with null values")
    void createMessageRecordEntityWithNullValues() {
        UUID id = UUID.randomUUID();
        String sender = null;
        String to = null;
        String cc = null;
        String subject = null;
        String content = null;
        LocalDateTime now = LocalDateTime.now();
        MessageStatusTypeEntity messageStatusType = null;
        TemplateDefinedEntity templateDefined = null;

        MessageRecordEntity messageRecordEntity = new MessageRecordEntity();
        messageRecordEntity.setId(id);
        messageRecordEntity.setSender(sender);
        messageRecordEntity.setTo(to);
        messageRecordEntity.setCc(cc);
        messageRecordEntity.setSubject(subject);
        messageRecordEntity.setContent(content);
        messageRecordEntity.setCreatedAt(now);
        messageRecordEntity.setUpdatedAt(now);
        messageRecordEntity.setMessageStatusType(messageStatusType);
        messageRecordEntity.setTemplateDefined(templateDefined);

        assertNotNull(messageRecordEntity);
        assertEquals(id, messageRecordEntity.getId());
        assertNull(messageRecordEntity.getSender());
        assertNull(messageRecordEntity.getTo());
        assertNull(messageRecordEntity.getCc());
        assertNull(messageRecordEntity.getSubject());
        assertNull(messageRecordEntity.getContent());
        assertEquals(now, messageRecordEntity.getCreatedAt());
        assertEquals(now, messageRecordEntity.getUpdatedAt());
        assertNull(messageRecordEntity.getMessageStatusType());
        assertNull(messageRecordEntity.getTemplateDefined());
    }
}
