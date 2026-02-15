package com.prx.mercury.mapper;

import com.prx.mercury.api.v1.to.VerificationCodeTO;
import com.prx.mercury.jpa.sql.entity.ApplicationEntity;
import com.prx.mercury.jpa.sql.entity.MessageRecordEntity;
import com.prx.mercury.jpa.sql.entity.VerificationCodeEntity;
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
    @DisplayName("toApplicationEntity should map applicationId to ApplicationEntity.id")
    void toApplicationEntityMapsId() {
        UUID appId = UUID.randomUUID();
        VerificationCodeTO to = new VerificationCodeTO(null, null, appId, null, null, null, null, null, null, null, null, null, null, null);

        ApplicationEntity app = mapper.toApplicationEntity(to);

        assertNotNull(app);
        assertEquals(appId, app.getId());
    }

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
