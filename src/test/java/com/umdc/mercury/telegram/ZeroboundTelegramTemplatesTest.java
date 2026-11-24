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
            "weekly_summary", Map.of("periodStart", "2026-10-10", "periodEnd", "2026-10-16", "income", "≈ 900", "spending", "≈ 700", "net", "≈ 200"),
            "economic_event", Map.of("phase", "released", "title", "CPI <m/m>", "country", "US", "scheduledAt", "2026-11-25T13:30Z",
                    "actual", "0.4 %", "forecast", "0.3 %", "previous", "0.2 %"));

    private final TelegramTemplateRenderer renderer = renderer();

    private static TelegramTemplateRenderer renderer() {
        final var config = new Configuration(Configuration.VERSION_2_3_30);
        try {
            config.setDirectoryForTemplateLoading(new File("src/test/resources/templates"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new TelegramTemplateRenderer(config, "");
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
    @DisplayName("economic_event: both phases render, the figure shows only once released, and a missing phase fails")
    void economicEventPhases() {
        for (String lang : List.of("es", "en")) {
            final var base = new java.util.HashMap<String, Object>(PARAMS.get("economic_event"));
            final String released = renderer.render("economic_event", lang, base);
            base.put("phase", "upcoming");
            base.put("actual", "-");
            final String upcoming = renderer.render("economic_event", lang, base);
            assertTrue(released.contains("0.4 %"));
            assertFalse(upcoming.contains("0.4 %"));
            assertTrue(upcoming.contains("CPI &lt;m/m&gt;"));
            assertEquals(1, TelegramTextSplitter.split(upcoming).size());
            base.remove("phase");
            assertThrows(IllegalStateException.class, () -> renderer.render("economic_event", lang, base));
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
    @DisplayName("there are 14 Telegram templates: 7 kinds x 2 languages; verification codes are not sent through Telegram")
    void fourteenFiles() {
        final var files = new File("src/test/resources/templates").list((d, n) -> n.startsWith("zb_") && n.contains("_tg_"));
        assertEquals(14, files == null ? 0 : files.length);
        assertFalse(renderer.exists("verification", "es"));
    }
}
