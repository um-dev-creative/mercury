package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.jpa.sql.entity.FrequencyTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.repository.FrequencyTypeRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Keeps {@code template_defined} (the row a campaign, {@code POST /api/v1/messages} and the template catalogue read) in step with {@code templates}.
 * Mercury is the only place templates are managed, so creating a template also makes it usable: before this class nothing wrote
 * {@code template_defined} and every send by template name was refused with "not registered for this application".
 */
@Component
public class TemplateDefinitionRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(TemplateDefinitionRegistrar.class);

    private final TemplateDefinedRepository definitions;
    private final FrequencyTypeRepository frequencyTypes;
    private final String defaultFrequencyName;

    public TemplateDefinitionRegistrar(TemplateDefinedRepository definitions, FrequencyTypeRepository frequencyTypes,
                                       @Value("${umdc.template.default-frequency-type:ON_DEMAND}") String defaultFrequencyName) {
        this.definitions = definitions;
        this.frequencyTypes = frequencyTypes;
        this.defaultFrequencyName = defaultFrequencyName;
    }

    /** Gives {@code template} its active definition (idempotent: an active one is kept) and returns it. */
    public TemplateDefinedEntity register(TemplateEntity template, UUID userId) {
        List<TemplateDefinedEntity> existing = definitions.findByTemplateId(template.getId());
        for (TemplateDefinedEntity definition : existing) {
            if (Boolean.TRUE.equals(definition.getIsActive())) {
                return definition;
            }
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        TemplateDefinedEntity definition = existing.isEmpty() ? new TemplateDefinedEntity() : existing.getFirst();
        definition.setTemplate(template);
        definition.setUserId(userId);
        definition.setApplicationId(template.getApplicationId());
        definition.setFrequencyType(Objects.requireNonNullElseGet(definition.getFrequencyType(), this::defaultFrequency));
        definition.setCreatedAt(Objects.requireNonNullElse(definition.getCreatedAt(), now));
        definition.setUpdatedAt(now);
        definition.setIsActive(true);
        TemplateDefinedEntity saved = definitions.save(definition);
        logger.info("Template definition registered. templateId={}, applicationId={}", template.getId(), template.getApplicationId());
        return saved;
    }

    /** Switches off every definition of a template that was soft-deleted, so it cannot be sent any more. */
    public void deactivate(UUID templateId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (TemplateDefinedEntity definition : definitions.findByTemplateId(templateId)) {
            if (!Boolean.FALSE.equals(definition.getIsActive())) {
                definition.setIsActive(false);
                definition.setUpdatedAt(now);
                definitions.save(definition);
            }
        }
    }

    /** The frequency every definition gets (the catalogue has no other meaning for an on-demand template): found by name, created if absent. */
    private FrequencyTypeEntity defaultFrequency() {
        return frequencyTypes.findFirstByNameIgnoreCaseAndActiveTrue(defaultFrequencyName).orElseGet(() -> {
            Instant now = Instant.now();
            FrequencyTypeEntity created = new FrequencyTypeEntity();
            created.setId(UUID.randomUUID());
            created.setName(defaultFrequencyName);
            created.setDescription("Sent when requested, not on a schedule");
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            created.setActive(true);
            return frequencyTypes.save(created);
        });
    }
}
