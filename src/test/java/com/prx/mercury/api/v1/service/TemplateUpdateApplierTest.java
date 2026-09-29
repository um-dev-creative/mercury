package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.service.TemplateUpdateApplier;
import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import com.umdc.mercury.jpa.sql.entity.SeverityTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateTypeEntity;
import com.umdc.mercury.jpa.sql.repository.SeverityTypeRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateTypeEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateUpdateApplier unit tests")
class TemplateUpdateApplierTest {

    @Mock
    private TemplateTypeEntityRepository templateTypeRepository;

    @Mock
    private SeverityTypeRepository severityTypeRepository;

    private TemplateUpdateApplier applier;
    private TemplateEntity entity;

    @BeforeEach
    void setUp() {
        applier = new TemplateUpdateApplier(templateTypeRepository, severityTypeRepository);
        entity = new TemplateEntity();
        entity.setDescription("Old description");
        entity.setLocation("old/loc");
        entity.setFileFormat("PDF");
    }

    @Nested
    @DisplayName("no-op requests")
    class NoOp {

        @Test
        @DisplayName("returns false and changes nothing when every field is null")
        void allNull_noChange() {
            boolean changed = applier.apply(entity, new UpdateTemplateRequest(null, null, null, null, null));

            assertThat(changed).isFalse();
            assertThat(entity.getDescription()).isEqualTo("Old description");
            assertThat(entity.getLocation()).isEqualTo("old/loc");
            assertThat(entity.getFileFormat()).isEqualTo("PDF");
        }

        @Test
        @DisplayName("blank description/location/fileFormat are treated as unset")
        void blankFields_noChange() {
            boolean changed = applier.apply(entity, new UpdateTemplateRequest(" ", " ", " ", null, null));

            assertThat(changed).isFalse();
            assertThat(entity.getDescription()).isEqualTo("Old description");
        }
    }

    @Nested
    @DisplayName("simple field updates")
    class SimpleFields {

        @Test
        @DisplayName("applies description, location and fileFormat")
        void appliesTextFields() {
            var request = new UpdateTemplateRequest("New description", "new/loc", "HTML", null, null);

            boolean changed = applier.apply(entity, request);

            assertThat(changed).isTrue();
            assertThat(entity.getDescription()).isEqualTo("New description");
            assertThat(entity.getLocation()).isEqualTo("new/loc");
            assertThat(entity.getFileFormat()).isEqualTo("HTML");
        }
    }

    @Nested
    @DisplayName("templateTypeId updates")
    class TemplateTypeUpdates {

        @Test
        @DisplayName("resolves and applies an active template type")
        void appliesActiveTemplateType() {
            UUID typeId = UUID.randomUUID();
            TemplateTypeEntity type = new TemplateTypeEntity();
            type.setId(typeId);
            type.setActive(true);
            when(templateTypeRepository.findById(typeId)).thenReturn(Optional.of(type));

            boolean changed = applier.apply(entity, new UpdateTemplateRequest(null, null, null, typeId, null));

            assertThat(changed).isTrue();
            assertThat(entity.getTemplateType()).isSameAs(type);
        }

        @Test
        @DisplayName("throws when template type does not exist")
        void throwsWhenNotFound() {
            UUID typeId = UUID.randomUUID();
            when(templateTypeRepository.findById(typeId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> applier.apply(entity, new UpdateTemplateRequest(null, null, null, typeId, null)));
        }

        @Test
        @DisplayName("throws when template type is inactive")
        void throwsWhenInactive() {
            UUID typeId = UUID.randomUUID();
            TemplateTypeEntity type = new TemplateTypeEntity();
            type.setId(typeId);
            type.setActive(false);
            when(templateTypeRepository.findById(typeId)).thenReturn(Optional.of(type));

            assertThrows(IllegalArgumentException.class,
                    () -> applier.apply(entity, new UpdateTemplateRequest(null, null, null, typeId, null)));
        }
    }

    @Nested
    @DisplayName("severityTypeId updates")
    class SeverityTypeUpdates {

        @Test
        @DisplayName("resolves and applies an active severity type")
        void appliesActiveSeverityType() {
            UUID severityId = UUID.randomUUID();
            SeverityTypeEntity severity = new SeverityTypeEntity();
            severity.setId(severityId);
            severity.setActive(true);
            when(severityTypeRepository.findById(severityId)).thenReturn(Optional.of(severity));

            boolean changed = applier.apply(entity, new UpdateTemplateRequest(null, null, null, null, severityId));

            assertThat(changed).isTrue();
            assertThat(entity.getSeverityType()).isSameAs(severity);
        }

        @Test
        @DisplayName("throws when severity type does not exist")
        void throwsWhenNotFound() {
            UUID severityId = UUID.randomUUID();
            when(severityTypeRepository.findById(severityId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> applier.apply(entity, new UpdateTemplateRequest(null, null, null, null, severityId)));
        }

        @Test
        @DisplayName("throws when severity type is inactive")
        void throwsWhenInactive() {
            UUID severityId = UUID.randomUUID();
            SeverityTypeEntity severity = new SeverityTypeEntity();
            severity.setId(severityId);
            severity.setActive(false);
            when(severityTypeRepository.findById(severityId)).thenReturn(Optional.of(severity));

            assertThrows(IllegalArgumentException.class,
                    () -> applier.apply(entity, new UpdateTemplateRequest(null, null, null, null, severityId)));
        }
    }
}
