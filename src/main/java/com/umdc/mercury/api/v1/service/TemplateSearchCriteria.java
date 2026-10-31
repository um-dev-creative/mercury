package com.umdc.mercury.api.v1.service;

import java.util.UUID;

/**
 * Internal parameter object bundling every filter/paging option accepted by
 * {@link TemplateService#searchTemplates(TemplateSearchCriteria)}.
 *
 * <p>{@code applicationId} is the mandatory security scope; every other field is an
 * optional filter or a paging/sort option that the service normalizes and bounds.</p>
 *
 * @param applicationId  mandatory application scope.
 * @param requesterId    UUID of the authenticated caller (from the session token).
 * @param sessionToken   the caller's raw session token, forwarded to
 *                       {@link ApplicationAuthorizationService} for the per-application permission check.
 * @param q              optional free-text filter matched against description/location.
 * @param templateTypeId optional exact template type filter.
 * @param severityTypeId optional exact severity type filter.
 * @param active         optional exact active-state filter; {@code null} defaults to active-only.
 * @param page           zero-based page index; {@code null} defaults to {@code 0}.
 * @param size           page size; {@code null} defaults to a service default, bounded by a safe maximum.
 * @param sort           {@code field,direction} sort spec; {@code null} defaults to {@code createdAt,desc}.
 */
public record TemplateSearchCriteria(
        UUID applicationId,
        UUID requesterId,
        String sessionToken,
        String q,
        UUID templateTypeId,
        UUID severityTypeId,
        Boolean active,
        Integer page,
        Integer size,
        String sort
) {
}
