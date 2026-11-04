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
    @ValueSource(strings = {"", " ", "a", "a.txt", "../a.ftl", "a/b.ftl", "a\\b.ftl", ".a.ftl", "-a.ftl", "a..ftl", "a b.ftl", "a.ftl ", "a.FTL"})
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
