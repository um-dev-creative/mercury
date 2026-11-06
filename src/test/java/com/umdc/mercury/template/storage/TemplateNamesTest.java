package com.umdc.mercury.template.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateNamesTest {

    @ParameterizedTest
    @ValueSource(strings = {"zb_debt_due_es.ftl", "iris_verification_en.ftl", "welcome.html", "a.md", "page.ftlh", "v1.2.ftl", "A-b_c.ftl"})
    void acceptsTemplateFileNames(String name) {
        assertThat(TemplateNames.isValid(name)).isTrue();
        assertThat(TemplateNames.require(name)).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"iris/iris_welcome.ftl", "zerobound/zb_debt_due_es.ftl", "smartkind/sk_x.md", "latinhub/a.html", "a-b_c9/v1.2.ftl"})
    void acceptsAFileInsideOneApplicationFolder(String name) {
        assertThat(TemplateNames.isValid(name)).isTrue();
        assertThat(TemplateNames.isFileName(name)).isFalse();
    }

    @Test
    void foldersAreLowerCaseNamesWithoutDotsOrSlashes() {
        assertThat(TemplateNames.isFolder("iris")).isTrue();
        assertThat(TemplateNames.isFolder("smart-kind_2")).isTrue();
        assertThat(TemplateNames.isFolder("a".repeat(TemplateNames.MAX_FOLDER_LENGTH))).isTrue();
        assertThat(TemplateNames.isFolder("a".repeat(TemplateNames.MAX_FOLDER_LENGTH + 1))).isFalse();
        for (String bad : new String[]{null, "", "Iris", "a.b", "a/b", "-a", "_a", "a b", ".."}) {
            assertThat(TemplateNames.isFolder(bad)).isFalse();
        }
    }

    @Test
    void inFolderAndFileOfAreInverse() {
        assertThat(TemplateNames.inFolder("iris", "a.ftl")).isEqualTo("iris/a.ftl");
        assertThat(TemplateNames.inFolder(null, "a.ftl")).isEqualTo("a.ftl");
        assertThat(TemplateNames.inFolder(" ", "a.ftl")).isEqualTo("a.ftl");
        assertThat(TemplateNames.fileOf("iris/a.ftl")).isEqualTo("a.ftl");
        assertThat(TemplateNames.fileOf("a.ftl")).isEqualTo("a.ftl");
        assertThat(TemplateNames.fileOf(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a", "a.txt", "../a.ftl", "a/b/c.ftl", "A/b.ftl", "-a/b.ftl", "a//b.ftl", "/b.ftl", "a/", "a/..", "a/../b.ftl", "a/.b.ftl", "a\\b.ftl", ".a.ftl", "-a.ftl", "a..ftl", "a b.ftl", "a.ftl ", "a.FTL"})
    void rejectsEverythingElse(String name) {
        assertThat(TemplateNames.isValid(name)).isFalse();
        assertThrows(IllegalArgumentException.class, () -> TemplateNames.require(name));
    }

    @Test
    void rejectsNullAndTooLongNames() {
        assertThat(TemplateNames.isValid(null)).isFalse();
        assertThat(TemplateNames.isValid("a".repeat(TemplateNames.MAX_LENGTH) + ".ftl")).isFalse();
        assertThat(TemplateNames.isValid("a".repeat(TemplateNames.MAX_LENGTH - 4) + ".ftl")).isTrue();
    }
}
