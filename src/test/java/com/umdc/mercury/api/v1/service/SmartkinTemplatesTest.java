package com.umdc.mercury.api.v1.service;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SmartKin mail templates (es/en)")
class SmartkinTemplatesTest {

    /** What smartkin-backend's UserServiceImpl.toEmailMessageTO puts in `params`: the code as ####-#### and the full name. */
    private static final Map<String, Object> SMARTKIN_PARAMS = Map.of("vc", "4821-9035", "user_name", "Ana Pérez");

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

    private String render(String lang, Map<String, Object> params) throws IOException, TemplateException {
        return FreeMarkerTemplateUtils.processTemplateIntoString(config.getTemplate("sk_verification_" + lang + ".ftl"), params);
    }

    @Test
    @DisplayName("renders in both languages with exactly the params smartkin-backend sends")
    void rendersWithWhatSmartkinSends() throws IOException, TemplateException {
        for (String lang : List.of("es", "en")) {
            String html = render(lang, SMARTKIN_PARAMS);

            assertThat(html).as(lang).contains("lang=\"" + lang + "\"").contains("4821-9035").contains("Ana Pérez")
                    .contains("SmartKin").doesNotContain("${").doesNotContain("expiresInMinutes");
        }
    }

    @Test
    @DisplayName("without expiresInMinutes it does not promise a duration, with it, it says how long")
    void expiryIsOptional() throws IOException, TemplateException {
        Map<String, Object> withExpiry = new HashMap<>(SMARTKIN_PARAMS);
        withExpiry.put("expiresInMinutes", "30");

        assertThat(render("es", SMARTKIN_PARAMS)).doesNotContain("minutos").contains("vigencia limitada");
        assertThat(render("en", SMARTKIN_PARAMS)).doesNotContain("minutes").contains("limited time");
        assertThat(render("es", withExpiry)).contains("30 minutos");
        assertThat(render("en", withExpiry)).contains("30 minutes");
    }

    @Test
    @DisplayName("values are HTML-escaped: a name cannot inject markup into the mail")
    void escapesValues() throws IOException, TemplateException {
        String html = render("es", Map.of("vc", "1111-2222", "user_name", "<script>alert(1)</script> & Co"));

        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; Co").doesNotContain("<script>");
    }

    @Test
    @DisplayName("a missing code or name fails loudly instead of sending a half-empty mail")
    void missingParameterFails() {
        for (String lang : List.of("es", "en")) {
            assertThrows(TemplateException.class, () -> render(lang, Map.of("user_name", "Ana")));
            assertThrows(TemplateException.class, () -> render(lang, Map.of("vc", "4821-9035")));
        }
    }

    @Test
    @DisplayName("there are exactly 2 SmartKin templates, and the seed script registers exactly those files")
    void filesAndSeedAgree() throws IOException {
        var files = new File("src/main/resources/templates").list((d, n) -> n.startsWith("sk_") && n.endsWith(".ftl"));
        String seed = Files.readString(Path.of("scripts/smartkin/seed-templates.sql"));

        assertThat(files).containsExactlyInAnyOrder("sk_verification_es.ftl", "sk_verification_en.ftl");
        for (String file : files) {
            assertThat(seed).as("seed registers " + file).contains("'" + file + "'");
        }
    }

    @Test
    @DisplayName("the bootstrap script prints exactly the ids the seed script takes, and creates every delivery status")
    void bootstrapAndSeedAgree() throws IOException {
        String bootstrap = Files.readString(Path.of("scripts/smartkin/bootstrap-reference-data.sql"));
        String seed = Files.readString(Path.of("scripts/smartkin/seed-templates.sql"));

        for (String variable : List.of("verification_type_id", "severity_id", "frequency_id")) {
            assertThat(bootstrap).as("bootstrap prints " + variable).contains("'" + variable + "'");
            assertThat(seed).as("seed takes " + variable).contains("-v " + variable + "=");
        }
        for (var status : com.umdc.mercury.constant.DeliveryStatusType.values()) {
            assertThat(bootstrap).as("bootstrap registers the status " + status).contains("'" + status.name() + "'");
        }
    }
}
