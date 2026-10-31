package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Telegram Bot API client")
class TelegramBotApiTest {

    private static final String TOKEN = "123456:SECRET-TOKEN";

    private static TelegramBotApi api(FakeBotApi fake, Duration readTimeout) {
        return new TelegramBotApi(new TelegramBotProperties(true, TOKEN, "", fake.url(), null, Duration.ofSeconds(1), readTimeout));
    }

    @Test
    @DisplayName("sendMessage posts HTML text to the chat and returns the Telegram result")
    void sendMessage() throws IOException {
        try (var fake = new FakeBotApi().reply(200, "{\"ok\":true,\"result\":{\"message_id\":7}}")) {
            final var result = api(fake, Duration.ofSeconds(2)).sendMessage(42L, "<b>hola</b>", true);
            final var ok = assertInstanceOf(BotResult.Ok.class, result);
            assertEquals(7, ((Map<?, ?>) ok.result()).get("message_id"));
            final var call = fake.calls().getFirst();
            assertEquals("/bot" + TOKEN + "/sendMessage", call.path());
            assertTrue(call.body().contains("\"chat_id\":42"));
            assertTrue(call.body().contains("\"parse_mode\":\"HTML\""));
            assertTrue(call.body().contains("\"disable_notification\":true"));
        }
    }

    @Test
    @DisplayName("a quiet message omits disable_notification")
    void notQuiet() throws IOException {
        try (var fake = new FakeBotApi()) {
            api(fake, Duration.ofSeconds(2)).sendMessage(1L, "x", false);
            assertFalse(fake.calls().getFirst().body().contains("disable_notification"));
        }
    }

    @Test
    @DisplayName("429 carries retry_after; without it the default is one second")
    void rateLimited() throws IOException {
        try (var fake = new FakeBotApi()
                .reply(429, "{\"ok\":false,\"error_code\":429,\"parameters\":{\"retry_after\":17}}")
                .reply(429, "{\"ok\":false}")) {
            final var api = api(fake, Duration.ofSeconds(2));
            assertEquals(17, assertInstanceOf(BotResult.RateLimited.class, api.sendMessage(1L, "x", false)).retryAfterSeconds());
            assertEquals(1, assertInstanceOf(BotResult.RateLimited.class, api.sendMessage(1L, "x", false)).retryAfterSeconds());
        }
    }

    @Test
    @DisplayName("403 is Forbidden (bot blocked); other 4xx are Rejected; 5xx is Unavailable")
    void statuses() throws IOException {
        try (var fake = new FakeBotApi()
                .reply(403, "{\"ok\":false,\"description\":\"Forbidden: bot was blocked by the user\"}")
                .reply(400, "{\"ok\":false,\"description\":\"Bad Request: chat not found\"}")
                .reply(401, "{\"ok\":false,\"description\":\"Unauthorized\"}")
                .reply(502, "<html>bad gateway</html>")) {
            final var api = api(fake, Duration.ofSeconds(2));
            assertTrue(assertInstanceOf(BotResult.Forbidden.class, api.sendMessage(1L, "x", false)).description().contains("blocked"));
            final var rejected = assertInstanceOf(BotResult.Rejected.class, api.sendMessage(1L, "x", false));
            assertEquals(400, rejected.status());
            assertEquals(401, assertInstanceOf(BotResult.Rejected.class, api.getMe()).status());
            assertInstanceOf(BotResult.Unavailable.class, api.sendMessage(1L, "x", false));
        }
    }

    @Test
    @DisplayName("a timeout is Unavailable and its reason does not contain the token")
    void timeoutDoesNotLeakToken() throws IOException {
        try (var fake = new FakeBotApi().delay(600)) {
            final var result = api(fake, Duration.ofMillis(100)).getMe();
            final var unavailable = assertInstanceOf(BotResult.Unavailable.class, result);
            assertFalse(unavailable.reason().contains("SECRET"));
            assertFalse(result.toString().contains("SECRET"));
        }
    }

    @Test
    @DisplayName("a connection refused is Unavailable")
    void unreachable() throws IOException {
        final String url;
        try (var fake = new FakeBotApi()) {
            url = fake.url();
        }
        final var api = new TelegramBotApi(new TelegramBotProperties(true, TOKEN, "", url, null, Duration.ofMillis(300), Duration.ofSeconds(1)));
        assertInstanceOf(BotResult.Unavailable.class, api.getMe());
    }

    @Test
    @DisplayName("a channel that is off never calls the Bot API")
    void off() throws IOException {
        try (var fake = new FakeBotApi()) {
            final var off = new TelegramBotApi(new TelegramBotProperties(false, TOKEN, "", fake.url(), null, null, null));
            assertInstanceOf(BotResult.Unavailable.class, off.sendMessage(1L, "x", false));
            assertTrue(fake.calls().isEmpty());
        }
    }

    @Test
    @DisplayName("getUpdates asks for message and my_chat_member with the offset; setWebhook sends the secret; deleteWebhook works")
    void updatesAndWebhook() throws IOException {
        try (var fake = new FakeBotApi().reply(200, "{\"ok\":true,\"result\":[{\"update_id\":5}]}")) {
            final var api = api(fake, Duration.ofSeconds(2));
            final var ok = assertInstanceOf(BotResult.Ok.class, api.getUpdates(5L));
            assertEquals(1, ((List<?>) ok.result()).size());
            assertInstanceOf(BotResult.Ok.class, api.getUpdates(null));
            assertInstanceOf(BotResult.Ok.class, api.setWebhook("https://x/hook", "s3cret"));
            assertInstanceOf(BotResult.Ok.class, api.deleteWebhook());
            final var calls = fake.calls();
            assertTrue(calls.get(0).body().contains("\"offset\":5"));
            assertTrue(calls.get(0).body().contains("\"timeout\":25"));
            assertTrue(calls.get(0).body().contains("my_chat_member"));
            assertFalse(calls.get(1).body().contains("offset"));
            assertTrue(calls.get(2).body().contains("\"secret_token\":\"s3cret\""));
            assertEquals("/bot" + TOKEN + "/deleteWebhook", calls.get(3).path());
        }
    }

    @Test
    @DisplayName("interpret tolerates a missing or odd body")
    void interpretOddBodies() {
        assertInstanceOf(BotResult.Unavailable.class, TelegramBotApi.interpret(500, null));
        assertInstanceOf(BotResult.Rejected.class, TelegramBotApi.interpret(404, Map.of("description", 5)));
        assertEquals(1, assertInstanceOf(BotResult.RateLimited.class,
                TelegramBotApi.interpret(429, Map.of("parameters", Map.of("retry_after", 0)))).retryAfterSeconds());
        assertInstanceOf(BotResult.Unavailable.class, TelegramBotApi.interpret(200, Map.of("ok", false)));
        assertInstanceOf(BotResult.Rejected.class, TelegramBotApi.interpret(499, Map.of()));
    }
}
