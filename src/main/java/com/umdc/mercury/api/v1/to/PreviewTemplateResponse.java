package com.umdc.mercury.api.v1.to;

import com.umdc.mercury.template.content.TemplateProblem;

import java.util.List;

/**
 * Result of a preview.
 *
 * @param ok         {@code true} if the template was rendered.
 * @param format     {@code MARKDOWN} or {@code FREEMARKER}; Markdown output is HTML.
 * @param output     The rendered message; empty when there are problems.
 * @param problems   What went wrong, with line and column.
 * @param parameters Parameters the template reads.
 */
public record PreviewTemplateResponse(boolean ok, String format, String output, List<TemplateProblem> problems,
                                      List<String> parameters) {
}
