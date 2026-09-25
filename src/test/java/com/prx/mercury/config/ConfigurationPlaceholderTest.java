package com.prx.mercury.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against property placeholders in Java sources that no longer resolve against
 * {@code application.yml}.
 *
 * <p>The {@code com.prx} → {@code com.umdc} migration renamed the whole configuration
 * namespace in YAML but left some {@code ${prx.*}} placeholders behind in annotations.
 * A placeholder <em>without</em> a default that resolves to nothing makes the Spring
 * context fail to start, which no unit test caught because the project has no
 * {@code @SpringBootTest}. This test closes that gap without needing any infrastructure.</p>
 *
 * <p>Only placeholders without a default are asserted: a placeholder that declares a
 * default is always resolvable, so its absence from the YAML cannot break startup
 * (some are intentionally supplied only by Vault/config-server at runtime).</p>
 */
@DisplayName("Configuration placeholder resolution")
class ConfigurationPlaceholderTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final String APPLICATION_YML = "application.yml";

    /** Matches ${some.property} but not ${some.property:withDefault}. */
    private static final Pattern PLACEHOLDER_WITHOUT_DEFAULT =
            Pattern.compile("\\$\\{([a-z][a-zA-Z0-9._-]*)}");

    @Test
    @DisplayName("every placeholder without a default resolves against application.yml")
    void placeholdersWithoutDefaultsAreDeclared() throws IOException {
        Set<String> declared = declaredProperties();
        List<String> unresolved = new ArrayList<>();

        try (Stream<Path> sources = Files.walk(SOURCE_ROOT)) {
            sources.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                String content = readString(path);
                Matcher matcher = PLACEHOLDER_WITHOUT_DEFAULT.matcher(content);
                while (matcher.find()) {
                    String property = matcher.group(1);
                    if (!declared.contains(property)) {
                        unresolved.add(property + "  (" + SOURCE_ROOT.relativize(path) + ")");
                    }
                }
            });
        }

        assertTrue(unresolved.isEmpty(),
                "These placeholders have no default and are not declared in " + APPLICATION_YML
                        + ", so the Spring context would fail to start:\n  " + String.join("\n  ", unresolved));
    }

    /** Flattens application.yml into dotted property paths, e.g. {@code umdc.scheduler.send-email.fixed-rate}. */
    private Set<String> declaredProperties() {
        Set<String> properties = new HashSet<>();
        try (InputStream yml = getClass().getClassLoader().getResourceAsStream(APPLICATION_YML)) {
            new Yaml().loadAll(yml).forEach(document -> flatten(document, "", properties));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + APPLICATION_YML, e);
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    private void flatten(Object node, String prefix, Set<String> properties) {
        if (!(node instanceof Map<?, ?> map)) {
            return;
        }
        ((Map<String, Object>) map).forEach((key, value) -> {
            String path = prefix + key;
            properties.add(path);
            flatten(value, path + ".", properties);
        });
    }

    private String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + path, e);
        }
    }
}
