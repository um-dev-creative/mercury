package com.umdc.mercury.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mercury never reads or writes the {@code general} schema: backbone-rest owns it. Entities that map a table of that schema exist
 * only to hold a foreign key (built from an id), so no repository may query them, and no query may name the schema. A read there
 * fails in any environment where Mercury's database role has no grant on it ("permission denied for table application").
 */
@DisplayName("general schema boundary")
class GeneralSchemaBoundaryTest {

    private static final Path MAIN = Path.of("src/main/java");
    private static final Pattern GENERAL_ENTITY = Pattern.compile("@Table\\([^)]*schema\\s*=\\s*\"general\"[^)]*\\)\\s*public class (\\w+)");

    @Test
    @DisplayName("no repository queries an entity that maps a table of the general schema")
    void noRepositoryReadsTheGeneralSchema() throws IOException {
        List<String> generalEntities = new ArrayList<>();
        List<String> repositories = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher entity = GENERAL_ENTITY.matcher(source.replace("\n", " "));
                if (entity.find()) {
                    generalEntities.add(entity.group(1));
                }
                if (source.contains("extends JpaRepository<") || source.contains("extends CrudRepository<")) {
                    repositories.add(file.getFileName() + ": " + source);
                }
            }
        }
        assertTrue(!generalEntities.isEmpty(), "the guard found no entity of the general schema: the pattern is out of date");
        List<String> offenders = new ArrayList<>();
        for (String repository : repositories) {
            for (String entity : generalEntities) {
                if (repository.matches("(?s).*Repository<\\s*" + entity + "\\s*,.*")) {
                    offenders.add(repository.substring(0, repository.indexOf(':')) + " reads " + entity);
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Mercury must not read the general schema:\n  " + String.join("\n  ", offenders));
    }

    @Test
    @DisplayName("no query or code in Mercury names a table of the general schema")
    void noQueryNamesTheGeneralSchema() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String code = line.strip();
                    boolean comment = code.startsWith("//") || code.startsWith("*") || code.startsWith("/*");
                    if (!comment && code.matches(".*(@Query|@NativeQuery|createNativeQuery|nativeQuery|\\bfrom\\b|\\bjoin\\b).*\\bgeneral\\.\\w+.*")) {
                        offenders.add(file.getFileName() + ": " + code);
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Mercury must not query the general schema:\n  " + String.join("\n  ", offenders));
    }
}
