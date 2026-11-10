package com.umdc.mercury.ingress;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The identity callers use for a template (decision D-15): a <em>name</em> and a <em>language</em>. The name is the template's location without its
 * extension and without the trailing {@code _<language>} (location {@code zb_verification_es.ftl}, language {@code es} -> name {@code zb_verification}).
 */
public final class TemplateIdentity {

    private static final Pattern LANGUAGE_SUFFIX = Pattern.compile("_([A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,4})?)\\.[A-Za-z0-9]+$");

    private TemplateIdentity() {
    }

    /** The language a location ends with ({@code zb_verification_es.ftl} -> {@code es}), when it has one. */
    public static Optional<String> languageOf(String location) {
        if (location == null) {
            return Optional.empty();
        }
        Matcher m = LANGUAGE_SUFFIX.matcher(location);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    /** The name of the template at {@code location} in {@code language}: the location minus the extension and the language suffix. */
    public static String nameOf(String location, String language) {
        String base = location;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        String suffix = "_" + language;
        if (language != null && !language.isBlank() && base.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
            base = base.substring(0, base.length() - suffix.length());
        }
        return base;
    }
}
