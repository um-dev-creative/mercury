package com.umdc.mercury.template.storage;

import java.util.Set;

/**
 * The only names a template file can have. A name is a file name, optionally inside ONE application folder
 * ({@code iris/iris_welcome.ftl}); never a deeper path: no {@code ..}, no backslashes, nothing that could leave the
 * template folder or bucket when it is joined to a root.
 *
 * <p>The application folder is lower-case letters, digits, {@code _} and {@code -}, up to {@value #MAX_FOLDER_LENGTH}
 * characters, starting with a letter or digit.</p>
 *
 * <p>Letters, digits, {@code _} and {@code -}, in one or more parts separated by single dots; it starts with a letter
 * or digit and ends with one of {@code .ftl}, {@code .ftlh}, {@code .md}, {@code .html}.</p>
 */
public final class TemplateNames {

    /** Longest accepted name, extension included. */
    public static final int MAX_LENGTH = 128;

    /** Longest accepted application folder. */
    public static final int MAX_FOLDER_LENGTH = 64;

    private static final Set<String> EXTENSIONS = Set.of("ftl", "ftlh", "md", "html");

    private TemplateNames() {
    }

    /** @return whether {@code name} is a template name: a file name, or {@code folder/file name}. */
    public static boolean isValid(String name) {
        if (name == null) {
            return false;
        }
        int slash = name.indexOf('/');
        return slash < 0 ? isFileName(name) : isFolder(name.substring(0, slash)) && isFileName(name.substring(slash + 1));
    }

    /** @return {@code folder/file} for a valid folder, or {@code file} alone when the folder is null or blank. */
    public static String inFolder(String folder, String file) {
        return folder == null || folder.isBlank() ? file : folder + "/" + file;
    }

    /** @return the file name of a template name, without its application folder. */
    public static String fileOf(String name) {
        return name == null ? null : name.substring(name.lastIndexOf('/') + 1);
    }

    /** @return whether {@code folder} is a valid application folder. */
    public static boolean isFolder(String folder) {
        if (folder == null || folder.isEmpty() || folder.length() > MAX_FOLDER_LENGTH) {
            return false;
        }
        for (int i = 0; i < folder.length(); i++) {
            char c = folder.charAt(i);
            boolean lower = c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
            if (!lower && (i == 0 || c != '_' && c != '-')) {
                return false;
            }
        }
        return true;
    }

    /** @return whether {@code name} is a bare file name (no folder) a template may have. */
    public static boolean isFileName(String name) {
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
