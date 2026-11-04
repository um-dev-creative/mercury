package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /api/v1/templates/{id}/content}.
 *
 * @param content The whole template as written (UTF-8 text).
 */
public record SaveTemplateContentRequest(@NotNull String content) {
}
