package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import com.umdc.mercury.jpa.sql.entity.SeverityTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateTypeEntity;
import com.umdc.mercury.jpa.sql.repository.SeverityTypeRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateTypeEntityRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Applies the mutable fields of an {@link UpdateTemplateRequest} onto an existing
 * {@link TemplateEntity}, field by field, skipping any field left unset in the request.
 *
 * <p>{@code applicationId} is deliberately not handled here — it is immutable once a
 * template is created (see {@link UpdateTemplateRequest}).</p>
 */
@Component
public class TemplateUpdateApplier {

    private final TemplateTypeEntityRepository templateTypeRepository;
    private final SeverityTypeRepository severityTypeRepository;

    public TemplateUpdateApplier(TemplateTypeEntityRepository templateTypeRepository,
                                  SeverityTypeRepository severityTypeRepository) {
        this.templateTypeRepository = templateTypeRepository;
        this.severityTypeRepository = severityTypeRepository;
    }

    /**
     * Applies every mutable field present in {@code request} onto {@code entity}.
     *
     * @return {@code true} if at least one field was changed, {@code false} otherwise
     */
    public boolean apply(TemplateEntity entity, UpdateTemplateRequest request) {
        boolean changed = false;
        changed |= applyDescription(entity, request);
        changed |= applyLocation(entity, request);
        changed |= applyFileFormat(entity, request);
        changed |= applyTemplateType(entity, request);
        changed |= applySeverityType(entity, request);
        return changed;
    }

    private boolean applyDescription(TemplateEntity entity, UpdateTemplateRequest request) {
        if (Objects.isNull(request.description()) || request.description().isBlank()) {
            return false;
        }
        entity.setDescription(request.description());
        return true;
    }

    private boolean applyLocation(TemplateEntity entity, UpdateTemplateRequest request) {
        if (Objects.isNull(request.location()) || request.location().isBlank()) {
            return false;
        }
        entity.setLocation(request.location());
        return true;
    }

    private boolean applyFileFormat(TemplateEntity entity, UpdateTemplateRequest request) {
        if (Objects.isNull(request.fileFormat()) || request.fileFormat().isBlank()) {
            return false;
        }
        entity.setFileFormat(request.fileFormat());
        return true;
    }

    private boolean applyTemplateType(TemplateEntity entity, UpdateTemplateRequest request) {
        if (Objects.isNull(request.templateTypeId())) {
            return false;
        }
        TemplateTypeEntity templateType = templateTypeRepository.findById(request.templateTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Template type not found: " + request.templateTypeId()));
        if (!Boolean.TRUE.equals(templateType.getActive())) {
            throw new IllegalArgumentException("Template type is not active: " + request.templateTypeId());
        }
        entity.setTemplateType(templateType);
        return true;
    }

    private boolean applySeverityType(TemplateEntity entity, UpdateTemplateRequest request) {
        if (Objects.isNull(request.severityTypeId())) {
            return false;
        }
        SeverityTypeEntity severityType = severityTypeRepository.findById(request.severityTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Severity type not found: " + request.severityTypeId()));
        if (!Boolean.TRUE.equals(severityType.getActive())) {
            throw new IllegalArgumentException("Severity type is not active: " + request.severityTypeId());
        }
        entity.setSeverityType(severityType);
        return true;
    }
}
