package com.umdc.mercury.api.v1.to;

import java.util.List;

/**
 * Stable, bounded paginated envelope returned by the template search endpoint.
 *
 * @param items          the templates matching the requested filters, for this page only.
 * @param page           zero-based index of the returned page.
 * @param size           maximum number of items requested per page.
 * @param totalElements  total number of templates matching the filters, across all pages.
 * @param totalPages     total number of pages available for the requested {@code size}.
 */
public record TemplateSearchResponse(
        List<TemplateDetailResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
