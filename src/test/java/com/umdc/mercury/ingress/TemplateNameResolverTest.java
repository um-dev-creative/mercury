package com.umdc.mercury.ingress;

import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateNameResolverTest {

    private final UUID app = UUID.randomUUID();
    private final TemplateDefinedRepository repository = mock(TemplateDefinedRepository.class);
    private final TemplateNameResolver resolver = new TemplateNameResolver(repository);

    private TemplateDefinedEntity definition(UUID id, String location, String language, Boolean active) {
        var template = mock(TemplateEntity.class);
        when(template.getLocation()).thenReturn(location);
        when(template.getLanguage()).thenReturn(language);
        when(template.getActive()).thenReturn(active);
        var defined = mock(TemplateDefinedEntity.class);
        when(defined.getId()).thenReturn(id);
        when(defined.getTemplate()).thenReturn(template);
        return defined;
    }

    private void offer(TemplateDefinedEntity... definitions) {
        when(repository.findByApplicationIdAndIsActiveTrue(app)).thenReturn(List.of(definitions));
    }

    @Test
    void aTemplateIsFoundByNameAndLanguage() {
        var es = UUID.randomUUID();
        var en = UUID.randomUUID();
        offer(definition(es, "zb_verification_es.ftl", "es", true), definition(en, "zb_verification_en.ftl", "en", true));

        assertEquals(es, resolver.resolve(app, "zb_verification", "es"));
        assertEquals(en, resolver.resolve(app, "zb_verification", "EN"));
    }

    @Test
    void aMissingLanguageIsRefusedAndTheAvailableOnesAreListedWithNoFallback() {
        offer(definition(UUID.randomUUID(), "zb_verification_es.ftl", "es", true));

        var e = assertThrows(IllegalArgumentException.class, () -> resolver.resolve(app, "zb_verification", "en"));

        assertTrue(e.getMessage().contains("no 'en' version"));
        assertTrue(e.getMessage().contains("available: es"));
    }

    @Test
    void anUnknownTemplateIsRefused() {
        offer();

        var e = assertThrows(IllegalArgumentException.class, () -> resolver.resolve(app, "zb_nothing", "es"));

        assertTrue(e.getMessage().contains("not registered"));
    }

    @Test
    void inactiveTemplatesAndOnesWithoutALanguageAreNotOffered() {
        offer(
                definition(UUID.randomUUID(), "zb_a_es.ftl", "es", false),
                definition(UUID.randomUUID(), "zb_b.ftl", null, true),
                definition(UUID.randomUUID(), "zb_c.ftl", " ", true),
                definition(UUID.randomUUID(), "zb_d_es.ftl", "es", null));

        assertEquals(List.of(new TemplateNameResolver.Entry("zb_d", List.of("es"))), resolver.catalogue(app));
    }

    @Test
    void theCatalogueGroupsLanguagesPerNameSorted() {
        offer(
                definition(UUID.randomUUID(), "zb_verification_es.ftl", "es", true),
                definition(UUID.randomUUID(), "zb_debt_due_en.ftl", "en", true),
                definition(UUID.randomUUID(), "zb_verification_en.ftl", "en", true));

        assertEquals(List.of(new TemplateNameResolver.Entry("zb_debt_due", List.of("en")),
                new TemplateNameResolver.Entry("zb_verification", List.of("en", "es"))), resolver.catalogue(app));
    }
}
