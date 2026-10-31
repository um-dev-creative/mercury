package com.umdc.mercury.telegram;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Renders {@code zb_<kind>_tg_<language>.ftl} (T-115/T-116) from mercury's template folder. The name is built
 * from a validated {@code kind} and a configured language, so a caller cannot reach another file. Output is
 * Telegram's HTML subset; templates escape their parameters with {@code ?html}.
 */
@Component
public class TelegramTemplateRenderer {

    static final Pattern KIND = Pattern.compile("[a-z][a-z0-9_]{0,49}");
    private static final String SUFFIX = ".ftl";

    private final Configuration freemarker;

    public TelegramTemplateRenderer(@Qualifier("getFreeMarkerConfiguration") Configuration freemarker) {
        this.freemarker = freemarker;
    }

    static String name(String kind, String language) {
        return "zb_" + kind + "_tg_" + language + SUFFIX;
    }

    public boolean exists(String kind, String language) {
        if (!KIND.matcher(kind).matches()) {
            return false;
        }
        try {
            freemarker.getTemplate(name(kind, language));
            return true;
        } catch (TemplateNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** The message text; throws {@link IllegalStateException} when the template is missing or a parameter it needs is not there. */
    public String render(String kind, String language, Map<String, Object> params) {
        try (var out = new StringWriter()) {
            freemarker.getTemplate(name(kind, language)).process(params, out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Telegram template could not be rendered: " + e.getClass().getSimpleName(), e);
        }
    }
}
