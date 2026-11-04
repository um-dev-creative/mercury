package com.umdc.mercury.template.storage;

import java.util.regex.Pattern;

/**
 * The only names a template file can have. A name is a file name, never a path: no directories, no {@code ..}, no
 * backslashes, nothing that could leave the template folder or bucket when it is joined to a root.
 */
public final class TemplateNames {

    /** Longest accepted name, extension included. */
    public static final int MAX_LENGTH = 128;

    private static final Pattern VALID =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*(\\.[A-Za-z0-9_-]+)*\\.(ftl|ftlh|md|html)$");

    private TemplateNames() {
    }

    /** @return whether {@code name} is a file name a template may have. */
    public static boolean isValid(String name) {
        return name != null && name.length() <= MAX_LENGTH && VALID.matcher(name).matches();
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
}
