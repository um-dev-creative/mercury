package com.umdc.mercury.template.markdown;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarkdownTemplatesTest {

    private static String render(String name, String source, Map<String, Object> model) throws Exception {
        Configuration config = com.umdc.mercury.config.FreeMarkerConfig.hardened(new freemarker.cache.StringTemplateLoader());
        StringWriter out = new StringWriter();
        new Template(name, new StringReader(MarkdownTemplates.forFreeMarker(name, source)), config).process(model, out);
        return out.toString();
    }

    @Test
    void recognisesMarkdownByExtension() {
        assertThat(MarkdownTemplates.isMarkdown("a.md")).isTrue();
        assertThat(MarkdownTemplates.isMarkdown("a.ftl")).isFalse();
        assertThat(MarkdownTemplates.isMarkdown(null)).isFalse();
        assertThat(MarkdownTemplates.lineOffset("a.md")).isEqualTo(1);
        assertThat(MarkdownTemplates.lineOffset("a.html")).isZero();
    }

    @Test
    void onlyMarkdownIsWrapped() {
        assertThat(MarkdownTemplates.forFreeMarker("a.ftl", "x")).isEqualTo("x");
        assertThat(MarkdownTemplates.forFreeMarker("a.md", "x")).startsWith("<@markdown>").endsWith("</@markdown>");
    }

    @Test
    void fillsInParametersBeforeReadingMarkdown() throws Exception {
        String html = render("w.md", "# Hola ${name}\n\nTu código: **${code}**", Map.of("name", "Ana", "code", "123456"));

        assertThat(html).contains("<h1>Hola Ana</h1>").contains("<strong>123456</strong>");
    }

    @Test
    void keepsUnderscoresInParameterNamesOutOfEmphasis() throws Exception {
        String html = render("w.md", "${first_name} y ${last_name}", Map.of("first_name", "Ana", "last_name", "Paz"));

        assertThat(html).contains("Ana y Paz").doesNotContain("<em>");
    }

    @Test
    void supportsDirectivesInsideMarkdown() throws Exception {
        String html = render("w.md", "<#list items as i>\n- ${i}\n</#list>", Map.of("items", java.util.List.of("uno", "dos")));

        assertThat(html).contains("<li>uno</li>").contains("<li>dos</li>");
    }

    @Test
    void rawHtmlIsEscapedAndScriptLinksAreDropped() {
        String html = MarkdownRenderer.toHtml("<script>alert(1)</script>\n\n[click](javascript:alert(1))");

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;").doesNotContain("javascript:");
    }

    @Test
    void anOrdinaryLinkIsKept() {
        assertThat(MarkdownRenderer.toHtml("[Iris](https://example.com)")).contains("href=\"https://example.com\"").contains(">Iris</a>");
    }

    @Test
    void theDirectiveTakesNoParameters() {
        assertThrows(Exception.class, () -> render("w.ftl", "<@markdown x=1>hi</@markdown>", Map.of()));
    }

    @Test
    void theDirectiveWithoutBodyWritesNothingUseful() throws Exception {
        assertThat(render("w.ftl", "<@markdown/>", Map.of())).isEmpty();
    }
}
