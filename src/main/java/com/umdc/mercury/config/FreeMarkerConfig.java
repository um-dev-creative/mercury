package com.umdc.mercury.config;

import com.umdc.mercury.template.storage.LocalTemplateStorage;
import com.umdc.mercury.template.storage.StorageTemplateLoader;
import com.umdc.mercury.template.storage.TemplateStorage;
import com.umdc.mercury.template.markdown.MarkdownDirective;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration class for FreeMarker template engine.
 * Provides setup and configuration for FreeMarker templates used in email generation.
 *
 * @version 1.0.0
 * @since 11
 */
@Configuration
public class FreeMarkerConfig {
    /**
     * Path to the directory containing FreeMarker templates.
     * Value is injected from application properties.
     */
    @Value("${spring.freemarker.template-loader-path}")
    private String templateLoaderPath;

    /**
     * Where the content of the templates lives: the folder at {@code spring.freemarker.template-loader-path}. Every
     * reader and writer of template content goes through this bean.
     *
     * @return the template storage
     */
    @Bean
    public TemplateStorage templateStorage() {
        return new LocalTemplateStorage(Path.of(templateLoaderPath));
    }

    /**
     * Creates and configures a FreeMarker Configuration instance.
     * Sets up template loading from the configured template directory.
     *
     * @return A configured FreeMarker Configuration object
     * @throws IOException If there is an error accessing the template directory
     */
    @Bean
    public freemarker.template.Configuration getFreeMarkerConfiguration() throws IOException {
        if (!Files.isDirectory(Path.of(templateLoaderPath))) {
            throw new IOException("Template directory does not exist: " + templateLoaderPath);
        }
        return hardened(new StorageTemplateLoader(templateStorage()));
    }

    /**
     * A FreeMarker configuration with the engine restricted to what a message needs, over {@code loader}. The editor's
     * syntax check and preview build theirs with this too, so they refuse what production refuses.
     *
     * @param loader where the templates are read from
     * @return the configuration
     */
    public static freemarker.template.Configuration hardened(TemplateLoader loader) {
        freemarker.template.Configuration config = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_30);
        config.setTemplateLoader(loader);
        // A template is code. Once people can edit them, a template must not be able to instantiate arbitrary classes
        // (`?new` with freemarker.template.utility.Execute or ObjectConstructor runs commands on the server) nor reach the
        // Java API of the objects it is given (`?api`). Both are refused here, for every channel that renders.
        config.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        config.setAPIBuiltinEnabled(false);
        // `.md` templates are wrapped in this directive by the loader
        config.setSharedVariable("markdown", new MarkdownDirective());
        return config;
    }
}
