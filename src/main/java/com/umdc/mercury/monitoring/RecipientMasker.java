package com.umdc.mercury.monitoring;

/** Masks a recipient before it is stored or shown by the monitoring views: enough to tell messages apart, never enough to contact anyone. */
public final class RecipientMasker {

    private RecipientMasker() {
        // utility class
    }

    /**
     * {@code john.doe@example.com} becomes {@code j***@e***.com}; anything else (phone number, chat id, device token) keeps only its last
     * three characters: {@code ***123}. A blank value gives {@code null}.
     */
    public static String mask(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return null;
        }
        var value = recipient.strip();
        int at = value.indexOf('@');
        if (at > 0 && at < value.length() - 1) {
            var domain = value.substring(at + 1);
            int dot = domain.lastIndexOf('.');
            var host = dot > 0 ? domain.substring(0, dot) : domain;
            var suffix = dot > 0 ? domain.substring(dot) : "";
            return value.charAt(0) + "***@" + host.charAt(0) + "***" + suffix;
        }
        return value.length() <= 3 ? "***" : "***" + value.substring(value.length() - 3);
    }
}
