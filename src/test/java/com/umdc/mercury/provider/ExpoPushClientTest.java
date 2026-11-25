package com.umdc.mercury.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Expo Push client (against a fake Expo server)")
class ExpoPushClientTest {

    private static final String ACCESS_TOKEN = "SECRET-ACCESS-TOKEN";
    private static final List<Map<String, Object>> ONE = List.of(Map.of("to", "ExponentPushToken[abc]", "body", "hi"));

    private static ExpoPushClient client(FakeExpoServer fake, String accessToken, int attempts, Duration readTimeout) {
        return new ExpoPushClient(new ExpoPushProperties(true, fake.url(), accessToken, Duration.ofSeconds(1), readTimeout, attempts,
                Duration.ofMillis(1), null, 0, null));
    }

    @Test
    @DisplayName("send posts the JSON array to /--/api/v2/push/send and reads one ticket per message")
    void sendReadsTickets() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, """
                {"data":[{"status":"ok","id":"R-1"},
                         {"status":"error","message":"\\"ExponentPushToken[zzz]\\" is not registered","details":{"error":"DeviceNotRegistered"}}]}""")) {
            var two = List.<Map<String, Object>>of(Map.of("to", "ExponentPushToken[a]", "body", "x"), Map.of("to", "ExponentPushToken[zzz]", "body", "y"));

            var result = assertInstanceOf(ExpoPushResult.Tickets.class, client(fake, "", 3, Duration.ofSeconds(2)).send(two));

            assertEquals(2, result.tickets().size());
            assertTrue(result.tickets().get(0).ok());
            assertEquals("R-1", result.tickets().get(0).id());
            assertFalse(result.tickets().get(1).ok());
            assertEquals("DeviceNotRegistered", result.tickets().get(1).error());
            var call = fake.calls().getFirst();
            assertEquals("/--/api/v2/push/send", call.path());
            assertTrue(call.contentType().startsWith("application/json"));
            assertTrue(call.body().startsWith("[{") && call.body().contains("\"to\":\"ExponentPushToken[a]\""), call.body());
            assertNull(call.authorization(), "no access token configured: no Authorization header");
        }
    }

    @Test
    @DisplayName("the access token, when configured, goes as a Bearer header")
    void bearer() throws IOException {
        try (var fake = new FakeExpoServer()) {
            client(fake, ACCESS_TOKEN, 1, Duration.ofSeconds(2)).send(ONE);
            assertEquals("Bearer " + ACCESS_TOKEN, fake.calls().getFirst().authorization());
        }
    }

    @Test
    @DisplayName("receipts posts {ids} to /--/api/v2/push/getReceipts and maps receipts by id, errors included")
    void receipts() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, """
                {"data":{"R-1":{"status":"ok"},"R-2":{"status":"error","message":"gone","details":{"error":"DeviceNotRegistered"}}}}""")) {
            var result = assertInstanceOf(ExpoPushResult.Receipts.class, client(fake, "", 3, Duration.ofSeconds(2)).receipts(List.of("R-1", "R-2", "R-3")));

            assertTrue(result.byId().get("R-1").ok());
            assertEquals("DeviceNotRegistered", result.byId().get("R-2").error());
            assertFalse(result.byId().containsKey("R-3"), "an id without a receipt is absent");
            assertEquals("/--/api/v2/push/getReceipts", fake.calls().getFirst().path());
            assertTrue(fake.calls().getFirst().body().contains("\"ids\":[\"R-1\",\"R-2\",\"R-3\"]"));
        }
    }

    @Test
    @DisplayName("429 and 5xx are retried with backoff, up to the bounded attempts, then Unavailable")
    void retriesThenGivesUp() throws IOException {
        try (var fake = new FakeExpoServer().reply(429, "{\"errors\":[{\"code\":\"TOO_MANY_REQUESTS\"}]}").reply(503, "").reply(500, "")) {
            var result = client(fake, "", 3, Duration.ofSeconds(2)).send(ONE);

            assertEquals("status 500", assertInstanceOf(ExpoPushResult.Unavailable.class, result).reason());
            assertEquals(3, fake.calls().size());
        }
    }

    @Test
    @DisplayName("a retry that finds Expo healthy again succeeds")
    void recoversOnRetry() throws IOException {
        try (var fake = new FakeExpoServer().reply(502, "").reply(200, "{\"data\":[{\"status\":\"ok\",\"id\":\"R\"}]}")) {
            assertInstanceOf(ExpoPushResult.Tickets.class, client(fake, "", 3, Duration.ofSeconds(2)).send(ONE));
            assertEquals(2, fake.calls().size());
        }
    }

    @Test
    @DisplayName("a 4xx is not retried: a malformed request is Rejected with Expo's error code")
    void rejectedIsNotRetried() throws IOException {
        try (var fake = new FakeExpoServer().reply(400, "{\"errors\":[{\"code\":\"PUSH_TOO_MANY_EXPERIENCE_IDS\",\"message\":\"x\"}]}")) {
            var rejected = assertInstanceOf(ExpoPushResult.Rejected.class, client(fake, "", 3, Duration.ofSeconds(2)).send(ONE));

            assertEquals(400, rejected.status());
            assertEquals("PUSH_TOO_MANY_EXPERIENCE_IDS", rejected.code());
            assertEquals(1, fake.calls().size());
        }
    }

    @Test
    @DisplayName("a 4xx with no errors array is still Rejected, with an empty code")
    void rejectedWithoutBody() throws IOException {
        try (var fake = new FakeExpoServer().reply(400, "not json")) {
            assertEquals("", assertInstanceOf(ExpoPushResult.Rejected.class, client(fake, "", 3, Duration.ofSeconds(2)).send(ONE)).code());
        }
    }

    @Test
    @DisplayName("401 and 403 are Unauthorized and not retried")
    void unauthorized() throws IOException {
        try (var fake = new FakeExpoServer().reply(401, "{\"errors\":[{\"code\":\"UNAUTHORIZED\"}]}")) {
            assertEquals(401, assertInstanceOf(ExpoPushResult.Unauthorized.class, client(fake, "", 3, Duration.ofSeconds(2)).send(ONE)).status());
            assertEquals(1, fake.calls().size());
        }
        assertEquals(403, assertInstanceOf(ExpoPushResult.Unauthorized.class, ExpoPushClient.interpret(403, Map.of(), false)).status());
    }

    @Test
    @DisplayName("a read timeout is bounded and ends as Unavailable")
    void readTimeout() throws IOException {
        try (var fake = new FakeExpoServer().delay(1500)) {
            long start = System.nanoTime();
            var result = client(fake, "", 1, Duration.ofMillis(200)).send(ONE);

            assertInstanceOf(ExpoPushResult.Unavailable.class, result);
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 1400, "the call must not wait for the slow server");
        }
    }

    @Test
    @DisplayName("an unreachable server is Unavailable, naming only the exception class")
    void unreachable() throws IOException {
        var fake = new FakeExpoServer();
        var client = client(fake, "", 2, Duration.ofSeconds(1));
        fake.close();

        var result = assertInstanceOf(ExpoPushResult.Unavailable.class, client.send(ONE));

        assertFalse(result.reason().contains("127.0.0.1"), result.reason());
    }

    @Test
    @DisplayName("a 200 that is not the documented shape is Unavailable, not a crash")
    void malformedAnswer() {
        assertInstanceOf(ExpoPushResult.Unavailable.class, ExpoPushClient.interpret(200, Map.of("data", "x"), false));
        assertInstanceOf(ExpoPushResult.Unavailable.class, ExpoPushClient.interpret(200, Map.of(), true));
        assertInstanceOf(ExpoPushResult.Unavailable.class, ExpoPushClient.interpret(200, null, false));
        var tickets = assertInstanceOf(ExpoPushResult.Tickets.class, ExpoPushClient.interpret(200, Map.of("data", List.of("oops", Map.of("status", "error"))), false));
        assertEquals("Malformed", tickets.tickets().get(0).error());
        assertEquals("Unknown", tickets.tickets().get(1).error());
    }

    @Test
    @DisplayName("the documented limits are enforced: 100 messages and 1000 receipt ids per request")
    void limits() throws IOException {
        try (var fake = new FakeExpoServer()) {
            var client = client(fake, "", 1, Duration.ofSeconds(1));
            var tooMany = IntStream.rangeClosed(1, 101).mapToObj(i -> Map.<String, Object>of("to", "t" + i)).toList();
            var tooManyIds = IntStream.rangeClosed(1, 1001).mapToObj(String::valueOf).toList();

            assertThrows(IllegalArgumentException.class, () -> client.send(tooMany));
            assertThrows(IllegalArgumentException.class, () -> client.receipts(tooManyIds));
            assertTrue(fake.calls().isEmpty());
        }
    }

    @Test
    @DisplayName("an interrupted backoff stops the retries and keeps the interrupt flag")
    void interrupted() throws IOException {
        try (var fake = new FakeExpoServer().reply(500, "")) {
            var client = new ExpoPushClient(new ExpoPushProperties(true, fake.url(), null, Duration.ofSeconds(1), Duration.ofSeconds(2), 3, Duration.ofSeconds(30), null, 0, null));
            Thread.currentThread().interrupt();
            try {
                assertEquals("interrupted", assertInstanceOf(ExpoPushResult.Unavailable.class, client.send(ONE)).reason());
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
        }
    }

    @Test
    @DisplayName("properties: safe defaults, bounded attempts, and the access token never printed")
    void properties() {
        var defaults = new ExpoPushProperties(false, null);
        assertEquals("https://exp.host", defaults.apiBaseUrl());
        assertEquals(3, defaults.maxAttempts());
        assertEquals(5, defaults.maxDeliveryAttempts());
        assertEquals(Duration.ofMinutes(15), defaults.receiptDelay());
        assertEquals("default", defaults.androidChannelId());
        assertEquals(5, new ExpoPushProperties(true, "https://x/", null, null, null, 99, null, " chan ", -1, null).maxAttempts());
        assertEquals("https://x", new ExpoPushProperties(true, "https://x//", null, null, null, 0, null, " chan ", -1, null).apiBaseUrl());
        assertEquals("chan", new ExpoPushProperties(true, "https://x/", null, null, null, 99, null, " chan ", -1, null).androidChannelId());
        assertFalse(new ExpoPushProperties(true, "https://x", ACCESS_TOKEN, null, null, 0, null, null, 0, null).toString().contains(ACCESS_TOKEN));
        assertTrue(new ExpoPushProperties(true, "https://x", ACCESS_TOKEN, null, null, 0, null, null, 0, null).toString().contains("***"));
        assertTrue(defaults.toString().contains("accessToken=,"));
    }
}
