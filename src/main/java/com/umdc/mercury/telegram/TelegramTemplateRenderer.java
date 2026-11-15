package com.umdc.mercury.telegram;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import com.umdc.mercury.template.storage.TemplateNames;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.umdc.mercury.template.ApplicationTemplateFolders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
    private final String folder;

    private final TemplateDefinedRepository definitions;
    private final ApplicationTemplateFolders folders;

    /** A template found by its definition id: the file it is stored as and the language it is written in. */
    public record Located(String location, String language) { }

    /** @param folder application folder of the ZeroBound templates in the storage ({@code umdc.templates.telegram-folder}) */
    @Autowired
    public TelegramTemplateRenderer(@Qualifier("getFreeMarkerConfiguration") Configuration freemarker,
                                    @Value("${umdc.templates.telegram-folder:zeronbou}") String folder,
                                    TemplateDefinedRepository definitions, ApplicationTemplateFolders folders) {
        this.freemarker = freemarker;
        this.folder = folder;
        this.definitions = definitions;
        this.folders = folders;
    }

    /** By kind only (no lookup by definition id). */
    TelegramTemplateRenderer(Configuration freemarker, String folder) {
        this(freemarker, folder, null, null);
    }

    /**
     * The template of the definition {@code id} when it is active, belongs to {@code application} and its file exists; empty otherwise
     * (the caller answers the same 400 for all of them, so nothing says whether an id exists in another application).
     */
    public Optional<Located> locate(String application, UUID id) {
        if (Objects.isNull(definitions) || Objects.isNull(id)) {
            return Optional.empty();
        }
        return definitions.findById(id)
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()) && Objects.nonNull(d.getApplicationId()) && d.getApplicationId().toString().equals(application))
                .filter(d -> Objects.nonNull(d.getTemplate()) && !Boolean.FALSE.equals(d.getTemplate().getActive())
                        && Objects.nonNull(d.getTemplate().getLocation()) && Objects.nonNull(d.getTemplate().getLanguage()))
                .map(d -> new Located(storageName(d.getApplicationId(), d.getTemplate().getLocation()), d.getTemplate().getLanguage().strip().toLowerCase(java.util.Locale.ROOT)))
                .filter(l -> fileExists(l.location()));
    }

    private String storageName(UUID applicationId, String location) {
        return folders.nameOf(applicationId, location);
    }

    private boolean fileExists(String name) {
        try {
            freemarker.getTemplate(name);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** The message text of the definition {@code id}; throws {@link IllegalStateException} when it is gone or a parameter it needs is not there. */
    public String renderById(UUID id, Map<String, Object> params) {
        var definition = Objects.isNull(definitions) ? Optional.<com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity>empty() : definitions.findById(id);
        if (definition.isEmpty() || Objects.isNull(definition.get().getTemplate())) {
            throw new IllegalStateException("Telegram template could not be rendered: NotFound");
        }
        var d = definition.get();
        try (var out = new StringWriter()) {
            freemarker.getTemplate(storageName(d.getApplicationId(), d.getTemplate().getLocation())).process(params, out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Telegram template could not be rendered: " + e.getClass().getSimpleName(), e);
        }
    }

    String name(String kind, String language) {
        return TemplateNames.inFolder(folder, "zb_" + kind + "_tg_" + language + SUFFIX);
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
