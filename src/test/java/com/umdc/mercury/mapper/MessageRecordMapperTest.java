package com.umdc.mercury.mapper;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.api.v1.to.MessageRecordTO;
import com.umdc.mercury.api.v1.to.MessageStatusTypeTO;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.jpa.sql.entity.MessageRecordEntity;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.mapper.MessageRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageRecordMapperTest {

    private final MessageRecordMapper mapper = new MessageRecordMapper() {
        @Override
        public MessageRecordEntity toMessageRecordEntity(MessageRecordTO messageRecordTO) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public MessageRecordTO toMessageRecordTO(MessageRecordEntity result) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public MessageRecordEntity toMessageRecordEntity(EmailMessageDocument emailMessageDocument, MessageStatusTypeTO messageStatusTypeTO, TemplateDefinedTO templateDefinedTO) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public MessageRecordTO toMessageRecordTO(EmailMessageDocument emailMessageDocument, MessageStatusTypeTO messageStatusTypeTO) {
            throw new UnsupportedOperationException("not implemented");
        }
    };

    @Test
    @DisplayName("listToString should convert list of EmailContact to string representation")
    void listToStringConvertsList() {
        var contacts = List.of(new EmailContact("a@b.com", "A", null), new EmailContact("c@d.com", "C", "alias"));
        var result = mapper.listToString(contacts);

        assertNotNull(result);
        assertTrue(result.contains("a@b.com"));
        assertTrue(result.contains("c@d.com"));
    }

    @Test
    @DisplayName("listToString should return empty string for null input")
    void listToStringNullReturnsEmpty() {
        String result = mapper.listToString(null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("toTemplateDefinedEntity should set id from TemplateDefinedTO")
    void toTemplateDefinedEntityFromTO() {
        UUID id = UUID.randomUUID();
        var tdto = new TemplateDefinedTO(id, null, null, null, null, null, null, null, null);
        var entity = mapper.toTemplateDefinedEntity(tdto);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
    }

    @Test
    @DisplayName("toTemplateDefinedEntity from MessageRecordTO should set templateDefinedId")
    void toTemplateDefinedEntityFromMessageRecordTO() {
        UUID id = UUID.randomUUID();
        var mto = new MessageRecordTO(null, id, "from@x.com", "content", "subject", null, null, null, null, null);
        var entity = mapper.toTemplateDefinedEntity(mto);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
    }

    @Test
    @DisplayName("toMessageStatusTypeEntity should map ids correctly from TO and DTO")
    void toMessageStatusTypeEntityMapping() {
        UUID id = UUID.randomUUID();
        var mto = new MessageRecordTO(null, null, "from@x.com", "content", "subject", null, null, id, null, null);
        var entityFromMto = mapper.toMessageStatusTypeEntity(mto);
        assertEquals(id, entityFromMto.getId());

        var msdto = new MessageStatusTypeTO(id, "code", "desc", LocalDateTime.now(), LocalDateTime.now(), true);
        var entityFromMsdto = mapper.toMessageStatusTypeEntity(msdto);
        assertEquals(id, entityFromMsdto.getId());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("a message sent without a send date still gets a creation time, so its record can be stored (it used to fail validation and stay SENT forever)")
    void recordOfAMessageWithoutSendDate() {
        var mapper = new MessageRecordMapperImpl();
        var statusType = new MessageStatusTypeTO(java.util.UUID.randomUUID(), "SENT", "sent", null, null, true);
        var withoutDate = new EmailMessageDocument("doc", java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "from@x.com",
                java.util.List.of(), java.util.List.of(), "subject", "body", null, java.util.Map.of(), com.umdc.mercury.constant.DeliveryStatusType.SENT);
        var before = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1);

        var record = mapper.toMessageRecordTO(withoutDate, statusType);

        org.junit.jupiter.api.Assertions.assertTrue(record.createdAt().isAfter(before));
        org.junit.jupiter.api.Assertions.assertTrue(record.updatedAt().isAfter(before));

        var sent = java.time.LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        var withDate = new EmailMessageDocument("doc", java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "from@x.com",
                java.util.List.of(), java.util.List.of(), "subject", "body", sent, java.util.Map.of(), com.umdc.mercury.constant.DeliveryStatusType.SENT);
        org.junit.jupiter.api.Assertions.assertEquals(sent, mapper.toMessageRecordTO(withDate, statusType).createdAt());
    }
}
