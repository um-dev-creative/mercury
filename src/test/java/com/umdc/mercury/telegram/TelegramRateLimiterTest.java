package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Telegram rate limits")
class TelegramRateLimiterTest {

    private long millis = Instant.parse("2026-11-02T10:00:00Z").toEpochMilli();
    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    };
    private final TelegramRateLimiter limiter = new TelegramRateLimiter(clock);

    @Test
    @DisplayName("one message per second to the same chat")
    void perChat() {
        assertTrue(limiter.tryAcquire(1L));
        assertFalse(limiter.tryAcquire(1L));
        millis += 999;
        assertFalse(limiter.tryAcquire(1L));
        millis += 1;
        assertTrue(limiter.tryAcquire(1L));
    }

    @Test
    @DisplayName("thirty messages per second for the bot, then room again")
    void perBot() {
        for (long chat = 0; chat < 30; chat++) {
            assertTrue(limiter.tryAcquire(chat));
        }
        assertFalse(limiter.tryAcquire(99L));
        millis += 1000;
        assertTrue(limiter.tryAcquire(99L));
    }

    @Test
    @DisplayName("the public constructor works with the system clock")
    void system() {
        assertTrue(new TelegramRateLimiter().tryAcquire(5L));
    }
}
