package com.umdc.mercury.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client of the Telegram Bot API (T-112): {@code getMe}, {@code sendMessage}, {@code getUpdates},
 * {@code setWebhook} and {@code deleteWebhook}.
 *
 * <p>The token is part of the URL, so an HTTP library's exception text would leak it: every failure is
 * caught here and reduced to a {@link BotResult}, and only the exception class is ever logged. Message
 * texts are never logged either.</p>
 */
@Component
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TelegramBotApi {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramBotApi.class);
    /** Telegram holds a long poll up to this many seconds; the read timeout of that client is longer. */
    static final int LONG_POLL_SECONDS = 25;
    private static final int DEFAULT_RETRY_AFTER = 1;

    private final TelegramBotProperties properties;
    private final RestClient client;
    private final RestClient longPollClient;

    public TelegramBotApi(TelegramBotProperties properties) {
        this.properties = properties;
        this.client = build(properties.connectTimeout(), properties.readTimeout());
        this.longPollClient = build(properties.connectTimeout(), Duration.ofSeconds(LONG_POLL_SECONDS).plus(properties.readTimeout()));
    }

    private static RestClient build(Duration connectTimeout, Duration readTimeout) {
        final var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(factory).build();
    }

    public BotResult getMe() {
        return call(client, "getMe", Map.of());
    }

    /** Sends HTML text to a chat; {@code text} must be at most 4096 characters (the sender splits). */
    public BotResult sendMessage(long chatId, String text, boolean disableNotification) {
        final var body = new LinkedHashMap<String, Object>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        body.put("link_preview_options", Map.of("is_disabled", true));
        if (disableNotification) {
            body.put("disable_notification", true);
        }
        return call(client, "sendMessage", body);
    }

    /** Long polling: updates with {@code update_id >= offset} (null = the oldest not yet confirmed). */
    public BotResult getUpdates(Long offset) {
        final var body = new LinkedHashMap<String, Object>();
        if (offset != null) {
            body.put("offset", offset);
        }
        body.put("timeout", LONG_POLL_SECONDS);
        body.put("allowed_updates", List.of("message", "my_chat_member"));
        return call(longPollClient, "getUpdates", body);
    }

    public BotResult setWebhook(String url, String secretToken) {
        final var body = new LinkedHashMap<String, Object>();
        body.put("url", url);
        body.put("secret_token", secretToken);
        body.put("allowed_updates", List.of("message", "my_chat_member"));
        return call(client, "setWebhook", body);
    }

    public BotResult deleteWebhook() {
        return call(client, "deleteWebhook", Map.of());
    }

    private BotResult call(RestClient http, String method, Map<String, Object> body) {
        if (!properties.active()) {
            return new BotResult.Unavailable("channel off");
        }
        try {
            return http.post()
                    .uri(properties.apiBaseUrl() + "/bot" + properties.botToken() + "/" + method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, response) -> interpret(response.getStatusCode().value(), read(response)));
        } catch (RuntimeException e) {
            // The message of an HTTP exception names the URL, hence the token: class only.
            LOG.debug("Telegram {} transport error: {}", method, e.getClass().getSimpleName());
            return new BotResult.Unavailable(e.getClass().getSimpleName());
        }
    }

    private static Map<?, ?> read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        try {
            return response.bodyTo(Map.class);
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    static BotResult interpret(int status, Map<?, ?> json) {
        final Map<?, ?> body = json == null ? Map.of() : json;
        final String description = body.get("description") instanceof String s ? s : "";
        if (status == HttpStatus.OK.value() && Boolean.TRUE.equals(body.get("ok"))) {
            return new BotResult.Ok(body.get("result"));
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return new BotResult.RateLimited(retryAfter(body));
        }
        if (status == HttpStatus.FORBIDDEN.value()) {
            return new BotResult.Forbidden(description);
        }
        if (HttpStatusCode.valueOf(status).is4xxClientError()) {
            return new BotResult.Rejected(status, description);
        }
        return new BotResult.Unavailable("status " + status);
    }

    private static int retryAfter(Map<?, ?> body) {
        if (body.get("parameters") instanceof Map<?, ?> p && p.get("retry_after") instanceof Number n && n.intValue() > 0) {
            return n.intValue();
        }
        return DEFAULT_RETRY_AFTER;
    }
}
