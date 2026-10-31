package com.umdc.mercury.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.List;

/**
 * Telegram bot settings bound at {@code umdc.mercury.telegram.*} (T-111, zerobound decision D-11).
 *
 * <p>{@code application} is the login-client alias whose users the bot serves (links are stored under it); {@code mode} is
 * {@code poll} (dev/QA) or {@code webhook} (production, posts to {@code webhookUrl}).
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
        Duration readTimeout,
        String application,
        String mode,
        String webhookUrl) {

    public static final String MODE_POLL = "poll";
    public static final String MODE_WEBHOOK = "webhook";

    /** Without the update input settings: the channel only sends and links (tests, and callers that do not receive). */
    public TelegramBotProperties(boolean enabled, String botToken, String webhookSecret, String apiBaseUrl,
                                 List<String> languages, Duration connectTimeout, Duration readTimeout) {
        this(enabled, botToken, webhookSecret, apiBaseUrl, languages, connectTimeout, readTimeout, null, null, null);
    }

    @ConstructorBinding
    public TelegramBotProperties {
        botToken = botToken == null ? "" : botToken.strip();
        webhookSecret = webhookSecret == null ? "" : webhookSecret.strip();
        apiBaseUrl = apiBaseUrl == null || apiBaseUrl.isBlank() ? "https://api.telegram.org" : apiBaseUrl.strip();
        languages = languages == null || languages.isEmpty() ? List.of("es", "en") : List.copyOf(languages);
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
        application = application == null ? "" : application.strip();
        mode = MODE_WEBHOOK.equalsIgnoreCase(mode == null ? "" : mode.strip()) ? MODE_WEBHOOK : MODE_POLL;
        webhookUrl = webhookUrl == null ? "" : webhookUrl.strip();
    }

    /** True when the channel is switched on and has a token to talk to the Bot API. */
    public boolean active() {
        return enabled && !botToken.isEmpty();
    }

    /** True when updates are fetched by long polling ({@code getUpdates}); false when Telegram posts them to the webhook. */
    public boolean polling() {
        return MODE_POLL.equals(mode);
    }

    @Override
    public String toString() {
        return "TelegramBotProperties[enabled=" + enabled + ", botToken=" + (botToken.isEmpty() ? "" : "***")
                + ", webhookSecret=" + (webhookSecret.isEmpty() ? "" : "***") + ", application=" + application + ", mode=" + mode + ", apiBaseUrl=" + apiBaseUrl
                + ", languages=" + languages + "]";
    }
}
