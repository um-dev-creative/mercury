package com.umdc.mercury.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TelegramConfigTest {

    private final TelegramConfig config = new TelegramConfig();

    @Test
    void noTokenMeansNoClient() {
        assertNull(config.telegramClient("  "));
    }

    @Test
    void aTokenBuildsTheClient() {
        assertNotNull(config.telegramClient("123:abc"));
    }
}
