package com.umdc.mercury.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Delivers the queued Telegram messages through the Bot API (T-115), outside any request. Rates are kept
 * (about 1/s per chat, 30/s per bot), a 429 waits its {@code retry_after}, a 403 blocks the link, and
 * a long text goes in parts of at most 4096 characters, resuming after the parts already sent. Message
 * texts and the token never reach a log.
 */
@Component
public class TelegramSender {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramSender.class);
    private static final int BATCH = 20;

    private final TelegramBotProperties properties;
    private final TelegramOutbox outbox;
    private final TelegramLinkService links;
    private final TelegramTemplateRenderer templates;
    private final TelegramBotApi api;
    private final TelegramRateLimiter limiter;
    private final Clock clock;

    @Autowired
    public TelegramSender(TelegramBotProperties properties, TelegramOutbox outbox, TelegramLinkService links,
                          TelegramTemplateRenderer templates, TelegramBotApi api, TelegramRateLimiter limiter) {
        this(properties, outbox, links, templates, api, limiter, Clock.systemUTC());
    }

    TelegramSender(TelegramBotProperties properties, TelegramOutbox outbox, TelegramLinkService links,
                   TelegramTemplateRenderer templates, TelegramBotApi api, TelegramRateLimiter limiter, Clock clock) {
        this.properties = properties;
        this.outbox = outbox;
        this.links = links;
        this.templates = templates;
        this.api = api;
        this.limiter = limiter;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${umdc.mercury.telegram.send-delay:1000}")
    public void sendDue() {
        if (!properties.active()) {
            return;
        }
        for (var message : outbox.claim(BATCH)) {
            try {
                deliver(message);
            } catch (RuntimeException e) {
                LOG.warn("Telegram delivery error: {}", e.getClass().getSimpleName());
                outbox.retry(message.id(), "error");
            }
        }
    }

    private void deliver(TelegramOutbox.Claimed message) {
        final var target = links.targetOf(message.application(), message.userId());
        if (target.isEmpty() || !target.get().active()) {
            outbox.blocked(message.id());
            return;
        }
        final long chat = target.get().chatId();
        if (!limiter.tryAcquire(chat)) {
            outbox.defer(message.id(), LocalDateTime.now(clock).plusSeconds(1));
            return;
        }
        final List<String> parts;
        try {
            parts = TelegramTextSplitter.split(templates.render(message.kind(), message.language(), message.params()));
        } catch (IllegalStateException e) {
            outbox.failed(message.id(), "template");
            return;
        }
        for (int i = message.partsSent(); i < parts.size(); i++) {
            if (!send(message, chat, parts.get(i))) {
                return;
            }
            outbox.partSent(message.id(), i + 1);
        }
        outbox.sent(message.id());
    }

    /** True when the part went out; otherwise the outcome has been recorded and the message is left. */
    private boolean send(TelegramOutbox.Claimed message, long chat, String text) {
        switch (api.sendMessage(chat, text, message.disableNotification())) {
            case BotResult.Ok ok -> {
                return true;
            }
            case BotResult.RateLimited limited -> outbox.defer(message.id(), LocalDateTime.now(clock).plusSeconds(limited.retryAfterSeconds()));
            case BotResult.Forbidden forbidden -> {
                links.markBlocked(message.application(), chat);
                outbox.blocked(message.id());
            }
            case BotResult.Rejected rejected -> outbox.failed(message.id(), "rejected-" + rejected.status());
            case BotResult.Unavailable unavailable -> outbox.retry(message.id(), "unavailable");
        }
        return false;
    }
}
