package com.umdc.mercury.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** With a named SSL bundle Spring Boot ignores server.ssl.enabled-protocols: TLS 1.3 must live in the bundle's options. */
class SslBundleProtocolsTest {

    private static Properties applicationYml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(Path.of("src/main/resources/application.yml")));
        return yaml.getObject();
    }

    @Test
    void theBundleEnforcesTls13() {
        assertEquals("TLSv1.3", applicationYml().getProperty("spring.ssl.bundle.jks.mercury-security.options.enabled-protocols"));
    }

    @Test
    void theIgnoredServerSslPropertyIsNotUsedAnyMore() {
        Properties yml = applicationYml();

        assertEquals("mercury-security", yml.getProperty("server.ssl.bundle"));
        assertNull(yml.getProperty("server.ssl.enabled-protocols"));
    }
}
