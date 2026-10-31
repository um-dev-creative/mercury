package com.umdc.mercury.telegram;

import java.util.ArrayList;
import java.util.List;

/** Splits a text into parts of at most {@link #MAX} characters (Telegram's limit), preferring line breaks (T-115). */
final class TelegramTextSplitter {

    static final int MAX = 4096;

    private TelegramTextSplitter() { }

    static List<String> split(String text) {
        final var parts = new ArrayList<String>();
        String rest = text.strip();
        while (rest.length() > MAX) {
            int cut = rest.lastIndexOf('\n', MAX);
            if (cut < MAX / 2) {
                cut = MAX;
            }
            parts.add(rest.substring(0, cut).strip());
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }
}
