package com.umdc.mercury.mapper;

import com.umdc.mercury.api.v1.to.VerificationCodeTO;
import com.umdc.mercury.jpa.sql.entity.MessageRecordEntity;
import com.umdc.mercury.jpa.sql.entity.VerificationCodeEntity;
import com.umdc.mercury.mapper.VerificationCodeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationCodeMapperTest {

    private final VerificationCodeMapper mapper = new VerificationCodeMapper() {
        @Override
        public VerificationCodeEntity toVerificationCodeEntity(VerificationCodeTO verificationCodeTO) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public VerificationCodeTO toVerificationCodeTO(VerificationCodeEntity verificationCodeEntity) {
            throw new UnsupportedOperationException("not implemented");
        }
    };

    @Test
    @DisplayName("toUserEntity should return the userId from TO")
    void toUserEntityReturnsUuid() {
        UUID userId = UUID.randomUUID();
        VerificationCodeTO to = new VerificationCodeTO(null, userId, null, null, null, null, null, null, null, null, null, null, null, null);

        UUID result = mapper.toUserEntity(to);

        assertEquals(userId, result);
    }

    @Test
    @DisplayName("toMessageRecordEntity should create MessageRecordEntity with the expected id")
    void toMessageRecordEntitySetsId() {
        UUID msgId = UUID.randomUUID();
        VerificationCodeTO to = new VerificationCodeTO(null, null, null, null, null, null, null, null, null, null, null, null, null, msgId);

        MessageRecordEntity mre = mapper.toMessageRecordEntity(to);

        assertNotNull(mre);
        assertEquals(msgId, mre.getId());
    }
}
