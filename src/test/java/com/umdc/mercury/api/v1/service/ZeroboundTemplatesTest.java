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

    /** kind -> the params docs/zerobound-mail-templates.md documents for it. */
    private static final Map<String, Map<String, Object>> PARAMS = Map.of(
            "debt_due", Map.of("debtName", "Visa <b>", "dueDate", "2026-11-05", "amount", "≈ 300"),
            "budget_exceeded", Map.of("categoryName", "Food", "period", "2026-10", "spent", "≈ 600", "limit", "≈ 500", "percent", "120"),
            "goal_reached", Map.of("goalName", "Emergency fund", "target", "≈ 5000"),
            "critical_insight", Map.of("title", "Low emergency fund", "summary", "Covers 1.2 months"),
            "price_alert", Map.of("symbol", "AAPL", "direction", "rose above", "threshold", "200", "price", "201.5"),
            "weekly_summary", Map.of("periodStart", "2026-10-10", "periodEnd", "2026-10-16", "income", "≈ 900", "spending", "≈ 700", "net", "≈ 200"),
            "verification", Map.of("vc", "483920", "expiresInHours", "24"));

    private final Configuration config = configuration();

    private static Configuration configuration() {
        var c = new Configuration(Configuration.VERSION_2_3_30);
        try {
            c.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
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
    @DisplayName("there are 14 templates: 7 kinds x 2 languages")
    void fourteenFiles() {
        var files = new File("src/main/resources/templates").list((d, n) -> n.startsWith("zb_") && n.endsWith(".ftl"));
        assertThat(files).hasSize(14);
    }
}
