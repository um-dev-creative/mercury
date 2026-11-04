package com.umdc.mercury.template.storage;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageTemplateLoaderTest {

    @TempDir
    Path dir;

    private LocalTemplateStorage storage;
    private Configuration config;

    @BeforeEach
    void setUp() {
        storage = new LocalTemplateStorage(dir);
        config = new Configuration(Configuration.VERSION_2_3_30);
        config.setTemplateLoader(new StorageTemplateLoader(storage));
        config.setDefaultEncoding("UTF-8");
        config.setTemplateUpdateDelayMilliseconds(0);
    }

    private String render(String name, Map<String, Object> model) throws Exception {
        StringWriter out = new StringWriter();
        config.getTemplate(name).process(model, out);
        return out.toString();
    }

    @Test
    void rendersATemplateFromTheStorage() throws Exception {
        storage.write("hola.ftl", "Hola ${name}, ¿qué tal?".getBytes(StandardCharsets.UTF_8), null);

        assertThat(render("hola.ftl", Map.of("name", "Ana"))).isEqualTo("Hola Ana, ¿qué tal?");
    }

    @Test
    void aMissingTemplateIsNotFound() {
        assertThrows(freemarker.template.TemplateNotFoundException.class, () -> config.getTemplate("nada.ftl"));
    }

    @Test
    void anInvalidNameIsNotFoundInsteadOfEscapingTheFolder() {
        assertThrows(IOException.class, () -> config.getTemplate("../outside.ftl"));
    }

    @Test
    void localizedLookupFallsBackToTheBaseName() throws Exception {
        storage.write("saludo.ftl", "base".getBytes(StandardCharsets.UTF_8), null);

        Template template = config.getTemplate("saludo.ftl", new Locale("es", "MX"));

        assertThat(template.getName()).isEqualTo("saludo.ftl");
    }

    @Test
    void localizedLookupPrefersAMoreSpecificTemplate() throws Exception {
        storage.write("saludo.ftl", "base".getBytes(StandardCharsets.UTF_8), null);
        storage.write("saludo_es.ftl", "español".getBytes(StandardCharsets.UTF_8), null);

        StringWriter out = new StringWriter();
        config.getTemplate("saludo.ftl", new Locale("es")).process(Map.of(), out);

        assertThat(out.toString()).isEqualTo("español");
    }

    @Test
    void anEditedTemplateIsPickedUpOnTheNextRead() throws Exception {
        String etag = storage.write("c.ftl", "uno".getBytes(StandardCharsets.UTF_8), null).etag();
        assertThat(render("c.ftl", Map.of())).isEqualTo("uno");

        storage.write("c.ftl", "dos".getBytes(StandardCharsets.UTF_8), etag);

        assertThat(render("c.ftl", Map.of())).isEqualTo("dos");
    }

    @Test
    void anEditInsideTheSameClockTickIsStillPickedUp() throws Exception {
        String etag = storage.write("t.ftl", "uno".getBytes(StandardCharsets.UTF_8), null).etag();
        java.nio.file.attribute.FileTime tick = java.nio.file.Files.getLastModifiedTime(dir.resolve("t.ftl"));
        assertThat(render("t.ftl", Map.of())).isEqualTo("uno");

        storage.write("t.ftl", "dos".getBytes(StandardCharsets.UTF_8), etag);
        // a file system with a coarse clock gives both edits the same modification time
        java.nio.file.Files.setLastModifiedTime(dir.resolve("t.ftl"), tick);

        assertThat(render("t.ftl", Map.of())).isEqualTo("dos");
    }

    @Test
    void aDeletedTemplateIsNoLongerServed() throws Exception {
        storage.write("d.ftl", "x".getBytes(StandardCharsets.UTF_8), null);
        assertThat(render("d.ftl", Map.of())).isEqualTo("x");

        storage.delete("d.ftl");

        assertThrows(freemarker.template.TemplateNotFoundException.class, () -> config.getTemplate("d.ftl"));
    }

    @Test
    void sourcesAreEqualByName() {
        StorageTemplateLoader loader = new StorageTemplateLoader(storage);
        storage.write("e.ftl", "1".getBytes(StandardCharsets.UTF_8), null);
        Object first = loader.findTemplateSource("e.ftl");
        Object second = loader.findTemplateSource("e.ftl");

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo("e.ftl");
        loader.closeTemplateSource(first);
    }
}
