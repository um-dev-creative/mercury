package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Body of {@code POST /api/v1/templates/preview}.
 *
 * @param applicationId Application the person is working in; they need the template permission on it.
 * @param name          File name the content would have; its extension tells HTML/FreeMarker from Markdown.
 * @param content       The template as written, saved or not.
 * @param params        Sample parameters.
 */
public record PreviewTemplateRequest(@NotNull UUID applicationId, @NotBlank String name, @NotNull String content,
                                     Map<String, Object> params) {
}
