package com.umdc.mercury.api.v1.service;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Iris mail templates (es/en)")
class IrisTemplatesTest {

    private String render(String language, Map<String, Object> params) throws IOException, TemplateException {
        var config = new Configuration(Configuration.VERSION_2_3_30);
        config.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
        return FreeMarkerTemplateUtils.processTemplateIntoString(config.getTemplate("iris_verification_" + language + ".ftl"), params);
    }

    @ParameterizedTest
    @ValueSource(strings = {"es", "en"})
    @DisplayName("renders the code, the name and the expiry the Iris BFF sends")
    void rendersWhatIrisSends(String language) throws IOException, TemplateException {
        String html = render(language, Map.of("vc", "482190", "user_name", "Ana Rojas", "expiresInMinutes", "15"));

        assertThat(html).contains("482190", "Ana Rojas", "15", "Iris").doesNotContainIgnoringCase("smartkin");
    }

    @ParameterizedTest
    @ValueSource(strings = {"es", "en"})
    @DisplayName("escapes the name, which comes from the sign-up form")
    void escapesTheName(String language) throws IOException, TemplateException {
        String html = render(language, Map.of("vc", "482190", "user_name", "<script>"));

        assertThat(html).doesNotContain("<script>");
    }
}
