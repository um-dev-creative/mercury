package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateSearchResponse;
import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages the reusable base {@code TemplateEntity} lifecycle: create, update, soft-delete,
 * single lookup and application-scoped search.
 *
 * <p>This service does not manage {@code TemplateDefinedEntity} (application/user-specific
 * template usage referenced by campaigns and message records) — see
 * {@link TemplateDefinedService} for that.</p>
 */
public interface TemplateService {

    /**
     * Validates the request and its references, persists a new active template and returns
     * its persisted representation.
     *
     * @param sessionToken the caller's raw session token, used for the per-application
     *                     permission check against backbone-rest.
     */
    CompletableFuture<TemplateDetailResponse> createTemplate(CreateTemplateRequest request, UUID requesterId, String sessionToken);

    /**
     * Applies mutable metadata changes to an existing, active template. {@code applicationId}
     * and {@code createdAt} are preserved; {@code updatedAt} is refreshed when a change is applied.
     */
    TemplateDetailResponse updateTemplate(UUID id, UpdateTemplateRequest request, UUID requesterId, String sessionToken);

    /**
     * Soft-deletes a template by setting {@code active=false}. Idempotent: deleting an
     * already-inactive template succeeds without further side effects.
     */
    void deleteTemplate(UUID id, UUID requesterId, String sessionToken);

    /**
     * Retrieves a single active template by id. Unknown or inactive ids are both reported
     * as not found.
     */
    TemplateDetailResponse getById(UUID id, UUID requesterId, String sessionToken);

    /**
     * Searches templates scoped to {@link TemplateSearchCriteria#applicationId()}, applying
     * the requested filters, paging and sort in the database.
     */
    TemplateSearchResponse searchTemplates(TemplateSearchCriteria criteria);
}
