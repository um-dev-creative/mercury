package com.umdc.mercury.template.content;

import com.umdc.mercury.template.storage.LocalTemplateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplatePreviewTest {

    @TempDir
    Path dir;

    private LocalTemplateStorage storage;
    private TemplatePreview preview;

    @BeforeEach
    void setUp() {
        storage = new LocalTemplateStorage(dir);
        preview = new TemplatePreview(storage, 200, 2_000);
    }

    @Test
    void rendersWithTheSampleParameters() {
        TemplatePreview.Result result = preview.render("p.ftl", "Hola ${name}", Map.of("name", "Ana"));

        assertThat(result.ok()).isTrue();
        assertThat(result.output()).isEqualTo("Hola Ana");
    }

    @Test
    void rendersMarkdownAsHtml() {
        TemplatePreview.Result result = preview.render("p.md", "# Hola ${name}", Map.of("name", "Ana"));

        assertThat(result.output()).contains("<h1>Hola Ana</h1>");
    }

    @Test
    void nullParametersAreAnEmptyModel() {
        assertThat(preview.render("p.ftl", "fijo", null).output()).isEqualTo("fijo");
    }

    @Test
    void aSyntaxErrorIsLocatedAndNothingIsRendered() {
        TemplatePreview.Result result = preview.render("p.ftl", "a\n<#if x>", Map.of());

        assertThat(result.ok()).isFalse();
        assertThat(result.output()).isEmpty();
        assertThat(result.problems().get(0).line()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void aMissingParameterIsReportedWithoutLeakingAStackTrace() {
        TemplatePreview.Result result = preview.render("p.ftl", "Hola ${name}", new HashMap<>());

        assertThat(result.ok()).isFalse();
        assertThat(result.output()).isEmpty();
        assertThat(result.problems().get(0).message()).contains("name").doesNotContain("at freemarker");
    }

    @Test
    void refusesWhatProductionRefuses() {
        TemplatePreview.Result result = preview.render("p.ftl", "<#assign x=\"freemarker.template.utility.Execute\"?new()>${x(\"id\")}", Map.of());

        assertThat(result.ok()).isFalse();
        assertThat(result.problems().get(0).message()).containsIgnoringCase("not allowed");
    }

    @Test
    void stopsATemplateThatWritesTooMuch() {
        TemplatePreview.Result result = preview.render("p.ftl", "<#list 1..10000 as i>0123456789</#list>", Map.of());

        assertThat(result.ok()).isFalse();
        assertThat(result.problems().get(0).message()).contains("longer than 200");
    }

    @Test
    void stopsATemplateThatWritesForTooLong() throws Exception {
        TemplatePreview slow = new TemplatePreview(storage, 10_000_000, 0);

        TemplatePreview.Result result = slow.render("p.ftl", "<#list 1..1000 as i>x</#list>", Map.of());

        assertThat(result.ok()).isFalse();
        assertThat(result.problems().get(0).message()).contains("took too long");
    }

    @Test
    void canIncludeAStoredTemplate() {
        storage.write("header.ftl", "== ${title} ==".getBytes(StandardCharsets.UTF_8), null);

        TemplatePreview.Result result = preview.render("p.ftl", "<#include \"header.ftl\"> fin", Map.of("title", "Hola"));

        assertThat(result.output()).isEqualTo("== Hola == fin");
    }

    @Test
    void cannotIncludeOutsideTheStorage() {
        TemplatePreview.Result result = preview.render("p.ftl", "<#include \"../secret.ftl\">", Map.of());

        assertThat(result.ok()).isFalse();
    }
}
