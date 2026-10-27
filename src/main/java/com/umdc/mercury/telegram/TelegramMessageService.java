package com.umdc.mercury.telegram;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Accepts {@code POST /api/v1/telegram/messages} (T-115): checks the user is linked and the template exists,
 * and queues the message. With an {@code Idempotency-Key} (scoped to the caller) the same request is accepted
 * once; the same key with another request is refused.
 */
@Service
public class TelegramMessageService {

    private final TelegramMessageRepository messages;
    private final TelegramLinkService links;
    private final TelegramTemplateRenderer templates;
    private final TelegramBotProperties properties;
    private final Clock clock;

    public sealed interface Result {
        record Accepted(UUID id, String status, boolean replayed) implements Result { }

        record NotLinked() implements Result { }

        record Blocked() implements Result { }
    }

    public TelegramMessageService(TelegramMessageRepository messages, TelegramLinkService links,
                                  TelegramTemplateRenderer templates, TelegramBotProperties properties) {
        this(messages, links, templates, properties, Clock.systemUTC());
    }

    TelegramMessageService(TelegramMessageRepository messages, TelegramLinkService links, TelegramTemplateRenderer templates,
                           TelegramBotProperties properties, Clock clock) {
        this.messages = messages;
        this.links = links;
        this.templates = templates;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @throws IllegalArgumentException unknown template or unsupported language (400)
     * @throws IllegalStateException    the key was used with a different request (422)
     */
    @Transactional
    public Result accept(String application, TelegramMessageRequest request, String idempotencyKey) {
        final String key = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.strip();
        final String language = request.language() == null || request.language().isBlank()
                ? TelegramLinkService.DEFAULT_LANGUAGE : request.language().strip().toLowerCase(java.util.Locale.ROOT);
        final Map<String, Object> params = request.params() == null ? Map.of() : request.params();
        final String hash = hash(request, language, params);

        if (key != null) {
            final var earlier = messages.findByApplicationAndIdempotencyKey(application, key);
            if (earlier.isPresent()) {
                return replay(earlier.get(), hash);
            }
        }
        if (!properties.languages().contains(language) || !templates.exists(request.kind(), language)) {
            throw new IllegalArgumentException("Unknown Telegram template or language");
        }
        final Optional<TelegramLinkService.Target> target = links.targetOf(application, request.userId());
        if (target.isEmpty()) {
            return new Result.NotLinked();
        }
        if (!target.get().active()) {
            return new Result.Blocked();
        }

        final LocalDateTime now = LocalDateTime.now(clock);
        final var entity = new TelegramMessageEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplication(application);
        entity.setUserId(request.userId());
        entity.setIdempotencyKey(key);
        entity.setRequestHash(hash);
        entity.setKind(request.kind());
        entity.setLanguage(language);
        entity.setParams(params);
        entity.setDisableNotification(Boolean.TRUE.equals(request.disableNotification()));
        entity.setStatus(TelegramMessageEntity.QUEUED);
        entity.setNextAttemptAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            messages.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same key raced: the other one won, answer with it.
            return key == null ? rethrow(e) : replay(messages.findByApplicationAndIdempotencyKey(application, key).orElseThrow(() -> e), hash);
        }
        return new Result.Accepted(entity.getId(), entity.getStatus(), false);
    }

    private static Result rethrow(DataIntegrityViolationException e) {
        throw e;
    }

    private static Result replay(TelegramMessageEntity first, String hash) {
        if (!first.getRequestHash().equals(hash)) {
            throw new IllegalStateException("Idempotency-Key was already used with a different request");
        }
        return new Result.Accepted(first.getId(), first.getStatus(), true);
    }

    /** State of a message {@code application} submitted. */
    @Transactional(readOnly = true)
    public Optional<TelegramMessageStatusResponse> status(UUID id, String application) {
        return messages.findByIdAndApplication(id, application)
                .map(m -> new TelegramMessageStatusResponse(m.getId(), m.getStatus(), m.getSentAt()));
    }

    /** SHA-256 over the request with the maps in key order, so equal requests hash equal. */
    static String hash(TelegramMessageRequest r, String language, Map<String, Object> params) {
        final String canonical = r.userId() + "|" + r.kind() + "|" + language + "|" + Boolean.TRUE.equals(r.disableNotification())
                + "|" + new TreeMap<>(params);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
