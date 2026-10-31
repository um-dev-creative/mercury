package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram inbox state")
class TelegramInboxTest {

    private final TelegramInboxStateRepository state = mock(TelegramInboxStateRepository.class);
    private final Instant t0 = Instant.parse("2026-11-02T10:00:00Z");
    private final TelegramInbox inbox = new TelegramInbox(state, Clock.fixed(t0, ZoneOffset.UTC));

    @Test
    @DisplayName("firstTime is true when the id advanced the counter and false for a replay")
    void firstTime() {
        when(state.advance("a", 5L)).thenReturn(1);
        when(state.advance("a", 4L)).thenReturn(0);
        assertTrue(inbox.firstTime("a", 5L));
        assertFalse(inbox.firstTime("a", 4L));
        verify(state, org.mockito.Mockito.times(2)).ensure("a");
    }

    @Test
    @DisplayName("nextOffset is last + 1, or null when nothing was handled")
    void nextOffset() {
        assertNull(inbox.nextOffset("a"));
        final var entity = new TelegramInboxStateEntity();
        entity.setLastUpdateId(41);
        when(state.findById("a")).thenReturn(Optional.of(entity));
        assertEquals(42L, inbox.nextOffset("a"));
        entity.setLastUpdateId(0);
        assertNull(inbox.nextOffset("a"));
    }

    @Test
    @DisplayName("the lease is taken for ttl from now and reports whether this owner holds it")
    void lease() {
        final var now = LocalDateTime.ofInstant(t0, ZoneOffset.UTC);
        when(state.acquire("a", "me", now, now.plusSeconds(90))).thenReturn(1);
        assertTrue(inbox.acquireLease("a", "me", Duration.ofSeconds(90)));
        assertFalse(inbox.acquireLease("a", "other", Duration.ofSeconds(90)));
    }

    @Test
    @DisplayName("public constructor uses the system clock; entity accessors round-trip")
    void misc() {
        final var s = new TelegramInbox(state);
        assertFalse(s.acquireLease("a", "me", Duration.ofSeconds(1)));
        final var entity = new TelegramInboxStateEntity();
        entity.setApplication("a");
        entity.setLeaseOwner("me");
        entity.setLeaseUntil(LocalDateTime.MAX);
        assertEquals("a", entity.getApplication());
        assertEquals("me", entity.getLeaseOwner());
        assertEquals(LocalDateTime.MAX, entity.getLeaseUntil());
    }
}
