package com.umdc.mercury.template;

import com.umdc.mercury.template.storage.TemplateNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells in which application folder of the template storage the templates of an application live
 * ({@code <prefix>/<folder>/<file>}, for example {@code lth/mercury/templates/iris/iris_welcome.ftl}).
 *
 * <p>The folder comes only from {@code umdc.templates.application-folders} ({@code <application id>=<folder>,...}).
 * Mercury never reads the {@code general} schema, which belongs to backbone-rest, so the code of an application is not
 * looked up: its id is configured here. An application with no entry is looked for at the root of the storage, as
 * before folders existed, and a warning says so.</p>
 */
@Component
public class ApplicationTemplateFolders {

    private static final int ENTRY_PARTS = 2;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationTemplateFolders.class);

    private final Map<UUID, String> configured;
    private final Set<UUID> warned = ConcurrentHashMap.newKeySet();

    public ApplicationTemplateFolders(@Value("${umdc.templates.application-folders:}") String configured) {
        this.configured = parse(configured);
    }

    /** @return the storage name of {@code file} for the application: {@code folder/file}, or {@code file} when it has no folder. */
    public String nameOf(UUID applicationId, String file) {
        if (Objects.isNull(file) || file.contains("/")) {
            return file;
        }
        return TemplateNames.inFolder(folderOf(applicationId).orElse(null), file);
    }

    /** @return the folder configured for the application, if it has one. */
    public Optional<String> folderOf(UUID applicationId) {
        if (Objects.isNull(applicationId)) {
            return Optional.empty();
        }
        String folder = configured.get(applicationId);
        if (Objects.isNull(folder) && warned.add(applicationId)) {
            logger.warn("Application {} has no entry in umdc.templates.application-folders: its templates are looked for at the root of the storage",
                    applicationId);
        }
        return Optional.ofNullable(folder);
    }

    static Map<UUID, String> parse(String value) {
        Map<UUID, String> map = new ConcurrentHashMap<>();
        if (Objects.isNull(value) || value.isBlank()) {
            return map;
        }
        for (String entry : value.split(",")) {
            String[] parts = entry.trim().split("=", ENTRY_PARTS);
            if (parts.length != ENTRY_PARTS) {
                throw new IllegalArgumentException("umdc.templates.application-folders: expected <application id>=<folder>, got '" + entry.trim() + "'");
            }
            String folder = parts[1].trim();
            if (!TemplateNames.isFolder(folder)) {
                throw new IllegalArgumentException("umdc.templates.application-folders: '" + folder + "' is not a valid folder name");
            }
            map.put(applicationId(parts[0], entry), folder);
        }
        return map;
    }

    private static UUID applicationId(String owner, String entry) {
        try {
            return UUID.fromString(owner.trim());
        } catch (IllegalArgumentException _) {
            throw new IllegalArgumentException("umdc.templates.application-folders: '" + owner.trim() + "' is not an application id (a UUID) in '"
                    + entry.trim() + "'; Mercury does not read the application code from the database, so use the id");
        }
    }
}
