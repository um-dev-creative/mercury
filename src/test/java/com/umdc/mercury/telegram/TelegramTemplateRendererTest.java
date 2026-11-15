package com.umdc.mercury.telegram;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Telegram template renderer")
class TelegramTemplateRendererTest {

    @TempDir
    Path dir;
    private TelegramTemplateRenderer renderer;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(dir.resolve("zb_debt_due_tg_es.ftl"), "Tu deuda <b>${name?html}</b> vence ${date}.");
        final var config = new Configuration(Configuration.VERSION_2_3_30);
        config.setDirectoryForTemplateLoading(dir.toFile());
        renderer = new TelegramTemplateRenderer(config, "");
    }

    @Test
    @DisplayName("renders zb_<kind>_tg_<lang> with the parameters, escaping what the template escapes")
    void renders() {
        assertEquals("Tu deuda <b>A &amp; B</b> vence 2026-11-05.", renderer.render("debt_due", "es", Map.of("name", "A & B", "date", "2026-11-05")));
    }

    @Test
    @DisplayName("exists only for a valid kind with a file in that language")
    void exists() {
        assertTrue(renderer.exists("debt_due", "es"));
        assertFalse(renderer.exists("debt_due", "en"));
        assertFalse(renderer.exists("../etc/passwd", "es"));
        assertFalse(renderer.exists("Debt_Due", "es"));
    }

    @Test
    @DisplayName("a missing template or a missing parameter is an IllegalStateException without the parameter values")
    void failures() {
        assertThrows(IllegalStateException.class, () -> renderer.render("nope", "es", Map.of()));
        final var e = assertThrows(IllegalStateException.class, () -> renderer.render("debt_due", "es", Map.of("secret", "1")));
        assertFalse(e.getMessage().contains("secret"));
    }

    @Test
    void theTemplateIsLookedUpInTheConfiguredApplicationFolder() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("tg");
        java.nio.file.Files.createDirectories(dir.resolve("zerobound"));
        java.nio.file.Files.writeString(dir.resolve("zerobound/zb_hello_tg_es.ftl"), "Hola ${name}");
        java.nio.file.Files.writeString(dir.resolve("zb_other_tg_es.ftl"), "root");
        var cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_30);
        cfg.setDirectoryForTemplateLoading(dir.toFile());
        var inFolder = new TelegramTemplateRenderer(cfg, "zerobound");

        org.junit.jupiter.api.Assertions.assertTrue(inFolder.exists("hello", "es"));
        org.junit.jupiter.api.Assertions.assertFalse(inFolder.exists("other", "es"));
        org.junit.jupiter.api.Assertions.assertEquals("Hola Ana", inFolder.render("hello", "es", java.util.Map.of("name", "Ana")));
    }

    private static com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity definition(UUID application, String location, String language, boolean active) {
        var template = mock(com.umdc.mercury.jpa.sql.entity.TemplateEntity.class);
        when(template.getLocation()).thenReturn(location);
        when(template.getLanguage()).thenReturn(language);
        when(template.getActive()).thenReturn(true);
        var defined = mock(com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity.class);
        when(defined.getTemplate()).thenReturn(template);
        when(defined.getApplicationId()).thenReturn(application);
        when(defined.getIsActive()).thenReturn(active);
        return defined;
    }

    @Test
    @DisplayName("by id: locates an active definition of the caller's application whose file exists, and renders it")
    void byId() throws Exception {
        Files.writeString(dir.resolve("zb-debt-due-tg-es.html"), "Deuda <b>${name?html}</b>");
        var app = UUID.randomUUID();
        var id = UUID.randomUUID();
        var repo = mock(com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository.class);
        var defined = definition(app, "zb-debt-due-tg-es.html", "ES", true);
        when(repo.findById(id)).thenReturn(Optional.of(defined));
        var folders = mock(com.umdc.mercury.template.ApplicationTemplateFolders.class);
        when(folders.nameOf(any(), anyString())).thenAnswer(i -> i.getArgument(1));
        var config = new Configuration(Configuration.VERSION_2_3_30);
        config.setDirectoryForTemplateLoading(dir.toFile());
        var byId = new TelegramTemplateRenderer(config, "", repo, folders);

        assertEquals(Optional.of(new TelegramTemplateRenderer.Located("zb-debt-due-tg-es.html", "es")), byId.locate(app.toString(), id));
        assertEquals("Deuda <b>A &amp; B</b>", byId.renderById(id, Map.of("name", "A & B")));
        assertTrue(byId.locate(UUID.randomUUID().toString(), id).isEmpty(), "another application's id looks like an unknown one");
        assertTrue(byId.locate(app.toString(), UUID.randomUUID()).isEmpty());
        assertTrue(byId.locate(app.toString(), null).isEmpty());
        assertThrows(IllegalStateException.class, () -> byId.renderById(UUID.randomUUID(), Map.of()));
    }

    @Test
    @DisplayName("by id: an inactive definition, or one whose file is missing, is not located")
    void byIdInactiveOrMissingFile() throws Exception {
        var app = UUID.randomUUID();
        var off = UUID.randomUUID();
        var gone = UUID.randomUUID();
        var repo = mock(com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository.class);
        var offDefinition = definition(app, "zb-debt-due-tg-es.html", "es", false);
        var goneDefinition = definition(app, "missing.html", "es", true);
        when(repo.findById(off)).thenReturn(Optional.of(offDefinition));
        when(repo.findById(gone)).thenReturn(Optional.of(goneDefinition));
        var folders = mock(com.umdc.mercury.template.ApplicationTemplateFolders.class);
        when(folders.nameOf(any(), anyString())).thenAnswer(i -> i.getArgument(1));
        var config = new Configuration(Configuration.VERSION_2_3_30);
        config.setDirectoryForTemplateLoading(dir.toFile());
        var byId = new TelegramTemplateRenderer(config, "", repo, folders);

        assertTrue(byId.locate(app.toString(), off).isEmpty());
        assertTrue(byId.locate(app.toString(), gone).isEmpty());
        assertTrue(renderer.locate(app.toString(), off).isEmpty(), "a renderer built for kinds only never locates by id");
        assertThrows(IllegalStateException.class, () -> renderer.renderById(off, Map.of()));
    }
}
