package com.umdc.mercury.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram message acceptance")
class TelegramMessageServiceTest {

    private static final String APP = "zerobound";
    private final TelegramMessageRepository messages = mock(TelegramMessageRepository.class);
    private final TelegramLinkService links = mock(TelegramLinkService.class);
    private final TelegramTemplateRenderer templates = mock(TelegramTemplateRenderer.class);
    private final UUID user = UUID.randomUUID();
    private TelegramMessageService service;

    @BeforeEach
    void setUp() {
        service = new TelegramMessageService(messages, links, templates, new TelegramBotProperties(true, "t", "", "", List.of("es", "en"), null, null),
                Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC));
        when(templates.exists("debt_due", "es")).thenReturn(true);
        when(links.targetOf(APP, user)).thenReturn(Optional.of(new TelegramLinkService.Target(5L, true)));
    }

    private TelegramMessageRequest request() {
        return new TelegramMessageRequest(user, "debt_due", null, Map.of("name", "Visa"), null);
    }

    @Test
    @DisplayName("queues a message for a linked user: QUEUED, due now, default language es, key kept")
    void accepts() {
        final var result = assertInstanceOf(TelegramMessageService.Result.Accepted.class, service.accept(APP, request(), " k1 "));
        assertFalse(result.replayed());
        assertEquals("QUEUED", result.status());
        final var captor = ArgumentCaptor.forClass(TelegramMessageEntity.class);
        verify(messages).saveAndFlush(captor.capture());
        assertEquals("es", captor.getValue().getLanguage());
        assertEquals("k1", captor.getValue().getIdempotencyKey());
        assertEquals(APP, captor.getValue().getApplication());
        assertEquals(Map.of("name", "Visa"), captor.getValue().getParams());
        assertFalse(captor.getValue().isDisableNotification());
    }

    @Test
    @DisplayName("a repeat with the same key and request answers the first message without queuing another")
    void replays() {
        final var first = new TelegramMessageEntity();
        first.setId(UUID.randomUUID());
        first.setStatus("SENT");
        first.setRequestHash(TelegramMessageService.hash(request(), "es", Map.of("name", "Visa")));
        when(messages.findByApplicationAndIdempotencyKey(APP, "k1")).thenReturn(Optional.of(first));
        final var result = assertInstanceOf(TelegramMessageService.Result.Accepted.class, service.accept(APP, request(), "k1"));
        assertTrue(result.replayed());
        assertEquals("SENT", result.status());
        verify(messages, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("the same key with another request is refused")
    void keyReuse() {
        final var first = new TelegramMessageEntity();
        first.setRequestHash("other");
        when(messages.findByApplicationAndIdempotencyKey(APP, "k1")).thenReturn(Optional.of(first));
        assertThrows(IllegalStateException.class, () -> service.accept(APP, request(), "k1"));
    }

    @Test
    @DisplayName("a race on the key answers with the winner; without a key the error is not swallowed")
    void race() {
        final var winner = new TelegramMessageEntity();
        winner.setId(UUID.randomUUID());
        winner.setStatus("QUEUED");
        winner.setRequestHash(TelegramMessageService.hash(request(), "es", Map.of("name", "Visa")));
        when(messages.findByApplicationAndIdempotencyKey(APP, "k1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(messages.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertTrue(assertInstanceOf(TelegramMessageService.Result.Accepted.class, service.accept(APP, request(), "k1")).replayed());
        assertThrows(DataIntegrityViolationException.class, () -> service.accept(APP, request(), null));
    }

    @Test
    @DisplayName("unknown template or unsupported language is an IllegalArgumentException")
    void badTemplate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.accept(APP, new TelegramMessageRequest(user, "nope", "es", null, null), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.accept(APP, new TelegramMessageRequest(user, "debt_due", "fr", null, null), null));
    }

    @Test
    @DisplayName("not linked and blocked users are reported, nothing is queued")
    void notLinked() {
        when(links.targetOf(APP, user)).thenReturn(Optional.empty());
        assertInstanceOf(TelegramMessageService.Result.NotLinked.class, service.accept(APP, request(), null));
        when(links.targetOf(APP, user)).thenReturn(Optional.of(new TelegramLinkService.Target(5L, false)));
        assertInstanceOf(TelegramMessageService.Result.Blocked.class, service.accept(APP, request(), null));
        verify(messages, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("status is scoped to the caller; the hash ignores map order but not content")
    void statusAndHash() {
        assertTrue(service.status(UUID.randomUUID(), APP).isEmpty());
        final var m = new TelegramMessageEntity();
        m.setId(UUID.randomUUID());
        m.setStatus("SENT");
        when(messages.findByIdAndApplication(m.getId(), APP)).thenReturn(Optional.of(m));
        assertEquals("SENT", service.status(m.getId(), APP).orElseThrow().status());
        final var a = TelegramMessageService.hash(request(), "es", Map.of("a", 1, "b", 2));
        assertEquals(a, TelegramMessageService.hash(request(), "es", new java.util.LinkedHashMap<>(Map.of("b", 2, "a", 1))));
        assertFalse(a.equals(TelegramMessageService.hash(request(), "es", Map.of("a", 1, "b", 3))));
        assertTrue(new TelegramMessageService(messages, links, templates, new TelegramBotProperties(true, "t", "", "", null, null, null))
                .status(UUID.randomUUID(), APP).isEmpty());
    }
}
