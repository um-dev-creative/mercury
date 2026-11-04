package com.umdc.mercury.template.markdown;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Markdown to HTML for message templates. Raw HTML in the Markdown is escaped (it shows as text) and dangerous link
 * targets such as {@code javascript:} are dropped, so a Markdown template cannot carry active content; a template that
 * needs HTML is written as HTML.
 */
public final class MarkdownRenderer {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();

    private MarkdownRenderer() {
    }

    /** @return the HTML of {@code markdown}. */
    public static String toHtml(String markdown) {
        return HTML.render(PARSER.parse(markdown));
    }
}
