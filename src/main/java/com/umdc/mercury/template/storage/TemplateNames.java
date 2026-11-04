package com.umdc.mercury.template.storage;

import java.util.Set;

/**
 * The only names a template file can have. A name is a file name, never a path: no directories, no {@code ..}, no
 * backslashes, nothing that could leave the template folder or bucket when it is joined to a root.
 *
 * <p>Letters, digits, {@code _} and {@code -}, in one or more parts separated by single dots; it starts with a letter
 * or digit and ends with one of {@code .ftl}, {@code .ftlh}, {@code .md}, {@code .html}.</p>
 */
public final class TemplateNames {

    /** Longest accepted name, extension included. */
    public static final int MAX_LENGTH = 128;

    private static final Set<String> EXTENSIONS = Set.of("ftl", "ftlh", "md", "html");

    private TemplateNames() {
    }

    /** @return whether {@code name} is a file name a template may have. */
    public static boolean isValid(String name) {
        if (name == null || name.isEmpty() || name.length() > MAX_LENGTH || !isAlphanumeric(name.charAt(0))) {
            return false;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 1 || !EXTENSIONS.contains(name.substring(lastDot + 1))) {
            return false;
        }
        char previous = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean dot = c == '.';
            // a dot never follows another dot, and a part is never empty
            if (!dot && !isAlphanumeric(c) && c != '_' && c != '-' || dot && previous == '.') {
                return false;
            }
            previous = c;
        }
        return true;
    }

    /**
     * @throws IllegalArgumentException if {@code name} is not a valid template file name.
     */
    public static String require(String name) {
        if (!isValid(name)) {
            throw new IllegalArgumentException("Invalid template name");
        }
        return name;
    }

    private static boolean isAlphanumeric(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
    }
}
