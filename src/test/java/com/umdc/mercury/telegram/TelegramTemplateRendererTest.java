package com.umdc.mercury.telegram;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
}
