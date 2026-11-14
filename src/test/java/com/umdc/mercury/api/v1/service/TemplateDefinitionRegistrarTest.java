package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.jpa.sql.entity.FrequencyTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.repository.FrequencyTypeRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateDefinitionRegistrar")
class TemplateDefinitionRegistrarTest {

    @Mock
    private TemplateDefinedRepository definitions;
    @Mock
    private FrequencyTypeRepository frequencyTypes;

    private TemplateDefinitionRegistrar registrar;
    private TemplateEntity template;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        registrar = new TemplateDefinitionRegistrar(definitions, frequencyTypes, "ON_DEMAND");
        template = new TemplateEntity();
        template.setId(UUID.randomUUID());
        template.setApplicationId(UUID.randomUUID());
    }

    private static FrequencyTypeEntity frequency() {
        FrequencyTypeEntity f = new FrequencyTypeEntity();
        f.setId(UUID.randomUUID());
        f.setName("ON_DEMAND");
        return f;
    }

    @Test
    @DisplayName("creates an active definition for the template, its application and the requester, with the existing frequency")
    void register_createsDefinition() {
        FrequencyTypeEntity frequency = frequency();
        when(definitions.findByTemplateId(template.getId())).thenReturn(List.of());
        when(frequencyTypes.findFirstByNameIgnoreCaseAndActiveTrue("ON_DEMAND")).thenReturn(Optional.of(frequency));
        when(definitions.save(any(TemplateDefinedEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateDefinedEntity saved = registrar.register(template, userId);

        assertThat(saved.getTemplate()).isSameAs(template);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getApplicationId()).isEqualTo(template.getApplicationId());
        assertThat(saved.getFrequencyType()).isSameAs(frequency);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(frequencyTypes, never()).save(any());
    }

    @Test
    @DisplayName("creates the default frequency type when the catalogue does not have it")
    void register_createsMissingFrequency() {
        when(definitions.findByTemplateId(template.getId())).thenReturn(List.of());
        when(frequencyTypes.findFirstByNameIgnoreCaseAndActiveTrue("ON_DEMAND")).thenReturn(Optional.empty());
        when(frequencyTypes.save(any(FrequencyTypeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(definitions.save(any(TemplateDefinedEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateDefinedEntity saved = registrar.register(template, userId);

        ArgumentCaptor<FrequencyTypeEntity> created = ArgumentCaptor.forClass(FrequencyTypeEntity.class);
        verify(frequencyTypes).save(created.capture());
        assertThat(created.getValue().getName()).isEqualTo("ON_DEMAND");
        assertThat(created.getValue().getActive()).isTrue();
        assertThat(created.getValue().getId()).isNotNull();
        assertThat(saved.getFrequencyType()).isSameAs(created.getValue());
    }

    @Test
    @DisplayName("is idempotent: an active definition is returned untouched")
    void register_keepsActiveDefinition() {
        TemplateDefinedEntity active = new TemplateDefinedEntity();
        active.setIsActive(true);
        when(definitions.findByTemplateId(template.getId())).thenReturn(List.of(active));

        assertThat(registrar.register(template, userId)).isSameAs(active);
        verify(definitions, never()).save(any());
    }

    @Test
    @DisplayName("reactivates an inactive definition instead of adding a second one, keeping its frequency and creation date")
    void register_reactivates() {
        FrequencyTypeEntity frequency = frequency();
        TemplateDefinedEntity inactive = new TemplateDefinedEntity();
        inactive.setIsActive(false);
        inactive.setFrequencyType(frequency);
        inactive.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        when(definitions.findByTemplateId(template.getId())).thenReturn(List.of(inactive));
        when(definitions.save(any(TemplateDefinedEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateDefinedEntity saved = registrar.register(template, userId);

        assertThat(saved).isSameAs(inactive);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getFrequencyType()).isSameAs(frequency);
        assertThat(saved.getCreatedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        verify(frequencyTypes, never()).findFirstByNameIgnoreCaseAndActiveTrue(any());
    }

    @Test
    @DisplayName("deactivate switches off only the definitions that are still on")
    void deactivate_switchesOffActiveOnes() {
        TemplateDefinedEntity on = new TemplateDefinedEntity();
        on.setIsActive(true);
        TemplateDefinedEntity off = new TemplateDefinedEntity();
        off.setIsActive(false);
        when(definitions.findByTemplateId(template.getId())).thenReturn(List.of(on, off));

        registrar.deactivate(template.getId());

        assertThat(on.getIsActive()).isFalse();
        verify(definitions).save(on);
        verify(definitions, never()).save(off);
    }
}
