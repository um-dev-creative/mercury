package com.umdc.mercury.jpa.sql.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemplateEntityLanguageTest {

    @Test
    void aTemplateSavedWithoutALanguageGetsTheOneItsFileNameEndsWith() {
        var template = new TemplateEntity();
        template.setLocation("zb_verification_es.ftl");

        template.deriveLanguage();

        assertEquals("es", template.getLanguage());
    }

    @Test
    void aLanguageSetExplicitlyIsKept() {
        var template = new TemplateEntity();
        template.setLocation("zb_verification_es.ftl");
        template.setLanguage("pt-BR");

        template.deriveLanguage();

        assertEquals("pt-BR", template.getLanguage());
    }

    @Test
    void aFileNameWithoutALanguageLeavesItEmpty() {
        var template = new TemplateEntity();
        template.setLocation("welcome.ftl");

        template.deriveLanguage();

        assertNull(template.getLanguage());
    }
}
