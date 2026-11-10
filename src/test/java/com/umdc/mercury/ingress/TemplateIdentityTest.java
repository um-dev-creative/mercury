package com.umdc.mercury.ingress;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateIdentityTest {

    @Test
    void theLanguageIsTheSuffixOfTheLocationBeforeTheExtension() {
        assertEquals(Optional.of("es"), TemplateIdentity.languageOf("zb_verification_es.ftl"));
        assertEquals(Optional.of("pt-BR"), TemplateIdentity.languageOf("zb_x_tg_pt-BR.ftl"));
        assertEquals(Optional.empty(), TemplateIdentity.languageOf("welcome.ftl"));
        assertEquals(Optional.empty(), TemplateIdentity.languageOf(null));
    }

    @Test
    void theNameIsTheLocationWithoutExtensionAndLanguage() {
        assertEquals("zb_verification", TemplateIdentity.nameOf("zb_verification_es.ftl", "es"));
        assertEquals("zb_debt_due_tg", TemplateIdentity.nameOf("zb_debt_due_tg_EN.ftl", "en"));
        assertEquals("welcome", TemplateIdentity.nameOf("welcome.ftl", "es"));
        assertEquals("welcome", TemplateIdentity.nameOf("welcome", null));
    }
}
