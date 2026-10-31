package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram long polling")
class TelegramUpdatePollerTest {

    private final TelegramBotApi api = mock(TelegramBotApi.class);
    private final TelegramInbox inbox = mock(TelegramInbox.class);
    private final TelegramUpdateHandler handler = mock(TelegramUpdateHandler.class);
    private Instant now = Instant.parse("2026-11-02T10:00:00Z");
    private final Clock clock = new Clock() {
        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    private TelegramUpdatePoller poller(boolean enabled, String token, String application, String mode) {
        final var props = new TelegramBotProperties(enabled, token, "", "", null, null, null, application, mode, "");
        when(inbox.acquireLease(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inbox.nextOffset(anyString())).thenReturn(null);
        when(api.deleteWebhook()).thenReturn(new BotResult.Ok(true));
        return new TelegramUpdatePoller(props, api, inbox, handler, clock);
    }

    @Test
    @DisplayName("polls from the next offset, hands each update to the handler and clears the webhook once")
    void polls() {
        final var poller = poller(true, "t", "zerobound", "poll");
        when(inbox.nextOffset("zerobound")).thenReturn(8L);
        when(api.getUpdates(8L)).thenReturn(new BotResult.Ok(List.of(Map.of("update_id", 8), Map.of("update_id", 9), "junk")));
        poller.poll();
        poller.poll();
        verify(handler, times(2)).handle(eq("zerobound"), eq(Map.of("update_id", 8)));
        verify(handler, times(2)).handle(eq("zerobound"), eq(Map.of("update_id", 9)));
        verify(api).deleteWebhook();
    }

    @Test
    @DisplayName("one failing update does not stop the next ones")
    void badUpdate() {
        final var poller = poller(true, "t", "zerobound", "poll");
        doThrow(new IllegalStateException("boom")).when(handler).handle(eq("zerobound"), eq(Map.of("update_id", 1)));
        when(api.getUpdates(null)).thenReturn(new BotResult.Ok(List.of(Map.of("update_id", 1), Map.of("update_id", 2))));
        poller.poll();
        verify(handler).handle("zerobound", Map.of("update_id", 2));
    }

    @Test
    @DisplayName("does nothing when off, in webhook mode, without application, or without the lease")
    void idle() {
        poller(false, "t", "zerobound", "poll").poll();
        poller(true, "", "zerobound", "poll").poll();
        poller(true, "t", "zerobound", "webhook").poll();
        poller(true, "t", "", "poll").poll();
        final var noLease = poller(true, "t", "zerobound", "poll");
        when(inbox.acquireLease(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        noLease.poll();
        verify(api, never()).getUpdates(any());
    }

    @Test
    @DisplayName("429, 4xx and outages pause the polling for a while")
    void pauses() {
        final var poller = poller(true, "t", "zerobound", "poll");
        when(api.getUpdates(any())).thenReturn(new BotResult.RateLimited(20), new BotResult.Ok(List.of()),
                new BotResult.Rejected(409, "conflict"), new BotResult.Forbidden("x"), new BotResult.Unavailable("down"),
                new BotResult.Ok(List.of()));
        poller.poll();
        poller.poll();
        verify(api, times(1)).getUpdates(any());
        now = now.plusSeconds(21);
        poller.poll();
        verify(api, times(2)).getUpdates(any());
        now = now.plusSeconds(31);
        poller.poll();
        now = now.plusSeconds(31);
        poller.poll();
        now = now.plusSeconds(31);
        poller.poll();
        verify(api, times(5)).getUpdates(any());
        verify(api, times(2)).deleteWebhook();
    }

    @Test
    @DisplayName("a result that is not a list is ignored")
    void oddResult() {
        final var poller = poller(true, "t", "zerobound", "poll");
        when(api.getUpdates(any())).thenReturn(new BotResult.Ok(true));
        poller.poll();
        verify(handler, never()).handle(anyString(), any());
    }
}
