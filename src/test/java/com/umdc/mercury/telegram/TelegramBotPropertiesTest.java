package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Telegram bot properties")
class TelegramBotPropertiesTest {

    @Test
    @DisplayName("missing values take safe defaults and the channel is not active")
    void defaults() {
        final var p = new TelegramBotProperties(false, null, null, null, null, null, null);
        assertEquals("", p.botToken());
        assertEquals("https://api.telegram.org", p.apiBaseUrl());
        assertEquals(List.of("es", "en"), p.languages());
        assertEquals(Duration.ofSeconds(3), p.connectTimeout());
        assertEquals(Duration.ofSeconds(10), p.readTimeout());
        assertFalse(p.active());
    }

    @Test
    @DisplayName("enabled without a token stays off; with a token it is active")
    void active() {
        assertFalse(new TelegramBotProperties(true, " ", "", "", List.of(), null, null).active());
        assertTrue(new TelegramBotProperties(true, " 123:abc ", "s", "http://x", List.of("es"), null, null).active());
    }

    @Test
    @DisplayName("toString never shows the token or the webhook secret")
    void toStringHidesSecrets() {
        final var text = new TelegramBotProperties(true, "123:SECRET-TOKEN", "WEBHOOK-SECRET", "", null, null, null).toString();
        assertFalse(text.contains("SECRET-TOKEN"));
        assertFalse(text.contains("WEBHOOK-SECRET"));
        assertTrue(text.contains("***"));
    }
}
