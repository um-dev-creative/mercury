package com.umdc.mercury.ingress;

import com.umdc.mercury.config.FreeMarkerConfig;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Objects;

/**
 * Renders the subject a template defines (edited from Iris) with the message's parameters, so a subject can quote a parameter (for example a verification code) with the usual FreeMarker placeholder.
 * Uses the same hardened engine as the bodies ({@code ?new} and {@code ?api} refused). The result is one plain-text line: line breaks are removed
 * so a parameter can never inject a header.
 */
@Component
public class SubjectRenderer {

    private final freemarker.template.Configuration config;

    public SubjectRenderer() {
        this.config = FreeMarkerConfig.hardened(new StringTemplateLoader());
    }

    /**
     * @param subject the template's subject text
     * @param params  the message parameters (may be {@code null})
     * @return the rendered, single-line, trimmed subject
     * @throws IllegalArgumentException the subject is not valid FreeMarker or uses a parameter the message did not send (400)
     */
    public String render(String subject, Map<String, Object> params) {
        try (var reader = new StringReader(subject)) {
            var template = new Template("subject", reader, config);
            String text = FreeMarkerTemplateUtils.processTemplateIntoString(template, Objects.requireNonNullElse(params, Map.of()));
            return text.replaceAll("[\\r\\n]+", " ").strip();
        } catch (IOException | TemplateException e) {
            throw new IllegalArgumentException("The subject of the template cannot be rendered: " + e.getMessage().lines().findFirst().orElse("invalid"), e);
        }
    }
}
