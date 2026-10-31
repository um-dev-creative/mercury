package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Telegram text splitting")
class TelegramTextSplitterTest {

    @Test
    @DisplayName("a short text is one part and an empty one is none")
    void short_() {
        assertEquals(1, TelegramTextSplitter.split("hola").size());
        assertEquals(0, TelegramTextSplitter.split("  ").size());
    }

    @Test
    @DisplayName("a long text is cut at line breaks, every part within 4096, and nothing is lost")
    void atLineBreaks() {
        final var line = "x".repeat(99) + "\n";
        final var parts = TelegramTextSplitter.split(line.repeat(100));
        assertTrue(parts.size() >= 3);
        parts.forEach(p -> assertTrue(p.length() <= TelegramTextSplitter.MAX));
        assertEquals(100 * 99, parts.stream().mapToInt(p -> p.replace("\n", "").length()).sum());
    }

    @Test
    @DisplayName("without line breaks it cuts at the limit")
    void hardCut() {
        final var parts = TelegramTextSplitter.split("y".repeat(9000));
        assertEquals(3, parts.size());
        assertEquals(4096, parts.get(0).length());
        assertEquals(9000, parts.stream().mapToInt(String::length).sum());
    }
}
