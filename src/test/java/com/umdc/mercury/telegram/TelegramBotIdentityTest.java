package com.umdc.mercury.telegram;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Telegram getMe at start-up")
class TelegramBotIdentityTest {

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String body = "{\"ok\":true,\"result\":{\"id\":1,\"username\":\"zerobound_bot\"}}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            path.set(ex.getRequestURI().getPath());
            final var bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private TelegramBotIdentity identity(boolean enabled, String token) {
        final var props = new TelegramBotProperties(enabled, token, "", "http://127.0.0.1:" + server.getAddress().getPort(),
                List.of("es"), null, null);
        return new TelegramBotIdentity(props, new TelegramBotApi(props));
    }

    @Test
    @DisplayName("returns the bot username from getMe")
    void username() {
        assertEquals("zerobound_bot", identity(true, "123:abc").fetchUsername().orElseThrow());
        assertEquals("/bot123:abc/getMe", path.get());
    }

    @Test
    @DisplayName("a wrong token (401), ok=false or a malformed body give an empty answer, never an exception")
    void failures() {
        status = 401;
        body = "{\"ok\":false}";
        assertTrue(identity(true, "bad").fetchUsername().isEmpty());
        status = 200;
        assertTrue(identity(true, "bad").fetchUsername().isEmpty());
        body = "{\"ok\":true,\"result\":{}}";
        assertTrue(identity(true, "t").fetchUsername().isEmpty());
        body = "not json";
        assertTrue(identity(true, "t").fetchUsername().isEmpty());
    }

    @Test
    @DisplayName("an unreachable Bot API is an empty answer")
    void unreachable() {
        server.stop(0);
        assertTrue(identity(true, "t").fetchUsername().isEmpty());
    }

    @Test
    @DisplayName("off or without token it does not call the Bot API")
    void off() {
        assertTrue(identity(false, "t").fetchUsername().isEmpty());
        assertTrue(identity(true, "").fetchUsername().isEmpty());
        assertEquals(null, path.get());
    }

    @Test
    @DisplayName("start-up logging covers every state without throwing")
    void startup() {
        identity(false, "").logAtStartup();
        identity(true, "").logAtStartup();
        identity(true, "t").logAtStartup();
        status = 500;
        identity(true, "t").logAtStartup();
    }

    @Test
    @DisplayName("username() is remembered after the first success")
    void usernameCached() {
        final var identity = identity(true, "123:abc");
        assertEquals("zerobound_bot", identity.username().orElseThrow());
        server.stop(0);
        assertEquals("zerobound_bot", identity.username().orElseThrow());
    }
}
