package com.umdc.mercury.template;

import com.umdc.mercury.api.v1.exception.FolderInUseException;
import com.umdc.mercury.jpa.sql.entity.ApplicationTemplateFolderEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationTemplateFolderRepository;
import com.umdc.mercury.template.storage.TemplateNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells in which application folder of the template storage the templates of an application live
 * ({@code <prefix>/<folder>/<file>}, for example {@code lth/mercury/templates/iris/iris_welcome.ftl}) and manages those folders.
 *
 * <p>The folders are rows of Mercury's own table {@code mercury.application_template_folders}: Mercury never reads the {@code general}
 * schema (backbone-rest's), so it does not look an application up. They are read dynamically (cached for
 * {@code umdc.templates.application-folders-cache-seconds}, so a change made through the API or another instance applies without a
 * restart) and written dynamically: {@link #ensureFolder} gives an application that has none the folder named like its id, so a new
 * application needs no configuration, and {@link #assign} sets a friendlier name. An application with no row keeps its templates at
 * the root of the storage, as before folders existed.</p>
 */
@Component
public class ApplicationTemplateFolders {

    private static final int ENTRY_PARTS = 2;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationTemplateFolders.class);

    private final ApplicationTemplateFolderRepository repository;
    private final Clock clock;
    private final long cacheMillis;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    @Autowired
    public ApplicationTemplateFolders(ApplicationTemplateFolderRepository repository,
                                      @Value("${umdc.templates.application-folders-cache-seconds:60}") long cacheSeconds) {
        this(repository, cacheSeconds, Clock.systemUTC());
    }

    ApplicationTemplateFolders(ApplicationTemplateFolderRepository repository, long cacheSeconds, Clock clock) {
        this.repository = repository;
        this.cacheMillis = Math.max(0, cacheSeconds) * 1000;
        this.clock = clock;
    }

    /** @return the storage name of {@code file} for the application: {@code folder/file}, or {@code file} when it has no folder. */
    public String nameOf(UUID applicationId, String file) {
        if (Objects.isNull(file) || file.contains("/")) {
            return file;
        }
        return TemplateNames.inFolder(folderOf(applicationId).orElse(null), file);
    }

    /** @return the folder of the application, or empty when it has none (its templates are at the root of the storage). */
    public Optional<String> folderOf(UUID applicationId) {
        if (Objects.isNull(applicationId)) {
            return Optional.empty();
        }
        long now = clock.millis();
        Cached cached = cache.get(applicationId);
        if (Objects.nonNull(cached) && cached.expiresAt() > now) {
            return cached.folder();
        }
        Optional<String> folder = repository.findById(applicationId).map(ApplicationTemplateFolderEntity::getFolder);
        cache.put(applicationId, new Cached(folder, now + cacheMillis));
        return folder;
    }

    /**
     * Gives the application a folder named like its id when it has none, and returns its folder. Safe to call from several instances at
     * once: the one that loses the race reads the winner's row.
     */
    public String ensureFolder(UUID applicationId) {
        Optional<String> existing = readThrough(applicationId);
        if (existing.isPresent()) {
            return existing.get();
        }
        String folder = defaultFolder(applicationId);
        try {
            repository.saveAndFlush(new ApplicationTemplateFolderEntity(applicationId, folder, now()));
            logger.info("Application {} has no template folder: assigned '{}'", applicationId, folder);
        } catch (DataIntegrityViolationException e) {
            return readThrough(applicationId).orElseThrow(() -> new FolderInUseException(folder, e));
        } finally {
            cache.remove(applicationId);
        }
        return folder;
    }

    /**
     * Sets the folder of the application (creating or replacing its row). It does not move files: a template already saved in the old
     * folder must be copied to the new one.
     *
     * @throws IllegalArgumentException if {@code folder} is not a valid folder name
     * @throws FolderInUseException     if another application already uses it
     */
    public String assign(UUID applicationId, String folder) {
        String wanted = Objects.isNull(folder) ? "" : folder.trim();
        if (!TemplateNames.isFolder(wanted)) {
            throw new IllegalArgumentException("'" + wanted + "' is not a valid folder name (lower-case letters, digits, '_' and '-', up to 64)");
        }
        Optional<ApplicationTemplateFolderEntity> owner = repository.findByFolder(wanted);
        if (owner.isPresent() && !owner.get().getApplicationId().equals(applicationId)) {
            throw new FolderInUseException(wanted);
        }
        ApplicationTemplateFolderEntity row = repository.findById(applicationId).orElse(null);
        try {
            if (Objects.isNull(row)) {
                repository.saveAndFlush(new ApplicationTemplateFolderEntity(applicationId, wanted, now()));
            } else {
                row.setFolder(wanted, now());
                repository.saveAndFlush(row);
            }
        } catch (DataIntegrityViolationException e) {
            throw new FolderInUseException(wanted, e);
        } finally {
            cache.remove(applicationId);
        }
        return wanted;
    }

    /**
     * Registers the configured folders that are not registered yet (never overwrites a row, and skips a folder another application owns).
     *
     * @return how many were added
     */
    public int importMissing(Map<UUID, String> configured) {
        int added = 0;
        for (Map.Entry<UUID, String> entry : configured.entrySet()) {
            if (readThrough(entry.getKey()).isPresent()) {
                continue;
            }
            try {
                assign(entry.getKey(), entry.getValue());
                added++;
            } catch (FolderInUseException e) {
                logger.warn("Not registering the folder of application {}: {}", entry.getKey(), e.getMessage());
            }
        }
        return added;
    }

    /** The folder an application gets when none is chosen: its own id, which is unique and needs no lookup. */
    public static String defaultFolder(UUID applicationId) {
        return applicationId.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private Optional<String> readThrough(UUID applicationId) {
        cache.remove(applicationId);
        return folderOf(applicationId);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
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

    private record Cached(Optional<String> folder, long expiresAt) {
    }
}
