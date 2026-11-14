package com.umdc.mercury.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards what Flyway needs to start Mercury safely: the migration files it will find and the settings that decide how
 * it treats a database that already has tables. A name Flyway cannot parse, a version that is not above the baseline
 * or a missing baseline would stop the application at startup, and no unit test starts the context.
 */
@DisplayName("Flyway migrations")
class FlywayMigrationsTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final String FLYWAY_SETTINGS = "flyway";

    @Test
    @DisplayName("every migration is named V<version>__<description>.sql and has content")
    void migrationNamesAndContent() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path file : migrationFiles()) {
            String name = file.getFileName().toString();
            if (!name.matches("V\\d+__[a-z0-9_]+\\.sql")) {
                problems.add(name + ": the name does not match V<number>__<snake_case>.sql");
            }
            if (Files.readString(file, StandardCharsets.UTF_8).isBlank()) {
                problems.add(name + ": the file is empty");
            }
        }
        assertTrue(problems.isEmpty(), "Flyway would refuse these migrations:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("no migration contains a ${...} sequence: Flyway reads it as a placeholder, even inside a comment, and refuses to start")
    void noFlywayPlaceholdersInScripts() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path file : migrationFiles()) {
            if (Files.readString(file, StandardCharsets.UTF_8).contains("${")) {
                problems.add(file.getFileName() + ": contains ${ (Flyway: No value provided for placeholder)");
            }
        }
        assertTrue(problems.isEmpty(), "Flyway would fail to parse:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("versions are unique, so Flyway never sees two migrations for the same version")
    void versionsAreUnique() throws IOException {
        List<Integer> versions = versions();
        assertEquals(versions.stream().distinct().count(), versions.size(), "Duplicated versions: " + versions);
    }

    @Test
    @DisplayName("every migration is above the baseline, otherwise Flyway would skip it as already applied")
    void versionsAreAboveTheBaseline() throws IOException {
        int baseline = Integer.parseInt(String.valueOf(flywaySettings().get("baseline-version")));
        List<Integer> below = versions().stream().filter(version -> version <= baseline).toList();
        assertTrue(below.isEmpty(), "Versions at or below baseline-version " + baseline + " are never applied: " + below);
    }

    @Test
    @DisplayName("migrations already applied by hand can run again: no statement fails when its object exists")
    void migrationsAreIdempotent() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path file : migrationFiles()) {
            for (String statement : Files.readString(file, StandardCharsets.UTF_8).split(";")) {
                String sql = withoutComments(statement).toUpperCase(java.util.Locale.ROOT);
                boolean creates = sql.contains("CREATE TABLE") || sql.contains("CREATE INDEX") || sql.contains("ADD COLUMN");
                if (creates && !sql.contains("IF NOT EXISTS")) {
                    problems.add(file.getFileName() + ": " + sql.strip().replaceAll("\\s+", " "));
                }
            }
        }
        assertTrue(problems.isEmpty(),
                "These statements fail on a database where the script was already applied by hand:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("Flyway is on by default, can be switched off, and baselines a database that already has tables")
    void settingsProtectExistingDatabases() throws IOException {
        Map<String, Object> flyway = flywaySettings();
        assertEquals("${MERCURY_FLYWAY_ENABLED:true}", flyway.get("enabled"));
        assertEquals(Boolean.TRUE, flyway.get("baseline-on-migrate"));
        assertEquals(Boolean.TRUE, flyway.get("validate-on-migrate"));
        assertEquals(Boolean.FALSE, flyway.get("out-of-order"));
        assertEquals("classpath:db/migration", flyway.get("locations"));
        assertEquals("mercury", flyway.get("schemas"));
        assertEquals("mercury", flyway.get("default-schema"));
    }

    @Test
    @DisplayName("the baseline is the last version that was applied by hand before Flyway")
    void baselineIsBelowTheFirstMigration() throws IOException {
        int baseline = Integer.parseInt(String.valueOf(flywaySettings().get("baseline-version")));
        int first = versions().stream().min(Integer::compare).orElseThrow();
        assertEquals(first - 1, baseline, "baseline-version must sit right below the first versioned migration");
    }

    private static List<Path> migrationFiles() throws IOException {
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<Path> sql = files.filter(path -> path.toString().endsWith(".sql")).sorted().toList();
            assertFalse(sql.isEmpty(), "No migrations found in " + MIGRATIONS);
            return sql;
        }
    }

    private static List<Integer> versions() throws IOException {
        return migrationFiles().stream()
                .map(path -> path.getFileName().toString())
                .map(name -> Integer.parseInt(name.substring(1, name.indexOf("__"))))
                .toList();
    }

    private static String withoutComments(String statement) {
        StringBuilder sql = new StringBuilder();
        for (String line : statement.split("\n")) {
            if (!line.strip().startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        return sql.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flywaySettings() throws IOException {
        try (InputStream yml = FlywayMigrationsTest.class.getClassLoader().getResourceAsStream("application.yml")) {
            for (Object document : new Yaml().loadAll(yml)) {
                if (document instanceof Map<?, ?> root && root.get("spring") instanceof Map<?, ?> spring
                        && spring.get(FLYWAY_SETTINGS) instanceof Map<?, ?> flyway) {
                    return (Map<String, Object>) flyway;
                }
            }
        }
        assertNotNull(null, "application.yml has no spring.flyway section");
        return Map.of();
    }
}
