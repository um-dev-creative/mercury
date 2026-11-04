package com.umdc.mercury.api.v1.to;

import com.umdc.mercury.template.content.TemplateProblem;

import java.util.List;

/**
 * Body of the {@code 422} answer when a template cannot be saved because FreeMarker cannot read it.
 *
 * @param message  Why it was refused.
 * @param problems What is wrong, with line and column.
 */
public record TemplateProblemsResponse(String message, List<TemplateProblem> problems) {
}
