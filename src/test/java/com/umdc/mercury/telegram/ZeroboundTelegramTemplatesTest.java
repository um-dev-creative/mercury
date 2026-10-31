package com.umdc.mercury.telegram;

import freemarker.template.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ZeroBound Telegram templates (es/en)")
class ZeroboundTelegramTemplatesTest {

    /** kind -> the params docs/zerobound-mail-templates.md documents for it (the same as the mail templates). */
    private static final Map<String, Map<String, Object>> PARAMS = Map.of(
            "debt_due", Map.of("debtName", "Visa <b>", "dueDate", "2026-11-05", "amount", "≈ 300"),
            "budget_exceeded", Map.of("categoryName", "Food", "period", "2026-10", "spent", "≈ 600", "limit", "≈ 500", "percent", "120"),
            "goal_reached", Map.of("goalName", "Emergency fund", "target", "≈ 5000"),
            "critical_insight", Map.of("title", "Low emergency fund", "summary", "Covers 1.2 months"),
            "price_alert", Map.of("symbol", "AAPL", "direction", "rose above", "threshold", "200", "price", "201.5"),
            "weekly_summary", Map.of("periodStart", "2026-10-10", "periodEnd", "2026-10-16", "income", "≈ 900", "spending", "≈ 700", "net", "≈ 200"));

    private final TelegramTemplateRenderer renderer = renderer();

    private static TelegramTemplateRenderer renderer() {
        final var config = new Configuration(Configuration.VERSION_2_3_30);
        try {
            config.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new TelegramTemplateRenderer(config);
    }

    @Test
    @DisplayName("every kind renders in both languages, fits one Telegram message and leaves no placeholder")
    void everyTemplateRenders() {
        for (var kind : PARAMS.entrySet()) {
            for (String lang : List.of("es", "en")) {
                assertTrue(renderer.exists(kind.getKey(), lang), kind.getKey() + "/" + lang);
                final String text = renderer.render(kind.getKey(), lang, kind.getValue());
                assertFalse(text.contains("${"), kind.getKey() + "/" + lang);
                assertFalse(text.contains("<#"), kind.getKey() + "/" + lang);
                assertEquals(1, TelegramTextSplitter.split(text).size(), kind.getKey() + "/" + lang);
            }
        }
    }

    @Test
    @DisplayName("values are HTML-escaped (Telegram would reject a raw < in the text) and masked amounts print as sent")
    void escapes() {
        final String text = renderer.render("debt_due", "es", PARAMS.get("debt_due"));
        assertTrue(text.contains("Visa &lt;b&gt;"));
        assertFalse(text.contains("Visa <b>"));
        assertTrue(text.contains("≈ 300"));
    }

    @Test
    @DisplayName("a missing parameter fails instead of sending a half-empty message")
    void missingParameter() {
        assertThrows(IllegalStateException.class, () -> renderer.render("goal_reached", "en", Map.of()));
    }

    @Test
    @DisplayName("there are 12 Telegram templates: 6 kinds x 2 languages; verification codes are not sent through Telegram")
    void twelveFiles() {
        final var files = new File("src/main/resources/templates").list((d, n) -> n.startsWith("zb_") && n.contains("_tg_"));
        assertEquals(12, files == null ? 0 : files.length);
        assertFalse(renderer.exists("verification", "es"));
    }
}
