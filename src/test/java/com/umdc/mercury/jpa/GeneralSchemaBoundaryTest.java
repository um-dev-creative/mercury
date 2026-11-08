package com.umdc.mercury.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mercury never reads or writes the {@code general} schema: backbone-rest owns it. No entity may map a table of that schema (not even
 * as the target of a foreign key: Hibernate checks such a reference with a SELECT on it, and that fails wherever Mercury's database
 * role has no grant, "permission denied for table application"), so applications and users are kept as plain ids; and no query may name
 * the schema. The foreign keys stay in the database, which enforces them without Mercury reading the referenced table.
 */
@DisplayName("general schema boundary")
class GeneralSchemaBoundaryTest {

    private static final Path MAIN = Path.of("src/main/java");

    private static final java.util.regex.Pattern GENERAL_TABLE_MAPPING = java.util.regex.Pattern.compile("@Table\\([^)]*schema\\s*=\\s*\"general\"");
    private static final java.util.regex.Pattern QUERY_KEYWORD = java.util.regex.Pattern.compile("@Query|@NativeQuery|createNativeQuery|nativeQuery|\\bfrom\\b|\\bjoin\\b");
    private static final java.util.regex.Pattern GENERAL_TABLE = java.util.regex.Pattern.compile("\\bgeneral\\.\\w+");

    /** Whether a line of code has a query keyword followed by a `general.<table>` name. */
    private static boolean namesGeneralTable(String code) {
        var keyword = QUERY_KEYWORD.matcher(code);
        return keyword.find() && GENERAL_TABLE.matcher(code).region(keyword.end(), code.length()).find();
    }

    @Test
    @DisplayName("no entity maps a table of the general schema")
    void noEntityMapsTheGeneralSchema() throws IOException {
        List<String> offenders = new ArrayList<>();
        int entities = 0;
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                if (!source.contains("@Entity")) {
                    continue;
                }
                entities++;
                if (GENERAL_TABLE_MAPPING.matcher(source).find()) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(entities > 0, "the guard found no entity: the pattern is out of date");
        assertTrue(offenders.isEmpty(), "Mercury must not map the general schema:\n  " + String.join("\n  ", offenders));
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
                    if (!comment && namesGeneralTable(code)) {
                        offenders.add(file.getFileName() + ": " + code);
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Mercury must not query the general schema:\n  " + String.join("\n  ", offenders));
    }
}
