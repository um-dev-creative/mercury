package com.umdc.mercury.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram update handler")
class TelegramUpdateHandlerTest {

    private static final String APP = "zerobound";

    private final TelegramInbox inbox = mock(TelegramInbox.class);
    private final TelegramLinkService links = mock(TelegramLinkService.class);
    private final TelegramBotApi api = mock(TelegramBotApi.class);
    private TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TelegramUpdateHandler(inbox, links, api);
        when(inbox.firstTime(eq(APP), anyLong())).thenReturn(true);
        when(api.sendMessage(anyLong(), anyString(), anyBoolean())).thenReturn(new BotResult.Ok(Map.of()));
        when(links.statusOfChat(eq(APP), anyLong())).thenReturn(Optional.empty());
    }

    private static Map<String, Object> message(String text, String chatType, String langCode) {
        return Map.of("update_id", 10, "message", Map.of("text", text, "chat", Map.of("id", 77, "type", chatType),
                "from", Map.of("language_code", langCode)));
    }

    private String sent() {
        final var text = ArgumentCaptor.forClass(String.class);
        verify(api).sendMessage(eq(77L), text.capture(), eq(false));
        return text.getValue();
    }

    @Test
    @DisplayName("/start <token> links the chat and confirms in the link's language")
    void startLinks() {
        when(links.redeem(APP, "tok", 77L)).thenReturn(new TelegramLinkService.Redemption.Linked(UUID.randomUUID(), "en", null));
        handler.handle(APP, message("/start tok", "private", "es"));
        assertTrue(sent().startsWith("✅ Done!"));
    }

    @Test
    @DisplayName("/start <token> that replaces another chat also tells the old one")
    void startReplaces() {
        when(links.redeem(APP, "tok", 77L)).thenReturn(new TelegramLinkService.Redemption.Linked(UUID.randomUUID(), "es", 55L));
        handler.handle(APP, message("/start tok", "private", "es"));
        verify(api).sendMessage(eq(55L), anyString(), eq(false));
        verify(api).sendMessage(eq(77L), anyString(), eq(false));
    }

    @Test
    @DisplayName("/start with a bad token says how to link again; without a token it explains how")
    void startInvalidOrBare() {
        when(links.redeem(APP, "bad", 77L)).thenReturn(new TelegramLinkService.Redemption.Invalid());
        handler.handle(APP, message("/start bad", "private", "en-US"));
        assertTrue(sent().contains("invalid or has expired"));
    }

    @Test
    @DisplayName("/start alone explains how to link and links nothing")
    void startBare() {
        handler.handle(APP, message("/start", "private", "es"));
        assertTrue(sent().contains("Conectar Telegram"));
        verify(links, never()).redeem(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("/ayuda and /help answer the help text; the @bot suffix and case are ignored")
    void help() {
        handler.handle(APP, message("/AYUDA@zerobound_bot", "private", "es"));
        assertTrue(sent().contains("/desvincular"));
    }

    @Test
    @DisplayName("/estado says linked, paused or not linked")
    void estado() {
        final var linkedStatus = new TelegramLinkService.LinkStatus(TelegramLinkService.Status.ACTIVE, LocalDateTime.now(), "es");
        when(links.statusOfChat(APP, 77L)).thenReturn(Optional.of(linkedStatus));
        handler.handle(APP, message("/estado", "private", "es"));
        assertTrue(sent().contains("vinculado a tu cuenta"));
    }

    @Test
    @DisplayName("/status of a blocked chat and of an unknown chat")
    void statusOtherStates() {
        when(links.statusOfChat(APP, 77L)).thenReturn(Optional.of(
                new TelegramLinkService.LinkStatus(TelegramLinkService.Status.BLOCKED, LocalDateTime.now(), "en")));
        handler.handle(APP, message("/status", "private", "en"));
        assertTrue(sent().contains("paused"));
    }

    @Test
    @DisplayName("/desvincular unlinks this chat and says so; an unlinked chat is told how to link")
    void unlink() {
        when(links.unlinkChat(APP, 77L)).thenReturn(true);
        handler.handle(APP, message("/desvincular", "private", "es"));
        assertTrue(sent().startsWith("Desvinculado"));
    }

    @Test
    @DisplayName("/unlink on a chat with no link points at the app")
    void unlinkNothing() {
        handler.handle(APP, message("/unlink", "private", "en"));
        assertTrue(sent().contains("Connect Telegram"));
    }

    @Test
    @DisplayName("groups, plain text, unknown commands and updates without text are ignored")
    void ignored() {
        handler.handle(APP, message("/start tok", "group", "es"));
        handler.handle(APP, message("hello", "private", "es"));
        handler.handle(APP, message("/nonsense", "private", "es"));
        handler.handle(APP, Map.of("update_id", 11, "message", Map.of("chat", Map.of("id", 1, "type", "private"))));
        handler.handle(APP, Map.of("update_id", 12, "edited_message", Map.of()));
        verify(api, never()).sendMessage(anyLong(), anyString(), anyBoolean());
        verify(links, never()).redeem(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("an update id seen before, or without an id, is not handled twice")
    void replays() {
        when(inbox.firstTime(APP, 10L)).thenReturn(false);
        handler.handle(APP, message("/ayuda", "private", "es"));
        handler.handle(APP, Map.of("message", Map.of()));
        verify(api, never()).sendMessage(anyLong(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("my_chat_member kicked or left marks the link BLOCKED; other states and groups do nothing")
    void blocked() {
        handler.handle(APP, Map.of("update_id", 1, "my_chat_member",
                Map.of("chat", Map.of("id", 77, "type", "private"), "new_chat_member", Map.of("status", "kicked"))));
        handler.handle(APP, Map.of("update_id", 2, "my_chat_member",
                Map.of("chat", Map.of("id", 78, "type", "private"), "new_chat_member", Map.of("status", "left"))));
        handler.handle(APP, Map.of("update_id", 3, "my_chat_member",
                Map.of("chat", Map.of("id", 79, "type", "private"), "new_chat_member", Map.of("status", "member"))));
        handler.handle(APP, Map.of("update_id", 4, "my_chat_member",
                Map.of("chat", Map.of("id", 80, "type", "group"), "new_chat_member", Map.of("status", "kicked"))));
        verify(links).markBlocked(APP, 77L);
        verify(links).markBlocked(APP, 78L);
        verify(links, never()).markBlocked(APP, 79L);
        verify(links, never()).markBlocked(APP, 80L);
    }

    @Test
    @DisplayName("a 403 on the reply marks the chat blocked")
    void replyForbidden() {
        when(api.sendMessage(anyLong(), anyString(), anyBoolean())).thenReturn(new BotResult.Forbidden("blocked"));
        handler.handle(APP, message("/ayuda", "private", "es"));
        verify(links).markBlocked(APP, 77L);
    }
}
