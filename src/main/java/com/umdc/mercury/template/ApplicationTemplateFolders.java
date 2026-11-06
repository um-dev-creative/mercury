package com.umdc.mercury.template;

import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import com.umdc.mercury.template.storage.TemplateNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells in which application folder of the template storage the templates of an application live
 * ({@code <prefix>/<folder>/<file>}, for example {@code lth/mercury/templates/iris/iris_welcome.ftl}).
 *
 * <p>The folder is, in this order: the one configured for the application id in
 * {@code umdc.templates.application-folders} ({@code <application id or code_name>=<folder>,...}), the one configured
 * for its {@code code_name} (case-insensitive), or the lower-cased {@code code_name} itself. When none gives a valid
 * folder the template is looked for at the root of the storage, as before folders existed, and a warning says so.</p>
 */
@Component
public class ApplicationTemplateFolders {

    private static final int ENTRY_PARTS = 2;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationTemplateFolders.class);

    private final ApplicationRepository applications;
    private final Map<String, String> configured;
    private final Map<UUID, Optional<String>> resolved = new ConcurrentHashMap<>();

    public ApplicationTemplateFolders(ApplicationRepository applications,
                                      @Value("${umdc.templates.application-folders:}") String configured) {
        this.applications = applications;
        this.configured = parse(configured);
    }

    /** @return the storage name of {@code file} for the application: {@code folder/file}, or {@code file} when it has no folder. */
    public String nameOf(UUID applicationId, String file) {
        if (Objects.isNull(file) || file.contains("/")) {
            return file;
        }
        return TemplateNames.inFolder(folderOf(applicationId).orElse(null), file);
    }

    /** @return the folder of the application, if it has one. */
    public Optional<String> folderOf(UUID applicationId) {
        if (Objects.isNull(applicationId)) {
            return Optional.empty();
        }
        Optional<String> folder = resolved.get(applicationId);
        if (Objects.isNull(folder)) {
            folder = lookUp(applicationId);
            if (folder.isPresent()) {
                // a missing folder is not cached: the application or its code may be created afterwards
                resolved.put(applicationId, folder);
            }
        }
        return folder;
    }

    private Optional<String> lookUp(UUID applicationId) {
        String explicit = configured.get(key(applicationId.toString()));
        if (Objects.nonNull(explicit)) {
            return Optional.of(explicit);
        }
        Optional<String> code = applications.findById(applicationId)
                .map(application -> application.getCodeName())
                .filter(Objects::nonNull)
                .map(value -> key(value));
        Optional<String> fromCode = code.map(value -> configured.getOrDefault(value, value)).filter(TemplateNames::isFolder);
        if (fromCode.isEmpty()) {
            logger.warn("Application {} has no template folder (no entry in umdc.templates.application-folders and no usable code_name): "
                    + "its templates are looked for at the root of the storage", applicationId);
        }
        return fromCode;
    }

    private static String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    static Map<String, String> parse(String value) {
        Map<String, String> map = new ConcurrentHashMap<>();
        if (Objects.isNull(value) || value.isBlank()) {
            return map;
        }
        for (String entry : value.split(",")) {
            String[] parts = entry.trim().split("=", ENTRY_PARTS);
            if (parts.length != ENTRY_PARTS) {
                throw new IllegalArgumentException("umdc.templates.application-folders: expected <application id or code_name>=<folder>, got '" + entry.trim() + "'");
            }
            String folder = parts[1].trim();
            if (!TemplateNames.isFolder(folder)) {
                throw new IllegalArgumentException("umdc.templates.application-folders: '" + folder + "' is not a valid folder name");
            }
            String owner = key(parts[0]);
            if (owner.isEmpty()) {
                throw new IllegalArgumentException("umdc.templates.application-folders: missing application id or code_name in '" + entry.trim() + "'");
            }
            map.put(owner, folder);
        }
        return map;
    }
}
