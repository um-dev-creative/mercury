package com.umdc.mercury.ingress;

import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Finds a template by name and language inside an application (decision D-15), so a caller never keeps mercury's template ids. An active definition
 * of an active template whose {@code language} column equals the one asked and whose name equals the one asked. There is <b>no fallback</b> to another
 * language: a message is refused rather than sent in a language nobody asked for.
 */
@Component
public class TemplateNameResolver {

    private final TemplateDefinedRepository definitions;

    public TemplateNameResolver(TemplateDefinedRepository definitions) {
        this.definitions = definitions;
    }

    /** What the application offers: template name and its languages. */
    public record Entry(String template, List<String> languages) {
    }

    /**
     * @return the id of the definition to queue the message with
     * @throws IllegalArgumentException the template does not exist for the application or has no version in that language (400); the text lists the languages it has
     */
    public UUID resolve(UUID applicationId, String template, String language) {
        List<TemplateDefinedEntity> named = usable(applicationId).stream()
                .filter(d -> TemplateIdentity.nameOf(d.getTemplate().getLocation(), d.getTemplate().getLanguage()).equals(template))
                .toList();
        if (named.isEmpty()) {
            throw new IllegalArgumentException("The template '" + template + "' is not registered for this application");
        }
        return named.stream()
                .filter(d -> language.equalsIgnoreCase(d.getTemplate().getLanguage()))
                .max(Comparator.comparing(TemplateNameResolver::created))
                .map(TemplateDefinedEntity::getId)
                .orElseThrow(() -> new IllegalArgumentException("The template '" + template + "' has no '" + language + "' version (available: "
                        + String.join(", ", named.stream().map(d -> d.getTemplate().getLanguage()).distinct().sorted().toList()) + ")"));
    }

    /** The templates the application can be asked for, sorted by name, each with its languages. */
    public List<Entry> catalogue(UUID applicationId) {
        var byName = new TreeMap<String, TreeSet<String>>();
        for (TemplateDefinedEntity d : usable(applicationId)) {
            byName.computeIfAbsent(TemplateIdentity.nameOf(d.getTemplate().getLocation(), d.getTemplate().getLanguage()), k -> new TreeSet<>())
                    .add(d.getTemplate().getLanguage());
        }
        return byName.entrySet().stream().map(e -> new Entry(e.getKey(), List.copyOf(e.getValue()))).toList();
    }

    private List<TemplateDefinedEntity> usable(UUID applicationId) {
        return definitions.findByApplicationIdAndIsActiveTrue(applicationId).stream()
                .filter(d -> Objects.nonNull(d.getTemplate()) && !Boolean.FALSE.equals(d.getTemplate().getActive())
                        && Objects.nonNull(d.getTemplate().getLanguage()) && !d.getTemplate().getLanguage().isBlank())
                .toList();
    }

    private static LocalDateTime created(TemplateDefinedEntity d) {
        return Objects.requireNonNullElse(d.getCreatedAt(), LocalDateTime.MIN);
    }
}
