package com.prx.mercury.mapper;

import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateTO;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.entity.SeverityTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateTypeEntity;
import com.umdc.mercury.mapper.TemplateMapper;
import com.umdc.mercury.mapper.TemplateTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

        @Override
        public TemplateEntity toEntity(CreateTemplateRequest request, TemplateTypeEntity templateType,
                                        ApplicationEntity application, SeverityTypeEntity severityType, LocalDateTime now) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public TemplateDetailResponse toDetailResponse(TemplateEntity templateEntity) {
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

    @Nested
    @DisplayName("generated mapper – Template Management API methods")
    class GeneratedMapperTests {

        private final TemplateMapper generated = Mappers.getMapper(TemplateMapper.class);

        private TemplateTypeEntity templateType;
        private ApplicationEntity application;
        private SeverityTypeEntity severityType;

        @BeforeEach
        void setUp() throws ReflectiveOperationException {
            templateType = new TemplateTypeEntity();
            templateType.setId(UUID.randomUUID());
            templateType.setName("Marketing");

            application = new ApplicationEntity();
            application.setId(UUID.randomUUID());

            severityType = new SeverityTypeEntity();
            severityType.setId(UUID.randomUUID());
            severityType.setName("High");

            // Mappers.getMapper() bypasses Spring, so the @Autowired nested TemplateTypeMapper
            // field is never injected; wire a real instance in by hand for this test only.
            var field = generated.getClass().getDeclaredField("templateTypeMapper");
            field.setAccessible(true);
            field.set(generated, Mappers.getMapper(TemplateTypeMapper.class));
        }

        @Test
        @DisplayName("toEntity maps every field and always forces active=true")
        void toEntity_mapsEveryField() {
            CreateTemplateRequest request = new CreateTemplateRequest(
                    "desc", "loc", "HTML", templateType.getId(), application.getId(), severityType.getId());
            LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

            TemplateEntity entity = generated.toEntity(request, templateType, application, severityType, now);

            assertAll("toEntity",
                    () -> assertNull(entity.getId()),
                    () -> assertEquals("desc", entity.getDescription()),
                    () -> assertEquals("loc", entity.getLocation()),
                    () -> assertEquals("HTML", entity.getFileFormat()),
                    () -> assertSame(templateType, entity.getTemplateType()),
                    () -> assertSame(application, entity.getApplication()),
                    () -> assertSame(severityType, entity.getSeverityType()),
                    () -> assertEquals(Boolean.TRUE, entity.getActive()),
                    () -> assertEquals(now, entity.getCreatedAt()),
                    () -> assertEquals(now, entity.getUpdatedAt())
            );
        }

        @Test
        @DisplayName("toDetailResponse maps every field, including severity and real active state")
        void toDetailResponse_mapsEveryField() {
            LocalDateTime now = LocalDateTime.of(2026, 2, 2, 12, 0);
            TemplateEntity entity = new TemplateEntity();
            UUID id = UUID.randomUUID();
            entity.setId(id);
            entity.setDescription("desc");
            entity.setLocation("loc");
            entity.setFileFormat("HTML");
            entity.setTemplateType(templateType);
            entity.setApplication(application);
            entity.setSeverityType(severityType);
            entity.setActive(false);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);

            TemplateDetailResponse response = generated.toDetailResponse(entity);

            assertAll("toDetailResponse",
                    () -> assertEquals(id, response.id()),
                    () -> assertEquals("desc", response.description()),
                    () -> assertEquals("loc", response.location()),
                    () -> assertEquals("HTML", response.fileFormat()),
                    () -> assertEquals(templateType.getId(), response.templateTypeId()),
                    () -> assertEquals("Marketing", response.templateTypeName()),
                    () -> assertEquals(application.getId(), response.applicationId()),
                    () -> assertEquals(severityType.getId(), response.severityTypeId()),
                    () -> assertEquals("High", response.severityTypeName()),
                    () -> assertEquals(Boolean.FALSE, response.active()),
                    () -> assertEquals(now, response.createdAt()),
                    () -> assertEquals(now, response.updatedAt())
            );
        }

        @Test
        @DisplayName("toTemplateTO maps the real active state instead of hardcoding true")
        void toTemplateTO_mapsRealActiveState() {
            TemplateEntity entity = new TemplateEntity();
            entity.setId(UUID.randomUUID());
            entity.setDescription("desc");
            entity.setLocation("loc");
            entity.setFileFormat("HTML");
            entity.setTemplateType(templateType);
            entity.setApplication(application);
            entity.setActive(false);

            TemplateTO result = generated.toTemplateTO(entity);

            assertThat(result.isActive()).isFalse();
        }
    }
}
