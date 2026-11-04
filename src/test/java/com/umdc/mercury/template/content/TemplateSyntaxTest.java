package com.umdc.mercury.template.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateSyntaxTest {

    @Test
    void aValidTemplateHasNoProblems() {
        assertThat(TemplateSyntax.validate("a.ftl", "Hola ${name}<#if x>!</#if>")).isEmpty();
    }

    @Test
    void anUnclosedDirectiveIsLocated() {
        List<TemplateProblem> problems = TemplateSyntax.validate("a.ftl", "línea uno\n<#if x>\nsin cerrar");

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0).line()).isGreaterThanOrEqualTo(2);
        assertThat(problems.get(0).message()).isNotBlank();
    }

    @Test
    void aBadInterpolationIsReported() {
        assertThat(TemplateSyntax.validate("a.ftl", "${")).hasSize(1);
    }

    @Test
    void markdownLinesAreCountedFromTheAuthorsFirstLine() {
        List<TemplateProblem> problems = TemplateSyntax.validate("a.md", "# Título\n<#foo>\nfin");

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0).line()).isEqualTo(2);
    }

    @Test
    void aMarkdownTemplateWithGoodSyntaxPasses() {
        assertThat(TemplateSyntax.validate("a.md", "# Hola ${name}\n\n- uno")).isEmpty();
    }

    @Test
    void parametersAreTheNamesTheTemplateReads() {
        assertThat(TemplateSyntax.parameters("Hola ${name}, tu código ${ vc }<#if expiresInMinutes??>…</#if>"))
                .containsExactly("name", "vc", "expiresInMinutes");
    }

    @Test
    void namesTheTemplateDeclaresAreNotParameters() {
        String source = "<#assign total = 3><#list items as item>${item} ${total}</#list><#macro box title body>${title}${body}</#macro>${real}";

        assertThat(TemplateSyntax.parameters(source)).containsExactly("items", "real");
    }

    @Test
    void listVariablesWithIndexAreDeclarations() {
        assertThat(TemplateSyntax.parameters("<#list rows as row, idx>${row}${idx}</#list>")).containsExactly("rows");
    }

    @Test
    void repeatedAndReservedNamesAppearOnce() {
        assertThat(TemplateSyntax.parameters("${a}${a}<#if true>${b}</#if>")).containsExactly("a", "b");
    }

    @Test
    void aTemplateWithoutParametersHasNone() {
        assertThat(TemplateSyntax.parameters("Hola mundo")).isEmpty();
    }
}
