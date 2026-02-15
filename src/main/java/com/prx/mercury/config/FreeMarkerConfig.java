package com.prx.mercury.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

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
     * Creates and configures a FreeMarker Configuration instance.
     * Sets up template loading from the configured template directory.
     *
     * @return A configured FreeMarker Configuration object
     * @throws IOException If there is an error accessing the template directory
     */
    @Bean
    public freemarker.template.Configuration getFreeMarkerConfiguration() throws IOException {
        File file = new File(templateLoaderPath);
        freemarker.template.Configuration config = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_30);
        config.setDirectoryForTemplateLoading(file);
        return config;
    }
}
