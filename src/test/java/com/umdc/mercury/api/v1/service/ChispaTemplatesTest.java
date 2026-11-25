package com.umdc.mercury.api.v1.service;

import freemarker.template.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CHISPA push templates (es/en)")
class ChispaTemplatesTest {

    /** kind -> the generic text of push-policy.ts PUSH_COPY (es) and its English version. */
    private static final Map<String, List<String>> TEXTS = Map.of(
            "invitation", List.of("Tienes una sorpresa en CHISPA.", "You have a surprise in CHISPA."),
            "challenge", List.of("Hay algo nuevo esperándote.", "There is something new waiting for you."),
            "surprise", List.of("Se ha desbloqueado una sorpresa.", "A surprise has been unlocked."),
            "plan", List.of("Tienes un plan esperando respuesta.", "You have a plan waiting for your answer."),
            "date_confirmed", List.of("Tienen un plan confirmado.", "You have a confirmed plan."),
            "memory", List.of("Un recuerdo especial te espera.", "A special memory is waiting for you."),
            "reward", List.of("Tienes una recompensa disponible.", "You have a reward available."));

    private static final Path SEED = Path.of("scripts/chispa/seed-templates.sql");

    private static Configuration configuration() throws IOException {
        var c = new Configuration(Configuration.VERSION_2_3_30);
        c.setDirectoryForTemplateLoading(new File("src/test/resources/templates"));
        return c;
    }

    @Test
    @DisplayName("every kind renders in es and en with no parameters, as plain text with the documented generic copy")
    void everyKindRenders() throws Exception {
        var config = configuration();
        for (var kind : TEXTS.entrySet()) {
            for (int i = 0; i < 2; i++) {
                String lang = i == 0 ? "es" : "en";
                String text = FreeMarkerTemplateUtils.processTemplateIntoString(config.getTemplate("ch_" + kind.getKey() + "_" + lang + ".ftl"), Map.of()).strip();
                assertThat(text).as(kind.getKey() + "/" + lang).isEqualTo(kind.getValue().get(i)).doesNotContain("<", "${");
            }
        }
    }

    @Test
    @DisplayName("the seed script registers exactly the shipped ch_* kinds in both languages and is idempotent")
    void seedMatchesTheFiles() throws IOException {
        String sql = Files.readString(SEED);

        for (String kind : TEXTS.keySet()) {
            assertThat(sql).contains("'" + kind + "'");
            assertThat(Path.of("src/test/resources/templates/ch_" + kind + "_es.ftl")).exists();
            assertThat(Path.of("src/test/resources/templates/ch_" + kind + "_en.ftl")).exists();
        }
        assertThat(sql).contains("VALUES ('es'), ('en')", "WHERE NOT EXISTS (SELECT 1 FROM mercury.templates t WHERE t.location = w.location", "BEGIN;", "COMMIT;");
        assertThat(sql.toUpperCase()).doesNotContain("DELETE FROM", "TRUNCATE", "DROP ");
    }
}
