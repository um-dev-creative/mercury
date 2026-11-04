package com.umdc.mercury.template.content;

import com.umdc.mercury.config.FreeMarkerConfig;
import com.umdc.mercury.template.markdown.MarkdownTemplates;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks a template without running it and says which parameters it reads. Uses the same restricted FreeMarker
 * configuration as production, so what it accepts is what Mercury would accept.
 */
public final class TemplateSyntax {

    private static final Configuration PARSER = FreeMarkerConfig.hardened(new StringTemplateLoader());

    private static final Pattern INTERPOLATION = Pattern.compile("\\$\\{\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern DIRECTIVE_USE = Pattern.compile("<#(?:if|elseif|list)\\s+\\(?\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern DECLARATION =
            Pattern.compile("<#(?:assign|local|global)\\s+([A-Za-z_][A-Za-z0-9_]*)|<#list\\s+[^>]*?\\sas\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s*,\\s*([A-Za-z_][A-Za-z0-9_]*))?|<#macro\\s+[A-Za-z_][A-Za-z0-9_]*\\s+([^>]*)");
    private static final Set<String> RESERVED = Set.of("true", "false", "null", "as", "in", "gt", "gte", "lt", "lte", "using");

    private TemplateSyntax() {
    }

    /**
     * @param name    file name, which tells whether the source is Markdown
     * @param content the template as written
     * @return what is wrong with it, empty if FreeMarker can read it
     */
    public static List<TemplateProblem> validate(String name, String content) {
        try {
            new Template(name, new StringReader(MarkdownTemplates.forFreeMarker(name, content)), PARSER);
            return List.of();
        } catch (ParseException e) {
            int line = Math.max(0, e.getLineNumber() - MarkdownTemplates.lineOffset(name));
            return List.of(new TemplateProblem(line, Math.max(0, e.getColumnNumber()), e.getEditorMessage()));
        } catch (IOException e) {
            return List.of(new TemplateProblem(0, 0, "The template could not be read"));
        }
    }

    /**
     * The names a template reads from the parameters it is given, in order of appearance. It is a reading of the text,
     * not an execution: names it declares itself (loop variables, {@code assign}, macro arguments) are left out, and a
     * parameter used only inside a complex expression may be missed.
     *
     * @param content the template as written
     * @return parameter names
     */
    public static List<String> parameters(String content) {
        Set<String> declared = new LinkedHashSet<>();
        Matcher declarations = DECLARATION.matcher(content);
        while (declarations.find()) {
            for (int group = 1; group <= 3; group++) {
                if (declarations.group(group) != null) {
                    declared.add(declarations.group(group));
                }
            }
            if (declarations.group(4) != null) {
                for (String argument : declarations.group(4).split("[\\s=]+")) {
                    if (argument.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                        declared.add(argument);
                    }
                }
            }
        }
        java.util.TreeMap<Integer, String> byPosition = new java.util.TreeMap<>();
        collect(INTERPOLATION.matcher(content), byPosition);
        collect(DIRECTIVE_USE.matcher(content), byPosition);
        Set<String> used = new LinkedHashSet<>(byPosition.values());
        used.removeAll(declared);
        used.removeAll(RESERVED);
        return new ArrayList<>(used);
    }

    private static void collect(Matcher matcher, java.util.Map<Integer, String> byPosition) {
        while (matcher.find()) {
            byPosition.put(matcher.start(1), matcher.group(1));
        }
    }
}
