package com.umdc.mercury.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Telegram bot settings bound at {@code umdc.mercury.telegram.*} (T-111, zerobound decision D-11).
 *
 * <p>{@code botToken} and {@code webhookSecret} are Vault-owned and empty by default: with an empty
 * token the channel stays off. The token travels in the Bot API URL, so neither value may reach a log
 * line, an exception message or {@link #toString()}.</p>
 */
@ConfigurationProperties(prefix = "umdc.mercury.telegram")
public record TelegramBotProperties(
        boolean enabled,
        String botToken,
        String webhookSecret,
        String apiBaseUrl,
        List<String> languages,
        Duration connectTimeout,
        Duration readTimeout) {

    public TelegramBotProperties {
        botToken = botToken == null ? "" : botToken.strip();
        webhookSecret = webhookSecret == null ? "" : webhookSecret.strip();
        apiBaseUrl = apiBaseUrl == null || apiBaseUrl.isBlank() ? "https://api.telegram.org" : apiBaseUrl.strip();
        languages = languages == null || languages.isEmpty() ? List.of("es", "en") : List.copyOf(languages);
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
    }

    /** True when the channel is switched on and has a token to talk to the Bot API. */
    public boolean active() {
        return enabled && !botToken.isEmpty();
    }

    @Override
    public String toString() {
        return "TelegramBotProperties[enabled=" + enabled + ", botToken=" + (botToken.isEmpty() ? "" : "***")
                + ", webhookSecret=" + (webhookSecret.isEmpty() ? "" : "***") + ", apiBaseUrl=" + apiBaseUrl
                + ", languages=" + languages + "]";
    }
}
