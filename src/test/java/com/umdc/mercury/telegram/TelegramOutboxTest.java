package com.umdc.mercury.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram outbox transitions")
class TelegramOutboxTest {

    private static final Instant T0 = Instant.parse("2026-11-02T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(T0, ZoneOffset.UTC);
    private final TelegramMessageRepository repo = mock(TelegramMessageRepository.class);
    private final TelegramOutbox outbox = new TelegramOutbox(repo, Clock.fixed(T0, ZoneOffset.UTC));
    private TelegramMessageEntity message;

    @BeforeEach
    void setUp() {
        message = new TelegramMessageEntity();
        message.setId(UUID.randomUUID());
        message.setApplication("zerobound");
        message.setUserId(UUID.randomUUID());
        message.setKind("debt_due");
        message.setLanguage("es");
        message.setParams(Map.of("amount", "$1"));
        message.setStatus(TelegramMessageEntity.QUEUED);
        message.setCreatedAt(NOW.minusMinutes(1));
        when(repo.findById(message.getId())).thenReturn(Optional.of(message));
    }

    @Test
    @DisplayName("claim leases the message for two minutes and returns its snapshot")
    void claim() {
        when(repo.lockDue(NOW, 20)).thenReturn(List.of(message));
        final var claimed = outbox.claim(20);
        assertEquals(1, claimed.size());
        assertEquals(message.getId(), claimed.getFirst().id());
        assertEquals(NOW.plus(TelegramOutbox.LEASE), message.getNextAttemptAt());
        assertEquals("debt_due", claimed.getFirst().kind());
    }

    @Test
    @DisplayName("sent is final: SENT, stamped, and the parameters are emptied")
    void sent() {
        outbox.sent(message.getId());
        assertEquals("SENT", message.getStatus());
        assertEquals(NOW, message.getSentAt());
        assertEquals(Map.of(), message.getParams());
        verify(repo).save(message);
    }

    @Test
    @DisplayName("blocked and failed are final and empty the parameters")
    void finals() {
        outbox.blocked(message.getId());
        assertEquals("BLOCKED", message.getStatus());
        assertEquals(Map.of(), message.getParams());
        message.setStatus("QUEUED");
        outbox.failed(message.getId(), "template");
        assertEquals("FAILED", message.getStatus());
        assertEquals("template", message.getLastError());
    }

    @Test
    @DisplayName("retry backs off 1 min, 5 min, 30 min, 2 h, 12 h and the 6th failure is final")
    void retries() {
        final Duration[] expected = {Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(12)};
        for (int i = 0; i < expected.length; i++) {
            outbox.retry(message.getId(), "unavailable");
            assertEquals("QUEUED", message.getStatus());
            assertEquals(NOW.plus(expected[i]), message.getNextAttemptAt());
        }
        outbox.retry(message.getId(), "unavailable");
        assertEquals("FAILED", message.getStatus());
        assertEquals(6, message.getAttempts());
    }

    @Test
    @DisplayName("a message older than 48 hours fails on its next failure")
    void tooOld() {
        message.setCreatedAt(NOW.minusHours(49));
        outbox.retry(message.getId(), "unavailable");
        assertEquals("FAILED", message.getStatus());
    }

    @Test
    @DisplayName("defer reschedules without spending an attempt; partSent records progress; unknown ids are ignored")
    void deferAndParts() {
        outbox.defer(message.getId(), NOW.plusSeconds(7));
        assertEquals(NOW.plusSeconds(7), message.getNextAttemptAt());
        assertEquals(0, message.getAttempts());
        outbox.partSent(message.getId(), 2);
        assertEquals(2, message.getPartsSent());
        outbox.sent(UUID.randomUUID());
        assertNull(message.getLastError());
        assertEquals(0, new TelegramOutbox(repo).claim(5).size());
    }
}
