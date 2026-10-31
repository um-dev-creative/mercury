package com.umdc.mercury.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Asks the Bot API {@code getMe} once at start-up (T-111) so a wrong token shows in the log, not on the
 * first user's message. A failure never stops mercury: email and the rest keep working. The log never
 * carries the token (it is in the URL, so the exception is reduced to its class and status).
 */
@Component
public class TelegramBotIdentity {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramBotIdentity.class);

    private final TelegramBotProperties properties;
    private final TelegramBotApi api;
    private final AtomicReference<String> cached = new AtomicReference<>();

    public TelegramBotIdentity(TelegramBotProperties properties, TelegramBotApi api) {
        this.properties = properties;
        this.api = api;
    }

    @EventListener(ApplicationReadyEvent.class)
    void logAtStartup() {
        if (properties.active()) {
            fetchUsername().ifPresentOrElse(
                    u -> LOG.info("Telegram bot @{} reachable", u),
                    () -> LOG.warn("Telegram getMe failed: check the bot token and the connection"));
        } else if (properties.enabled()) {
            LOG.warn("Telegram channel enabled but TELEGRAM_BOT_TOKEN is empty: it stays off");
        } else {
            LOG.info("Telegram channel disabled");
        }
    }

    /** The username, remembered after the first success (it never changes for a token). */
    public Optional<String> username() {
        if (cached.get() == null) {
            fetchUsername().ifPresent(cached::set);
        }
        return Optional.ofNullable(cached.get());
    }

    /** The bot's {@code username} from {@code getMe}, or empty when the call fails (never throws). */
    public Optional<String> fetchUsername() {
        if (api.getMe() instanceof BotResult.Ok ok && ok.result() instanceof Map<?, ?> bot
                && bot.get("username") instanceof String username) {
            return Optional.of(username);
        }
        return Optional.empty();
    }
}
