package com.umdc.mercury.template.markdown;

/**
 * Markdown templates are stored as written (Markdown with FreeMarker in it). To render one, the source is wrapped in
 * the {@code markdown} directive, which adds one line on top.
 */
public final class MarkdownTemplates {

    /** Lines the wrapper adds before the author's first line. */
    public static final int WRAPPER_LINES = 1;

    private static final String OPEN = "<@markdown>\n";
    private static final String CLOSE = "\n</@markdown>";

    private MarkdownTemplates() {
    }

    /** @return whether {@code name} is a Markdown template. */
    public static boolean isMarkdown(String name) {
        return name != null && name.endsWith(".md");
    }

    /** @return the source FreeMarker must read for a template called {@code name}. */
    public static String forFreeMarker(String name, String source) {
        return isMarkdown(name) ? OPEN + source + CLOSE : source;
    }

    /** @return the number of lines {@link #forFreeMarker} adds on top of {@code name}'s source. */
    public static int lineOffset(String name) {
        return isMarkdown(name) ? WRAPPER_LINES : 0;
    }
}
