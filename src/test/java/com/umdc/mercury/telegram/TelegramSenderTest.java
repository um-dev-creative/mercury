package com.umdc.mercury.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram sender")
class TelegramSenderTest {

    private static final Instant T0 = Instant.parse("2026-11-02T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(T0, ZoneOffset.UTC);
    private final TelegramOutbox outbox = mock(TelegramOutbox.class);
    private final TelegramLinkService links = mock(TelegramLinkService.class);
    private final TelegramTemplateRenderer templates = mock(TelegramTemplateRenderer.class);
    private final TelegramBotApi api = mock(TelegramBotApi.class);
    private final TelegramRateLimiter limiter = mock(TelegramRateLimiter.class);
    private final UUID user = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private TelegramSender sender;

    @BeforeEach
    void setUp() {
        sender = new TelegramSender(new TelegramBotProperties(true, "t", "", "", null, null, null), outbox, links, templates, api, limiter,
                Clock.fixed(T0, ZoneOffset.UTC));
        when(outbox.claim(20)).thenReturn(List.of(new TelegramOutbox.Claimed(id, "zerobound", user, "debt_due", "es", Map.of(), false, 0, 0, NOW)));
        when(links.targetOf("zerobound", user)).thenReturn(Optional.of(new TelegramLinkService.Target(5L, true)));
        when(limiter.tryAcquire(5L)).thenReturn(true);
        when(templates.render(anyString(), anyString(), org.mockito.ArgumentMatchers.anyMap())).thenReturn("hola");
        when(api.sendMessage(anyLong(), anyString(), anyBoolean())).thenReturn(new BotResult.Ok(Map.of()));
    }

    @Test
    @DisplayName("renders, sends and marks the message sent")
    void sends() {
        sender.sendDue();
        verify(api).sendMessage(5L, "hola", false);
        verify(outbox).partSent(id, 1);
        verify(outbox).sent(id);
    }

    @Test
    @DisplayName("a long text goes in parts and resumes after the parts already sent")
    void parts() {
        when(templates.render(anyString(), anyString(), org.mockito.ArgumentMatchers.anyMap())).thenReturn("y".repeat(5000));
        when(outbox.claim(20)).thenReturn(List.of(new TelegramOutbox.Claimed(id, "zerobound", user, "debt_due", "es", Map.of(), true, 1, 1, NOW)));
        sender.sendDue();
        verify(api, times(1)).sendMessage(eq(5L), anyString(), eq(true));
        verify(outbox).sent(id);
    }

    @Test
    @DisplayName("an unlinked or blocked user ends the message as BLOCKED")
    void notLinked() {
        when(links.targetOf("zerobound", user)).thenReturn(Optional.empty());
        sender.sendDue();
        when(links.targetOf("zerobound", user)).thenReturn(Optional.of(new TelegramLinkService.Target(5L, false)));
        sender.sendDue();
        verify(outbox, times(2)).blocked(id);
        verify(api, never()).sendMessage(anyLong(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("over the rate limit the message waits a second without spending an attempt")
    void rateLimited() {
        when(limiter.tryAcquire(5L)).thenReturn(false);
        sender.sendDue();
        verify(outbox).defer(id, NOW.plusSeconds(1));
        verify(api, never()).sendMessage(anyLong(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("a template that cannot be rendered fails the message for good")
    void templateFails() {
        when(templates.render(anyString(), anyString(), org.mockito.ArgumentMatchers.anyMap())).thenThrow(new IllegalStateException("x"));
        sender.sendDue();
        verify(outbox).failed(id, "template");
    }

    @Test
    @DisplayName("Bot API outcomes: 429 waits retry_after, 403 blocks the link, 4xx fails, 5xx retries")
    void outcomes() {
        when(api.sendMessage(anyLong(), anyString(), anyBoolean())).thenReturn(new BotResult.RateLimited(17), new BotResult.Forbidden("blocked"),
                new BotResult.Rejected(400, "bad"), new BotResult.Unavailable("down"));
        for (int i = 0; i < 4; i++) {
            sender.sendDue();
        }
        verify(outbox).defer(id, NOW.plusSeconds(17));
        verify(links).markBlocked("zerobound", 5L);
        verify(outbox).blocked(id);
        verify(outbox).failed(id, "rejected-400");
        verify(outbox).retry(id, "unavailable");
        verify(outbox, never()).sent(id);
    }

    @Test
    @DisplayName("an unexpected error retries that message and the others still go")
    void unexpected() {
        when(links.targetOf("zerobound", user)).thenThrow(new IllegalStateException("db"));
        sender.sendDue();
        verify(outbox).retry(id, "error");
    }

    @Test
    @DisplayName("an inactive channel claims nothing; the public constructor builds")
    void off() {
        new TelegramSender(new TelegramBotProperties(false, "t", "", "", null, null, null), outbox, links, templates, api, limiter).sendDue();
        verify(outbox, never()).claim(20);
    }
}
