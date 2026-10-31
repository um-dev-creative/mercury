package com.umdc.mercury.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram links")
class TelegramLinkServiceTest {

    private static final String APP = "zerobound";
    private static final Instant T0 = Instant.parse("2026-11-02T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(T0, ZoneOffset.UTC);

    private final TelegramLinkRepository links = mock(TelegramLinkRepository.class);
    private final TelegramLinkTokenRepository tokens = mock(TelegramLinkTokenRepository.class);
    private final TelegramBotIdentity bot = mock(TelegramBotIdentity.class);
    private final UUID user = UUID.randomUUID();
    private TelegramLinkService service;

    @BeforeEach
    void setUp() {
        when(bot.username()).thenReturn(Optional.of("zerobound_bot"));
        service = new TelegramLinkService(links, tokens, new TelegramBotProperties(true, "t", "", "", List.of("es", "en"), null, null),
                bot, Clock.fixed(T0, ZoneOffset.UTC));
    }

    private TelegramLinkTokenEntity createdToken(String[] rawOut) {
        final var created = service.createLink(APP, user, "EN");
        final var captor = ArgumentCaptor.forClass(TelegramLinkTokenEntity.class);
        verify(tokens).save(captor.capture());
        rawOut[0] = created.deepLink().substring(created.deepLink().indexOf("start=") + 6);
        return captor.getValue();
    }

    @Test
    @DisplayName("createLink: 43-character base64url token, stored only as its hash, valid 10 minutes, language normalised")
    void createLink() {
        final var raw = new String[1];
        final var token = createdToken(raw);
        assertTrue(raw[0].matches("[A-Za-z0-9_-]{43}"));
        assertEquals(TelegramLinkService.hash(raw[0]), token.getTokenHash());
        assertFalse(token.getTokenHash().contains(raw[0]));
        assertEquals(NOW.plus(Duration.ofMinutes(10)), token.getExpiresAt());
        assertEquals("en", token.getLanguage());
        assertEquals(APP, token.getApplication());
        verify(tokens).deleteByApplicationAndUserId(APP, user);
        verify(tokens).deleteByExpiresAtBefore(NOW);
    }

    @Test
    @DisplayName("createLink: link points at the bot; default language es; an unsupported language is a 400; bot down is a 503")
    void createLinkRules() {
        assertTrue(service.createLink(APP, user, null).deepLink().startsWith("https://t.me/zerobound_bot?start="));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.createLink(APP, user, "fr")).getStatusCode().value());
        when(bot.username()).thenReturn(Optional.empty());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.createLink(APP, user, "es")).getStatusCode().value());
    }

    @Test
    @DisplayName("two links for the same user are different tokens")
    void tokensAreRandom() {
        final var a = service.createLink(APP, user, "es").deepLink();
        final var b = service.createLink(APP, user, "es").deepLink();
        assertFalse(a.equals(b));
    }

    @Test
    @DisplayName("status: link row wins, else a live token is PENDING, else NONE")
    void status() {
        assertEquals(TelegramLinkService.Status.NONE, service.status(APP, user).status());
        when(tokens.existsByApplicationAndUserIdAndExpiresAtAfter(APP, user, NOW)).thenReturn(true);
        assertEquals(TelegramLinkService.Status.PENDING, service.status(APP, user).status());
        final var link = link(5L, TelegramLinkEntity.BLOCKED);
        when(links.findByApplicationAndUserId(APP, user)).thenReturn(Optional.of(link));
        final var status = service.status(APP, user);
        assertEquals(TelegramLinkService.Status.BLOCKED, status.status());
        assertEquals("es", status.language());
    }

    private TelegramLinkEntity link(long chat, String status) {
        final var l = new TelegramLinkEntity();
        l.setId(UUID.randomUUID());
        l.setApplication(APP);
        l.setUserId(user);
        l.setChatId(chat);
        l.setStatus(status);
        l.setLanguage("es");
        l.setLinkedAt(NOW.minusDays(1));
        return l;
    }

    private TelegramLinkTokenEntity token(LocalDateTime expires, String app) {
        final var t = new TelegramLinkTokenEntity();
        t.setTokenHash(TelegramLinkService.hash("tok"));
        t.setApplication(app);
        t.setUserId(user);
        t.setLanguage("en");
        t.setExpiresAt(expires);
        return t;
    }

    @Test
    @DisplayName("redeem: a valid token links the chat, consumes the token and reports no replaced chat")
    void redeemNew() {
        when(tokens.findById(TelegramLinkService.hash("tok"))).thenReturn(Optional.of(token(NOW.plusMinutes(5), APP)));
        when(tokens.consume(anyString())).thenReturn(1);
        final var result = assertInstanceOf(TelegramLinkService.Redemption.Linked.class, service.redeem(APP, " tok ", 99L));
        assertEquals(user, result.userId());
        assertEquals("en", result.language());
        assertNull(result.replacedChatId());
        final var captor = ArgumentCaptor.forClass(TelegramLinkEntity.class);
        verify(links).saveAndFlush(captor.capture());
        assertEquals(99L, captor.getValue().getChatId());
        assertEquals(TelegramLinkEntity.ACTIVE, captor.getValue().getStatus());
        assertEquals(NOW, captor.getValue().getLinkedAt());
    }

    @Test
    @DisplayName("redeem: relinking replaces the user's chat and reports the old one; the same chat reports none")
    void redeemReplaces() {
        when(tokens.findById(anyString())).thenReturn(Optional.of(token(NOW.plusMinutes(5), APP)));
        when(tokens.consume(anyString())).thenReturn(1);
        when(links.findByApplicationAndUserId(APP, user)).thenReturn(Optional.of(link(11L, TelegramLinkEntity.BLOCKED)));
        assertEquals(11L, assertInstanceOf(TelegramLinkService.Redemption.Linked.class, service.redeem(APP, "tok", 22L)).replacedChatId());
        when(links.findByApplicationAndUserId(APP, user)).thenReturn(Optional.of(link(22L, TelegramLinkEntity.BLOCKED)));
        assertNull(assertInstanceOf(TelegramLinkService.Redemption.Linked.class, service.redeem(APP, "tok", 22L)).replacedChatId());
    }

    @Test
    @DisplayName("redeem: a chat already linked to another user of the application is taken over (one chat, one user)")
    void redeemTakesOverChat() {
        when(tokens.findById(anyString())).thenReturn(Optional.of(token(NOW.plusMinutes(5), APP)));
        when(tokens.consume(anyString())).thenReturn(1);
        final var other = link(33L, TelegramLinkEntity.ACTIVE);
        other.setUserId(UUID.randomUUID());
        when(links.findByApplicationAndChatId(APP, 33L)).thenReturn(Optional.of(other));
        service.redeem(APP, "tok", 33L);
        verify(links).delete(other);
    }

    @Test
    @DisplayName("redeem: unknown, blank, expired, other-application or already consumed tokens are Invalid and link nothing")
    void redeemInvalid() {
        assertInstanceOf(TelegramLinkService.Redemption.Invalid.class, service.redeem(APP, null, 1L));
        assertInstanceOf(TelegramLinkService.Redemption.Invalid.class, service.redeem(APP, "  ", 1L));
        assertInstanceOf(TelegramLinkService.Redemption.Invalid.class, service.redeem(APP, "nope", 1L));
        when(tokens.findById(anyString())).thenReturn(Optional.of(token(NOW.minusSeconds(1), APP)));
        when(tokens.consume(anyString())).thenReturn(1);
        assertInstanceOf(TelegramLinkService.Redemption.Invalid.class, service.redeem(APP, "tok", 1L));
        when(tokens.findById(anyString())).thenReturn(Optional.of(token(NOW.plusMinutes(5), "other-app")));
        assertInstanceOf(TelegramLinkService.Redemption.Invalid.class, service.redeem(APP, "tok", 1L));
        when(tokens.findById(anyString())).thenReturn(Optional.of(token(NOW.plusMinutes(5), APP)));
        when(tokens.consume(anyString())).thenReturn(0);
        assertInstanceOf(TelegramLinkService.Redemption.Invalid.class, service.redeem(APP, "tok", 1L));
        verify(links, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("unlink: removes link and tokens, returns the chat only when it was ACTIVE, and is idempotent")
    void unlink() {
        assertTrue(service.unlink(APP, user).isEmpty());
        final var active = link(7L, TelegramLinkEntity.ACTIVE);
        when(links.findByApplicationAndUserId(APP, user)).thenReturn(Optional.of(active));
        assertEquals(Optional.of(7L), service.unlink(APP, user));
        verify(links).delete(active);
        when(links.findByApplicationAndUserId(APP, user)).thenReturn(Optional.of(link(8L, TelegramLinkEntity.BLOCKED)));
        assertTrue(service.unlink(APP, user).isEmpty());
        verify(tokens, org.mockito.Mockito.times(3)).deleteByApplicationAndUserId(APP, user);
    }

    @Test
    @DisplayName("markBlocked: flips the link of that chat to BLOCKED; an unknown chat is ignored")
    void markBlocked() {
        service.markBlocked(APP, 1L);
        verify(links, never()).save(any());
        final var l = link(2L, TelegramLinkEntity.ACTIVE);
        when(links.findByApplicationAndChatId(APP, 2L)).thenReturn(Optional.of(l));
        service.markBlocked(APP, 2L);
        assertEquals(TelegramLinkEntity.BLOCKED, l.getStatus());
        assertEquals(NOW, l.getUpdatedAt());
    }

    @Test
    @DisplayName("public constructor uses the system clock")
    void publicConstructor() {
        final var s = new TelegramLinkService(links, tokens, new TelegramBotProperties(true, "t", "", "", null, null, null), bot);
        assertTrue(s.createLink(APP, user, null).expiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));
    }
}
