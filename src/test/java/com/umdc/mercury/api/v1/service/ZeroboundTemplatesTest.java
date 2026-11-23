package com.umdc.mercury.api.v1.service;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ZeroBound mail templates (es/en)")
class ZeroboundTemplatesTest {

    private static final Map<String, Object> ECON_UPCOMING = Map.of("phase", "upcoming", "title", "CPI <m/m>", "country", "US",
            "scheduledAt", "2026-11-25T13:30Z", "actual", "-", "forecast", "0.3 %", "previous", "0.2 %");
    private static final Map<String, Object> ECON_RELEASED = Map.of("phase", "released", "title", "CPI <m/m>", "country", "US",
            "scheduledAt", "2026-11-25T13:30Z", "actual", "0.4 %", "forecast", "0.3 %", "previous", "0.2 %");

    /** kind -> the params docs/zerobound-mail-templates.md documents for it. */
    private static final Map<String, Map<String, Object>> PARAMS = Map.of(
            "debt_due", Map.of("debtName", "Visa <b>", "dueDate", "2026-11-05", "amount", "≈ 300"),
            "budget_exceeded", Map.of("categoryName", "Food", "period", "2026-10", "spent", "≈ 600", "limit", "≈ 500", "percent", "120"),
            "goal_reached", Map.of("goalName", "Emergency fund", "target", "≈ 5000"),
            "critical_insight", Map.of("title", "Low emergency fund", "summary", "Covers 1.2 months"),
            "price_alert", Map.of("symbol", "AAPL", "direction", "rose above", "threshold", "200", "price", "201.5"),
            "weekly_summary", Map.of("periodStart", "2026-10-10", "periodEnd", "2026-10-16", "income", "≈ 900", "spending", "≈ 700", "net", "≈ 200"),
            "verification", Map.of("vc", "483920", "expiresInMinutes", "15"),
            "economic_event", ECON_UPCOMING);

    private final Configuration config = configuration();

    private static Configuration configuration() {
        var c = new Configuration(Configuration.VERSION_2_3_30);
        try {
            c.setDirectoryForTemplateLoading(new File("src/test/resources/templates"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return c;
    }

    @Test
    @DisplayName("every kind renders in both languages with exactly its documented params")
    void everyTemplateRenders() throws IOException, TemplateException {
        for (var kind : PARAMS.entrySet()) {
            for (String lang : List.of("es", "en")) {
                String html = FreeMarkerTemplateUtils.processTemplateIntoString(
                        config.getTemplate("zb_" + kind.getKey() + "_" + lang + ".ftl"), kind.getValue());
                assertThat(html).as(kind.getKey() + "/" + lang).contains("lang=\"" + lang + "\"").doesNotContain("${");
            }
        }
    }

    @Test
    @DisplayName("economic_event renders both phases in both languages, escaped, with the disclaimer and no unresolved variable")
    void economicEventPhases() throws IOException, TemplateException {
        for (String lang : List.of("es", "en")) {
            String upcoming = FreeMarkerTemplateUtils.processTemplateIntoString(config.getTemplate("zb_economic_event_" + lang + ".ftl"), ECON_UPCOMING);
            String released = FreeMarkerTemplateUtils.processTemplateIntoString(config.getTemplate("zb_economic_event_" + lang + ".ftl"), ECON_RELEASED);
            assertThat(upcoming).contains("CPI &lt;m/m&gt;").doesNotContain("${").contains("0.3 %").doesNotContain("0.4 %");
            assertThat(released).contains("0.4 %").contains("0.3 %").contains("0.2 %").doesNotContain("${");
            assertThat(upcoming).isNotEqualTo(released);
            assertThat(released).containsIgnoringCase(lang.equals("es") ? "no constituye asesoría financiera" : "not financial advice");
        }
    }

    @Test
    @DisplayName("economic_event without a phase fails instead of guessing")
    void economicEventNeedsPhase() {
        org.junit.jupiter.api.Assertions.assertThrows(TemplateException.class, () -> FreeMarkerTemplateUtils.processTemplateIntoString(
                config.getTemplate("zb_economic_event_en.ftl"), Map.of("title", "x", "country", "US", "scheduledAt", "t", "actual", "-", "forecast", "-", "previous", "-")));
    }

    @Test
    @DisplayName("values are HTML-escaped and amounts print as the sender masked them")
    void escapesAndKeepsMasks() throws IOException, TemplateException {
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(
                config.getTemplate("zb_debt_due_es.ftl"), PARAMS.get("debt_due"));

        assertThat(html).contains("Visa &lt;b&gt;").doesNotContain("Visa <b>").contains("≈ 300");
    }

    @Test
    @DisplayName("a missing parameter fails loudly instead of sending a half-empty mail")
    void missingParameterFails() {
        org.junit.jupiter.api.Assertions.assertThrows(TemplateException.class, () ->
                FreeMarkerTemplateUtils.processTemplateIntoString(config.getTemplate("zb_goal_reached_en.ftl"), Map.of()));
    }

    @Test
    @DisplayName("there are 16 mail templates: 8 kinds x 2 languages (the Telegram ones are _tg_)")
    void sixteenFiles() {
        var files = new File("src/test/resources/templates").list((d, n) -> n.startsWith("zb_") && n.endsWith(".ftl") && !n.contains("_tg_"));
        assertThat(files).hasSize(16);
    }
}
