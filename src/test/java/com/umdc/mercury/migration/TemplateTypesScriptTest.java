package com.umdc.mercury.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards {@code scripts/templates/seed-template-types.sql}: it must insert each type once, be safe to re-run, and give a type to every
 * kind of template that ships (the reference copies in {@code src/test/resources/templates}). A new template with no type fails here.
 */
@DisplayName("Template types script")
class TemplateTypesScriptTest {

    private static final Path SCRIPT = Path.of("scripts/templates/seed-template-types.sql");
    private static final Path TEMPLATES = Path.of("src/test/resources/templates");
    private static final Pattern TYPE_ROW = Pattern.compile("^\\s*\\('([A-Z_]+)',\\s*'", Pattern.MULTILINE);
    private static final Pattern MAP_LINE = Pattern.compile("^--\\s+MAP\\s+(\\S+)\\s+([A-Z_]+)\\s*$", Pattern.MULTILINE);
    private static final int NAME_LENGTH = 100;

    @Test
    @DisplayName("inserts each type once, with a name that fits the column and a description")
    void insertsEachTypeOnce() throws IOException {
        List<String> names = types();

        assertFalse(names.isEmpty());
        assertEquals(names.size(), new HashSet<>(names).size(), "a type is inserted twice");
        names.forEach(name -> assertTrue(name.length() <= NAME_LENGTH, name + " is longer than the column"));
        assertTrue(names.contains("VERIFICATION_CODE"), "the verification-code type is what umdc.verification.code.template.id points to");
    }

    @Test
    @DisplayName("is safe to run twice: it only inserts names that do not exist yet, in one transaction, and deletes nothing")
    void isIdempotent() throws IOException {
        String sql = Files.readString(SCRIPT);

        assertTrue(sql.contains("WHERE NOT EXISTS (SELECT 1 FROM mercury.template_types t WHERE lower(t.name) = lower(v.name))"));
        assertTrue(sql.contains("BEGIN;") && sql.contains("COMMIT;"));
        assertFalse(sql.toUpperCase().contains("DELETE FROM") || sql.toUpperCase().contains("TRUNCATE") || sql.toUpperCase().contains("UPDATE MERCURY"));
    }

    @Test
    @DisplayName("every kind of template that ships has a type, and every type is used by some template")
    void everyTemplateKindHasAType() throws IOException {
        Map<String, String> typeOfKind = new HashMap<>();
        Matcher map = MAP_LINE.matcher(Files.readString(SCRIPT));
        while (map.find()) {
            typeOfKind.put(map.group(1), map.group(2));
        }
        Set<String> declared = new HashSet<>(types());

        Set<String> kinds;
        try (Stream<Path> files = Files.list(TEMPLATES)) {
            kinds = files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".ftl"))
                    .map(TemplateTypesScriptTest::kindOf)
                    .collect(Collectors.toSet());
        }

        for (String kind : kinds) {
            assertTrue(typeOfKind.containsKey(kind), "the template kind " + kind + " has no MAP line in the script");
            assertTrue(declared.contains(typeOfKind.get(kind)), kind + " maps to a type the script does not insert");
        }
        assertEquals(kinds, typeOfKind.keySet(), "a MAP line names a kind that no template has");
        assertEquals(declared, new HashSet<>(typeOfKind.values()), "a type is inserted but no template uses it");
    }

    /** {@code zb_budget_exceeded_tg_es.ftl} → {@code zb_budget_exceeded}. */
    static String kindOf(String fileName) {
        return fileName.replaceFirst("\\.ftl$", "").replaceFirst("_(es|en)$", "").replaceFirst("_tg$", "");
    }

    private static List<String> types() throws IOException {
        Matcher row = TYPE_ROW.matcher(Files.readString(SCRIPT));
        List<String> names = new java.util.ArrayList<>();
        while (row.find()) {
            names.add(row.group(1));
        }
        return names;
    }

    @Test
    @DisplayName("the kind of a file ignores language and Telegram variants")
    void kindIgnoresVariants() {
        assertEquals("zb_budget_exceeded", kindOf("zb_budget_exceeded_tg_es.ftl"));
        assertEquals("zb_budget_exceeded", kindOf("zb_budget_exceeded_en.ftl"));
        assertEquals("first_message_template", kindOf("first_message_template.ftl"));
    }
}
