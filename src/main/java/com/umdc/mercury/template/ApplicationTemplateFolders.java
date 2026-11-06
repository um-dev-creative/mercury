package com.umdc.mercury.template;

import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import com.umdc.mercury.template.storage.TemplateNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>The folder is the lower-cased {@code code_name} of the application ({@code general.application.code_name}), with
 * no mapping. When the application has no usable {@code code_name} the template is looked for at the root of the
 * storage, as before folders existed, and a warning says so.</p>
 */
@Component
public class ApplicationTemplateFolders {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationTemplateFolders.class);

    private final ApplicationRepository applications;
    private final Map<UUID, Optional<String>> resolved = new ConcurrentHashMap<>();

    public ApplicationTemplateFolders(ApplicationRepository applications) {
        this.applications = applications;
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
        Optional<String> folder = applications.findById(applicationId)
                .map(application -> application.getCodeName())
                .filter(Objects::nonNull)
                .map(code -> code.trim().toLowerCase(Locale.ROOT))
                .filter(TemplateNames::isFolder);
        if (folder.isEmpty()) {
            logger.warn("Application {} has no usable code_name to name its template folder: its templates are looked for at the root of the storage",
                    applicationId);
        }
        return folder;
    }
}
