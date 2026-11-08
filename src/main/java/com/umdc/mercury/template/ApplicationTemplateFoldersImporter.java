package com.umdc.mercury.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Registers at startup the folders listed in {@code umdc.templates.application-folders} (<code>&lt;application id&gt;=&lt;folder&gt;,...</code>)
 * that are not registered yet, so applications whose templates already live in a named folder keep working. It is optional and only
 * ever adds: a new application needs no entry, because Mercury assigns it a folder when its first template is created.
 */
@Component
public class ApplicationTemplateFoldersImporter implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationTemplateFoldersImporter.class);

    private final ApplicationTemplateFolders folders;
    private final String configured;

    public ApplicationTemplateFoldersImporter(ApplicationTemplateFolders folders,
                                              @Value("${umdc.templates.application-folders:}") String configured) {
        this.folders = folders;
        this.configured = configured;
    }

    @Override
    public void run(ApplicationArguments args) {
        var entries = ApplicationTemplateFolders.parse(configured);
        if (entries.isEmpty()) {
            return;
        }
        try {
            logger.info("Template folders: {} of {} configured application(s) registered", folders.importMissing(entries), entries.size());
        } catch (RuntimeException e) {
            // the table is created by Flyway (V2033); without it the folders are read as absent until it exists
            logger.error("Could not register the configured template folders: {}", e.getMessage());
        }
    }
}
