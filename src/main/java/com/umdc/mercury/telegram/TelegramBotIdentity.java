package com.umdc.mercury.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;

/**
 * Asks the Bot API {@code getMe} once at start-up (T-111) so a wrong token shows in the log, not on the
 * first user's message. A failure never stops mercury: email and the rest keep working. The log never
 * carries the token (it is in the URL, so the exception is reduced to its class and status).
 */
@Component
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TelegramBotIdentity {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramBotIdentity.class);

    private final TelegramBotProperties properties;

    public TelegramBotIdentity(TelegramBotProperties properties) {
        this.properties = properties;
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

    /** The bot's {@code username} from {@code getMe}, or empty when the call fails (never throws). */
    public Optional<String> fetchUsername() {
        if (!properties.active()) {
            return Optional.empty();
        }
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build()) {
            final var factory = new JdkClientHttpRequestFactory(http);
            factory.setReadTimeout(properties.readTimeout());
            final Map<?, ?> body = RestClient.builder().requestFactory(factory).build().get()
                    .uri(properties.apiBaseUrl() + "/bot" + properties.botToken() + "/getMe")
                    .retrieve().body(Map.class);
            if (body != null && Boolean.TRUE.equals(body.get("ok")) && body.get("result") instanceof Map<?, ?> bot
                    && bot.get("username") instanceof String username) {
                return Optional.of(username);
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.debug("Telegram getMe error: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
