package com.prx.mercury.mapper;

import com.umdc.mercury.api.v1.to.TemplateTO;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.mapper.TemplateMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateMapperTest {

    private final TemplateMapper mapper = new TemplateMapper() {
        @Override
        public TemplateEntity toSource(TemplateTO templateTO) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public TemplateTO toTemplateTO(TemplateEntity templateEntity) {
            throw new UnsupportedOperationException("not implemented");
        }
    };

    @Test
    @DisplayName("getApplication should produce ApplicationEntity with same id")
    void getApplicationProducesApplicationEntity() {
        UUID applicationId = UUID.randomUUID();
        var result = mapper.getApplication(applicationId);

        assertNotNull(result);
        assertEquals(applicationId, result.getId());
    }

    @Test
    @DisplayName("getTemplate should produce TemplateEntity with same id")
    void getTemplateProducesTemplateEntity() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = mapper.getTemplate(templateId);

        assertNotNull(template);
        assertEquals(templateId, template.getId());
    }
}
