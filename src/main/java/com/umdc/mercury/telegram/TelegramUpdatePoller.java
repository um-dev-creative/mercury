package com.umdc.mercury.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Long polling for development and QA, where mercury is not reachable from the Internet (T-114). Only
 * the instance holding the lease polls (Telegram answers 409 when two do); it resumes after the last
 * update handled, so a restart neither loses nor repeats updates. In production the webhook is used.
 */
@Component
public class TelegramUpdatePoller {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramUpdatePoller.class);
    private static final Duration LEASE = Duration.ofSeconds(90);

    private final TelegramBotProperties properties;
    private final TelegramBotApi api;
    private final TelegramInbox inbox;
    private final TelegramUpdateHandler handler;
    private final Clock clock;
    private final String owner = UUID.randomUUID().toString();
    private Instant pausedUntil = Instant.MIN;
    private boolean webhookCleared;

    public TelegramUpdatePoller(TelegramBotProperties properties, TelegramBotApi api, TelegramInbox inbox, TelegramUpdateHandler handler) {
        this(properties, api, inbox, handler, Clock.systemUTC());
    }

    TelegramUpdatePoller(TelegramBotProperties properties, TelegramBotApi api, TelegramInbox inbox,
                         TelegramUpdateHandler handler, Clock clock) {
        this.properties = properties;
        this.api = api;
        this.inbox = inbox;
        this.handler = handler;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${umdc.mercury.telegram.poll-delay:1000}")
    public void poll() {
        final String application = properties.application();
        if (!properties.active() || !properties.polling() || application.isEmpty()
                || clock.instant().isBefore(pausedUntil) || !inbox.acquireLease(application, owner, LEASE)) {
            return;
        }
        if (!webhookCleared) {
            // getUpdates answers 409 while a webhook is set.
            webhookCleared = api.deleteWebhook() instanceof BotResult.Ok;
        }
        switch (api.getUpdates(inbox.nextOffset(application))) {
            case BotResult.Ok ok -> process(application, ok.result());
            case BotResult.RateLimited limited -> pause(limited.retryAfterSeconds());
            case BotResult.Rejected rejected -> {
                LOG.warn("Telegram getUpdates rejected ({}): check the token and that no webhook is set", rejected.status());
                forgetWebhookCleared();
                pause(30);
            }
            case BotResult.Forbidden forbidden -> pause(30);
            case BotResult.Unavailable unavailable -> pause(5);
        }
    }

    private void pause(long seconds) {
        pausedUntil = clock.instant().plusSeconds(seconds);
    }

    private void forgetWebhookCleared() {
        webhookCleared = false;
    }

    private void process(String application, Object result) {
        if (result instanceof List<?> updates) {
            for (Object update : updates) {
                if (update instanceof Map<?, ?> map) {
                    try {
                        handler.handle(application, map);
                    } catch (RuntimeException e) {
                        // One bad update must not stop the rest; the type only (no text, no token).
                        LOG.warn("Telegram update failed: {}", e.getClass().getSimpleName());
                    }
                }
            }
        }
    }
}
