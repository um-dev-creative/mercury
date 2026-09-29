package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.TemplateSearchCriteria;
import com.umdc.mercury.api.v1.service.TemplateService;
import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateSearchResponse;
import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import static com.umdc.commons.util.JwtUtil.getUidFromToken;

/**
 * REST controller that handles reusable base template management operations.
 *
 * <p>Implements the {@link TemplateApi} contract and delegates business logic to
 * {@link TemplateService}.</p>
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController implements TemplateApi {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public ResponseEntity<TemplateDetailResponse> createTemplate(CreateTemplateRequest request, String sessionToken) {
        UUID requesterId = getUidFromToken(sessionToken);
        TemplateDetailResponse created = templateService.createTemplate(request, requesterId, sessionToken).join();
        return ResponseEntity
                .created(URI.create("/api/v1/templates/" + created.id()))
                .body(created);
    }

    @Override
    public ResponseEntity<TemplateDetailResponse> updateTemplate(UUID id, String sessionToken, UpdateTemplateRequest request) {
        UUID requesterId = getUidFromToken(sessionToken);
        TemplateDetailResponse updated = templateService.updateTemplate(id, request, requesterId, sessionToken);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<Void> deleteTemplate(UUID id, String sessionToken) {
        UUID requesterId = getUidFromToken(sessionToken);
        templateService.deleteTemplate(id, requesterId, sessionToken);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<TemplateDetailResponse> getTemplateById(UUID id, String sessionToken) {
        UUID requesterId = getUidFromToken(sessionToken);
        return ResponseEntity.ok(templateService.getById(id, requesterId, sessionToken));
    }

    @Override
    public ResponseEntity<TemplateSearchResponse> searchTemplates(UUID applicationId, String q, UUID templateTypeId,
                                                                    UUID severityTypeId, Boolean active, Integer page,
                                                                    Integer size, String sort, String sessionToken) {
        UUID requesterId = getUidFromToken(sessionToken);
        TemplateSearchCriteria criteria = new TemplateSearchCriteria(
                applicationId, requesterId, sessionToken, q, templateTypeId, severityTypeId, active, page, size, sort);
        return ResponseEntity.ok(templateService.searchTemplates(criteria));
    }
}
