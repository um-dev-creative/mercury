package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.api.v1.service.TemplateSearchCriteria;
import com.umdc.mercury.api.v1.service.TemplateService;
import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateSearchResponse;
import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller that handles reusable base template management operations.
 *
 * <p>Implements the {@link TemplateApi} contract and delegates business logic to
 * {@link TemplateService}. {@link TemplateApi} is not {@code @SkipSessionValidation}, so
 * Mercury's {@code SessionJwtInterceptor} already verifies {@code session-token}'s
 * signature before any of these methods run; {@code requesterId} below is still resolved
 * through {@link SessionJwtServiceImpl#getVerifiedUid(String)} — not the unverified
 * {@code com.umdc.commons.util.JwtUtil.getUidFromToken} — so this controller doesn't
 * silently depend on that interceptor coverage remaining in place, and a malformed
 * {@code uid} claim fails loudly instead of flowing through as a {@code null} requester.</p>
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController implements TemplateApi {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or missing session token.";

    private final TemplateService templateService;
    private final SessionJwtServiceImpl sessionJwtService;

    public TemplateController(TemplateService templateService, SessionJwtServiceImpl sessionJwtService) {
        this.templateService = templateService;
        this.sessionJwtService = sessionJwtService;
    }

    private UUID resolveVerifiedUserId(String sessionToken) {
        return sessionJwtService.getVerifiedUid(sessionToken)
                .orElseThrow(() -> new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE));
    }

    @Override
    public ResponseEntity<TemplateDetailResponse> createTemplate(CreateTemplateRequest request, String sessionToken) {
        UUID requesterId = resolveVerifiedUserId(sessionToken);
        TemplateDetailResponse created = templateService.createTemplate(request, requesterId, sessionToken).join();
        return ResponseEntity
                .created(URI.create("/api/v1/templates/" + created.id()))
                .body(created);
    }

    @Override
    public ResponseEntity<TemplateDetailResponse> updateTemplate(UUID id, String sessionToken, UpdateTemplateRequest request) {
        UUID requesterId = resolveVerifiedUserId(sessionToken);
        TemplateDetailResponse updated = templateService.updateTemplate(id, request, requesterId, sessionToken);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<Void> deleteTemplate(UUID id, String sessionToken) {
        UUID requesterId = resolveVerifiedUserId(sessionToken);
        templateService.deleteTemplate(id, requesterId, sessionToken);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<TemplateDetailResponse> getTemplateById(UUID id, String sessionToken) {
        UUID requesterId = resolveVerifiedUserId(sessionToken);
        return ResponseEntity.ok(templateService.getById(id, requesterId, sessionToken));
    }

    @Override
    public ResponseEntity<TemplateSearchResponse> searchTemplates(UUID applicationId, String q, UUID templateTypeId,
                                                                    UUID severityTypeId, Boolean active, Integer page,
                                                                    Integer size, String sort, String sessionToken) {
        UUID requesterId = resolveVerifiedUserId(sessionToken);
        TemplateSearchCriteria criteria = new TemplateSearchCriteria(
                applicationId, requesterId, sessionToken, q, templateTypeId, severityTypeId, active, page, size, sort);
        return ResponseEntity.ok(templateService.searchTemplates(criteria));
    }
}
