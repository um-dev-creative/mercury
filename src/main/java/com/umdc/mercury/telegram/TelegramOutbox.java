package com.umdc.mercury.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The delivery queue's state changes (T-115), each in its own short transaction so that nothing is held
 * open while the Bot API is called. A message is claimed by pushing its {@code next_attempt_at} out by a
 * lease: if the instance dies mid-send it becomes due again.
 */
@Service
public class TelegramOutbox {

    static final Duration LEASE = Duration.ofMinutes(2);
    static final Duration[] BACKOFF = {Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(12)};
    static final int MAX_ATTEMPTS = 6;
    static final Duration MAX_AGE = Duration.ofHours(48);

    private final TelegramMessageRepository messages;
    private final Clock clock;

    public record Claimed(UUID id, String application, UUID userId, String kind, String language, Map<String, Object> params,
                          boolean disableNotification, int attempts, int partsSent, LocalDateTime createdAt) { }

    @Autowired
    public TelegramOutbox(TelegramMessageRepository messages) {
        this(messages, Clock.systemUTC());
    }

    TelegramOutbox(TelegramMessageRepository messages, Clock clock) {
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional
    public List<Claimed> claim(int limit) {
        final LocalDateTime now = LocalDateTime.now(clock);
        return messages.lockDue(now, limit).stream().map(m -> {
            m.setNextAttemptAt(now.plus(LEASE));
            m.setUpdatedAt(now);
            return new Claimed(m.getId(), m.getApplication(), m.getUserId(), m.getKind(), m.getLanguage(), m.getParams(),
                    m.isDisableNotification(), m.getAttempts(), m.getPartsSent(), m.getCreatedAt());
        }).toList();
    }

    @Transactional
    public void partSent(UUID id, int partsSent) {
        update(id, m -> m.setPartsSent(partsSent));
    }

    @Transactional
    public void sent(UUID id) {
        final LocalDateTime now = LocalDateTime.now(clock);
        update(id, m -> {
            m.setStatus(TelegramMessageEntity.SENT);
            m.setSentAt(now);
            m.setLastError(null);
            m.setParams(Map.of());
        });
    }

    /** The chat is gone or blocked the bot: final. */
    @Transactional
    public void blocked(UUID id) {
        update(id, m -> finish(m, TelegramMessageEntity.BLOCKED, "blocked"));
    }

    /** Final failure (bad request, missing template): retrying does not help. */
    @Transactional
    public void failed(UUID id, String reason) {
        update(id, m -> finish(m, TelegramMessageEntity.FAILED, reason));
    }

    /** Not now, without spending an attempt (rate limit): due again at {@code at}. */
    @Transactional
    public void defer(UUID id, LocalDateTime at) {
        update(id, m -> m.setNextAttemptAt(at));
    }

    /** A failure worth retrying: backoff 1 min, 5 min, 30 min, 2 h, 12 h; the 6th failure or 48 h ends it. */
    @Transactional
    public void retry(UUID id, String reason) {
        final LocalDateTime now = LocalDateTime.now(clock);
        update(id, m -> {
            final int attempts = m.getAttempts() + 1;
            m.setAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS || m.getCreatedAt().plus(MAX_AGE).isBefore(now)) {
                finish(m, TelegramMessageEntity.FAILED, reason);
            } else {
                m.setLastError(reason);
                m.setNextAttemptAt(now.plus(BACKOFF[attempts - 1]));
            }
        });
    }

    private static void finish(TelegramMessageEntity m, String status, String reason) {
        m.setStatus(status);
        m.setLastError(reason);
        m.setParams(Map.of());
    }

    private void update(UUID id, java.util.function.Consumer<TelegramMessageEntity> change) {
        messages.findById(id).ifPresent(m -> {
            change.accept(m);
            m.setUpdatedAt(LocalDateTime.now(clock));
            messages.save(m);
        });
    }
}
