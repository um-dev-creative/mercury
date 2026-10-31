package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.RecipientTO;
import com.umdc.mercury.api.v1.to.UpdateCampaignRequest;
import com.umdc.mercury.jpa.sql.entity.CampaignEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Applies the mutable fields of an {@link UpdateCampaignRequest} onto an existing
 * {@link CampaignEntity}, field by field, skipping any field left unset in the request.
 *
 * <p>Extracted out of {@link CampaignServiceImpl} to keep that class focused on
 * orchestration rather than per-field patch logic.</p>
 */
@Component
public class CampaignUpdateApplier {

    private final TemplateDefinedRepository templateDefinedRepository;

    public CampaignUpdateApplier(TemplateDefinedRepository templateDefinedRepository) {
        this.templateDefinedRepository = templateDefinedRepository;
    }

    /**
     * Applies every mutable field present in {@code updateRequest} onto {@code entity}.
     *
     * @return {@code true} if at least one field was changed, {@code false} otherwise
     */
    public boolean apply(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        boolean changed = false;
        changed |= applyName(entity, updateRequest);
        changed |= applyTemplate(entity, updateRequest);
        changed |= applyApplicationId(entity, updateRequest);
        changed |= applyScheduledAt(entity, updateRequest);
        changed |= applyStatus(entity, updateRequest);
        changed |= applyTemplateParams(entity, updateRequest);
        changed |= applyRecipients(entity, updateRequest);
        return changed;
    }

    private boolean applyName(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        if (Objects.isNull(updateRequest.name()) || updateRequest.name().isBlank()) {
            return false;
        }
        entity.setName(updateRequest.name());
        return true;
    }

    private boolean applyTemplate(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        if (Objects.isNull(updateRequest.templateId())) {
            return false;
        }
        var template = templateDefinedRepository.findById(updateRequest.templateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + updateRequest.templateId()));
        entity.setTemplateDefined(template);
        return true;
    }

    private boolean applyApplicationId(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        if (Objects.isNull(updateRequest.applicationId())) {
            return false;
        }
        entity.setApplicationId(updateRequest.applicationId());
        return true;
    }

    private boolean applyScheduledAt(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        if (Objects.isNull(updateRequest.scheduledAt())) {
            return false;
        }
        entity.setScheduledAt(updateRequest.scheduledAt());
        return true;
    }

    private boolean applyStatus(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        if (Objects.isNull(updateRequest.status()) || updateRequest.status().isBlank()) {
            return false;
        }
        entity.setStatus(updateRequest.status());
        return true;
    }

    private boolean applyTemplateParams(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        if (Objects.isNull(updateRequest.templateParams())) {
            return false;
        }
        Map<String, Object> metadata = entity.getMetadata();
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
        // Store template parameters under a simple key to avoid complex nested types
        metadata.put("templateParams", updateRequest.templateParams());
        entity.setMetadata(metadata);
        return true;
    }

    private boolean applyRecipients(CampaignEntity entity, UpdateCampaignRequest updateRequest) {
        if (Objects.isNull(updateRequest.recipients())) {
            return false;
        }
        List<RecipientTO> deduped = dedupeRecipients(updateRequest.recipients());
        if (deduped.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }
        entity.setTotalRecipients(deduped.size());
        return true;
    }

    /** Deduplicates recipients by identifier, dropping entries with a blank/missing identifier. */
    private List<RecipientTO> dedupeRecipients(List<RecipientTO> recipients) {
        Set<String> seen = new LinkedHashSet<>();
        List<RecipientTO> deduped = new ArrayList<>();
        for (RecipientTO r : recipients) {
            if (r == null || r.identifier() == null || r.identifier().isBlank()) {
                continue;
            }
            if (seen.add(r.identifier())) {
                deduped.add(r);
            }
        }
        return deduped;
    }
}
