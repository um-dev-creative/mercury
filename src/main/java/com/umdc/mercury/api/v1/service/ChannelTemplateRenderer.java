package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Renders the body of a non-email channel message from its campaign template.
 *
 * <p>Email renders its template inside {@link EmailServiceImpl}; SMS, Telegram, WhatsApp and
 * Push had no equivalent and used the raw {@code message} parameter instead, so the
 * {@code templateId} chosen when creating the campaign was effectively ignored for them.
 * This component closes that gap using the same FreeMarker configuration.</p>
 */
@Component
public class ChannelTemplateRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ChannelTemplateRenderer.class);

    private final Configuration freemarkerConfig;

    public ChannelTemplateRenderer(@Qualifier("getFreeMarkerConfiguration") Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }

    /**
     * Renders {@code template} with {@code params}.
     *
     * <p>Falls back to {@code rawBody} — the literal text the campaign was created with — when
     * the template is missing or cannot be rendered, so a misconfigured template degrades the
     * message instead of dropping it. Every fallback is logged at WARN.</p>
     *
     * @param template the resolved campaign template; may be {@code null}
     * @param params   template parameters supplied by the campaign
     * @param rawBody  the body to use when the template cannot be rendered
     * @return the rendered body, or {@code rawBody} when rendering is not possible
     */
    public String render(TemplateDefinedTO template, Map<String, Object> params, String rawBody) {
        String location = locationOf(template);
        if (Objects.isNull(location)) {
            logger.warn("No template location available; falling back to the raw message body.");
            return rawBody;
        }
        try {
            return FreeMarkerTemplateUtils.processTemplateIntoString(
                    freemarkerConfig.getTemplate(location),
                    Objects.isNull(params) ? Map.of() : params);
        } catch (IOException | TemplateException e) {
            logger.warn("Unable to render template '{}'; falling back to the raw message body.", location, e);
            return rawBody;
        }
    }

    private String locationOf(TemplateDefinedTO template) {
        if (Objects.isNull(template) || Objects.isNull(template.template())) {
            return null;
        }
        String location = template.template().location();
        return Objects.isNull(location) || location.isBlank() ? null : location;
    }
}
