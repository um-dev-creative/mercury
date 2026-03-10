package com.prx.mercury.mapper;

import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.MessageRecordTO;
import com.prx.mercury.jpa.sql.entity.MessageRecordEntity;
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
        public MessageRecordEntity toMessageRecordEntity(com.prx.mercury.jpa.nosql.document.EmailMessageDocument emailMessageDocument, com.prx.mercury.api.v1.to.MessageStatusTypeTO messageStatusTypeTO, com.prx.mercury.api.v1.to.TemplateDefinedTO templateDefinedTO) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public MessageRecordTO toMessageRecordTO(com.prx.mercury.jpa.nosql.document.EmailMessageDocument emailMessageDocument, com.prx.mercury.api.v1.to.MessageStatusTypeTO messageStatusTypeTO) {
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
        var tdto = new com.prx.mercury.api.v1.to.TemplateDefinedTO(id, null, null, null, null, null, null, null, null);
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

        var msdto = new com.prx.mercury.api.v1.to.MessageStatusTypeTO(id, "code", "desc", LocalDateTime.now(), LocalDateTime.now(), true);
        var entityFromMsdto = mapper.toMessageStatusTypeEntity(msdto);
        assertEquals(id, entityFromMsdto.getId());
    }
}
