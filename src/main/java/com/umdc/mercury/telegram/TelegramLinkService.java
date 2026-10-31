package com.umdc.mercury.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Links a user of an application to a private Telegram chat (T-113, zerobound decision D-11).
 *
 * <p>The user opens {@code t.me/<bot>?start=<token>}; the token is 32 random bytes in base64url (43
 * characters, valid for Telegram's {@code start}), stored only as its SHA-256, single use, valid
 * {@link #TOKEN_TTL}. One user has at most one chat and a chat at most one user per application:
 * linking again replaces. The {@code application} is the caller's login-client alias, so a caller never
 * sees another's rows, and the {@code chat_id} is never returned by this service's API.</p>
 */
@Service
public class TelegramLinkService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    static final String DEFAULT_LANGUAGE = "es";
    private static final int TOKEN_BYTES = 32;

    private final TelegramLinkRepository links;
    private final TelegramLinkTokenRepository tokens;
    private final TelegramBotProperties properties;
    private final TelegramBotIdentity bot;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public enum Status { NONE, PENDING, ACTIVE, BLOCKED }

    public record Created(String deepLink, LocalDateTime expiresAt) { }

    public record LinkStatus(Status status, LocalDateTime linkedAt, String language) { }

    /** Result of a {@code /start <token>}: {@code replacedChatId} is the user's previous chat, if any, to tell it. */
    public sealed interface Redemption {
        record Linked(UUID userId, String language, Long replacedChatId) implements Redemption { }

        record Invalid() implements Redemption { }
    }

    @Autowired
    public TelegramLinkService(TelegramLinkRepository links, TelegramLinkTokenRepository tokens,
                               TelegramBotProperties properties, TelegramBotIdentity bot) {
        this(links, tokens, properties, bot, Clock.systemUTC());
    }

    TelegramLinkService(TelegramLinkRepository links, TelegramLinkTokenRepository tokens,
                        TelegramBotProperties properties, TelegramBotIdentity bot, Clock clock) {
        this.links = links;
        this.tokens = tokens;
        this.properties = properties;
        this.bot = bot;
        this.clock = clock;
    }

    /** A fresh deep link; earlier tokens of the user stop working. 503 when the bot is off or unreachable. */
    @Transactional
    public Created createLink(String application, UUID userId, String language) {
        final String lang = resolveLanguage(language);
        final String username = bot.username().orElseThrow(
                () -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram channel unavailable"));
        final LocalDateTime now = now();
        tokens.deleteByExpiresAtBefore(now);
        tokens.deleteByApplicationAndUserId(application, userId);

        final byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        final var entity = new TelegramLinkTokenEntity();
        entity.setTokenHash(hash(token));
        entity.setApplication(application);
        entity.setUserId(userId);
        entity.setLanguage(lang);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plus(TOKEN_TTL));
        tokens.save(entity);
        return new Created("https://t.me/" + username + "?start=" + token, entity.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public LinkStatus status(String application, UUID userId) {
        return links.findByApplicationAndUserId(application, userId)
                .map(l -> new LinkStatus(Status.valueOf(l.getStatus()), l.getLinkedAt(), l.getLanguage()))
                .orElseGet(() -> tokens.existsByApplicationAndUserIdAndExpiresAtAfter(application, userId, now())
                        ? new LinkStatus(Status.PENDING, null, null)
                        : new LinkStatus(Status.NONE, null, null));
    }

    /** Called by the update input (T-114) when a private chat sends {@code /start <token>}. */
    @Transactional
    public Redemption redeem(String application, String rawToken, long chatId) {
        if (rawToken == null || rawToken.isBlank()) {
            return new Redemption.Invalid();
        }
        final String hash = hash(rawToken.strip());
        final var token = tokens.findById(hash).filter(t -> t.getApplication().equals(application)).orElse(null);
        if (token == null || tokens.consume(hash) != 1 || !token.getExpiresAt().isAfter(now())) {
            return new Redemption.Invalid();
        }
        final LocalDateTime now = now();
        links.findByApplicationAndChatId(application, chatId)
                .filter(other -> !other.getUserId().equals(token.getUserId()))
                .ifPresent(other -> {
                    links.delete(other);
                    links.flush();
                });
        final var existing = links.findByApplicationAndUserId(application, token.getUserId());
        final Long replaced = existing.map(TelegramLinkEntity::getChatId).filter(c -> c != chatId).orElse(null);
        final var link = existing.orElseGet(TelegramLinkEntity::new);
        if (existing.isEmpty()) {
            link.setId(UUID.randomUUID());
            link.setApplication(application);
            link.setUserId(token.getUserId());
        }
        link.setChatId(chatId);
        link.setStatus(TelegramLinkEntity.ACTIVE);
        link.setLanguage(token.getLanguage());
        link.setLinkedAt(now);
        link.setUpdatedAt(now);
        links.saveAndFlush(link);
        return new Redemption.Linked(token.getUserId(), token.getLanguage(), replaced);
    }

    /** Removes the link and any pending token (idempotent); the chat that was active, if any, so the caller can say goodbye. */
    @Transactional
    public Optional<Long> unlink(String application, UUID userId) {
        tokens.deleteByApplicationAndUserId(application, userId);
        final var link = links.findByApplicationAndUserId(application, userId);
        link.ifPresent(links::delete);
        return link.filter(l -> TelegramLinkEntity.ACTIVE.equals(l.getStatus())).map(TelegramLinkEntity::getChatId);
    }

    /** The user blocked the bot (my_chat_member kicked, or a 403 on send): sends stop until a new link. */
    @Transactional
    public void markBlocked(String application, long chatId) {
        links.findByApplicationAndChatId(application, chatId).ifPresent(l -> {
            l.setStatus(TelegramLinkEntity.BLOCKED);
            l.setUpdatedAt(now());
            links.save(l);
        });
    }

    private String resolveLanguage(String language) {
        if (language == null || language.isBlank()) {
            return properties.languages().contains(DEFAULT_LANGUAGE) ? DEFAULT_LANGUAGE : properties.languages().getFirst();
        }
        final String lang = language.strip().toLowerCase(java.util.Locale.ROOT);
        if (!properties.languages().contains(lang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language");
        }
        return lang;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
