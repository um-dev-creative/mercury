package com.umdc.mercury.provider;

import com.umdc.mercury.constant.DeliveryFailureReason;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Expo push dispatcher (against a fake Expo server)")
class ExpoPushDispatcherTest {

    private static final int MAX_DELIVERY_ATTEMPTS = 3;

    private static ExpoPushDispatcher dispatcher(FakeExpoServer fake) {
        var properties = new ExpoPushProperties(true, fake.url(), null, Duration.ofSeconds(1), Duration.ofSeconds(2), 1, Duration.ofMillis(1), "alerts",
                MAX_DELIVERY_ATTEMPTS, Duration.ofMinutes(15));
        return new ExpoPushDispatcher(new ExpoPushClient(properties), properties);
    }

    private static PushNotificationMessageDocument message(String token, String body) {
        var document = new PushNotificationMessageDocument();
        document.setId("m-" + token);
        document.setDeviceToken(token);
        document.setBody(body);
        document.setTitle("CHISPA");
        document.setDeliveryStatus(DeliveryStatusType.OPENED);
        return document;
    }

    private static String token(String id) {
        return "ExponentPushToken[" + id + "]";
    }

    @Test
    @DisplayName("an accepted message is SENT and keeps the receipt id and the send time; the payload follows the Expo contract")
    void accepted() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, "{\"data\":[{\"status\":\"ok\",\"id\":\"RCPT-1\"}]}")) {
            var doc = message(token("a"), "Tienes una sorpresa en CHISPA.");
            doc.setData(Map.of("kind", "invitation", "ref", "42"));
            doc.setBadge(2);
            doc.setPriority("high");
            doc.setSound("ding");

            var changed = dispatcher(fake).dispatch(List.of(doc));

            assertEquals(List.of(doc), changed);
            assertEquals(DeliveryStatusType.SENT, doc.getDeliveryStatus());
            assertEquals("RCPT-1", doc.getProviderReceiptId());
            assertNotNull(doc.getSentAt());
            assertNull(doc.getFailureReason());
            var body = fake.calls().getFirst().body();
            assertTrue(body.contains("\"to\":\"ExponentPushToken[a]\"") && body.contains("\"title\":\"CHISPA\"")
                    && body.contains("\"body\":\"Tienes una sorpresa en CHISPA.\"") && body.contains("\"data\":{") && body.contains("\"badge\":2")
                    && body.contains("\"priority\":\"high\"") && body.contains("\"sound\":\"ding\"") && body.contains("\"channelId\":\"alerts\""), body);
        }
    }

    @Test
    @DisplayName("defaults: sound default, priority default for an unknown value, and no title or data when there are none")
    void payloadDefaults() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, "{\"data\":[{\"status\":\"ok\",\"id\":\"R\"}]}")) {
            var doc = message(token("a"), "hi");
            doc.setTitle(" ");
            doc.setPriority("urgent");

            dispatcher(fake).dispatch(List.of(doc));

            var body = fake.calls().getFirst().body();
            assertTrue(body.contains("\"sound\":\"default\"") && body.contains("\"priority\":\"default\""), body);
            assertFalse(body.contains("\"title\"") || body.contains("\"data\"") || body.contains("\"badge\""), body);
        }
    }

    @Test
    @DisplayName("a dead device token (DeviceNotRegistered in the ticket) is FAILED with DEVICE_NOT_REGISTERED")
    void deadTokenInTicket() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, """
                {"data":[{"status":"error","message":"nope","details":{"error":"DeviceNotRegistered"}}]}""")) {
            var doc = message(token("dead"), "hi");

            dispatcher(fake).dispatch(List.of(doc));

            assertEquals(DeliveryStatusType.FAILED, doc.getDeliveryStatus());
            assertEquals(DeliveryFailureReason.DEVICE_NOT_REGISTERED, doc.getFailureReason());
        }
    }

    @Test
    @DisplayName("ticket errors map to their reason: MessageTooBig, MismatchSenderId, InvalidCredentials and unknown ones")
    void ticketErrorMapping() {
        assertEquals(DeliveryFailureReason.MESSAGE_TOO_BIG, ExpoPushPayload.reasonOf("MessageTooBig"));
        assertEquals(DeliveryFailureReason.MISMATCH_SENDER_ID, ExpoPushPayload.reasonOf("MismatchSenderId"));
        assertEquals(DeliveryFailureReason.INVALID_CREDENTIALS, ExpoPushPayload.reasonOf("InvalidCredentials"));
        assertEquals(DeliveryFailureReason.MESSAGE_RATE_EXCEEDED, ExpoPushPayload.reasonOf("MessageRateExceeded"));
        assertEquals(DeliveryFailureReason.DEVICE_NOT_REGISTERED, ExpoPushPayload.reasonOf("DeviceNotRegistered"));
        assertEquals(DeliveryFailureReason.PROVIDER_ERROR, ExpoPushPayload.reasonOf("Whatever"));
        assertEquals(DeliveryFailureReason.PROVIDER_ERROR, ExpoPushPayload.reasonOf(null));
    }

    @Test
    @DisplayName("a permanent ticket error other than a dead token is FAILED with its own reason")
    void permanentTicketError() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, "{\"data\":[{\"status\":\"error\",\"details\":{\"error\":\"MessageTooBig\"}}]}")) {
            var doc = message(token("a"), "hi");
            dispatcher(fake).dispatch(List.of(doc));
            assertEquals(DeliveryFailureReason.MESSAGE_TOO_BIG, doc.getFailureReason());
        }
    }

    @Test
    @DisplayName("a token that is not an Expo token and a message without text fail locally, without calling Expo")
    void invalidLocally() throws IOException {
        try (var fake = new FakeExpoServer()) {
            var badToken = message("fcm-raw-token", "hi");
            var noText = message(token("a"), " ");
            var noToken = message(null, "hi");

            var changed = dispatcher(fake).dispatch(List.of(badToken, noText, noToken));

            assertEquals(3, changed.size());
            assertEquals(DeliveryFailureReason.INVALID_TOKEN, badToken.getFailureReason());
            assertEquals(DeliveryFailureReason.INVALID_MESSAGE, noText.getFailureReason());
            assertEquals(DeliveryFailureReason.INVALID_TOKEN, noToken.getFailureReason());
            assertTrue(fake.calls().isEmpty());
        }
    }

    @Test
    @DisplayName("token shapes: ExponentPushToken[..] and ExpoPushToken[..] are valid; anything else is not")
    void tokenShape() {
        assertTrue(ExpoPushPayload.isExpoToken("ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]"));
        assertTrue(ExpoPushPayload.isExpoToken(" ExpoPushToken[abc-123] "));
        assertFalse(ExpoPushPayload.isExpoToken("ExponentPushToken[]"));
        assertFalse(ExpoPushPayload.isExpoToken("ExponentPushToken[a b]"));
        assertFalse(ExpoPushPayload.isExpoToken("fcm:APA91b"));
        assertFalse(ExpoPushPayload.isExpoToken(null));
    }

    @Test
    @DisplayName("more than 100 messages go in batches of at most 100")
    void batches() throws IOException {
        try (var fake = new FakeExpoServer()) {
            var docs = IntStream.range(0, 250).mapToObj(i -> message(token("t" + i), "hi")).toList();
            fake.reply(200, tickets(100)).reply(200, tickets(100)).reply(200, tickets(50));

            var changed = dispatcher(fake).dispatch(docs);

            assertEquals(250, changed.size());
            assertEquals(3, fake.calls().size());
            assertTrue(docs.stream().allMatch(d -> d.getDeliveryStatus() == DeliveryStatusType.SENT));
            assertEquals("R0", docs.getFirst().getProviderReceiptId());
        }
    }

    private static String tickets(int n) {
        return "{\"data\":[" + String.join(",", IntStream.range(0, n).mapToObj(i -> "{\"status\":\"ok\",\"id\":\"R" + i + "\"}").toList()) + "]}";
    }

    @Test
    @DisplayName("a transient failure (503) leaves the message OPENED with one attempt counted, and FAILED once the attempts run out")
    void transientThenExhausted() throws IOException {
        try (var fake = new FakeExpoServer().reply(503, "").reply(503, "").reply(503, "")) {
            var doc = message(token("a"), "hi");
            var dispatcher = dispatcher(fake);

            dispatcher.dispatch(List.of(doc));
            assertEquals(DeliveryStatusType.OPENED, doc.getDeliveryStatus());
            assertEquals(1, doc.getDeliveryAttempts());
            dispatcher.dispatch(List.of(doc));
            assertEquals(DeliveryStatusType.OPENED, doc.getDeliveryStatus());
            dispatcher.dispatch(List.of(doc));

            assertEquals(DeliveryStatusType.FAILED, doc.getDeliveryStatus());
            assertEquals(DeliveryFailureReason.PROVIDER_UNAVAILABLE, doc.getFailureReason());
            assertEquals(3, doc.getDeliveryAttempts());
        }
    }

    @Test
    @DisplayName("MessageRateExceeded in a ticket is transient: retried on a later run, and FAILED with its reason when exhausted")
    void rateExceeded() throws IOException {
        var rate = "{\"data\":[{\"status\":\"error\",\"details\":{\"error\":\"MessageRateExceeded\"}}]}";
        try (var fake = new FakeExpoServer().reply(200, rate).reply(200, rate).reply(200, rate)) {
            var doc = message(token("a"), "hi");
            var dispatcher = dispatcher(fake);
            dispatcher.dispatch(List.of(doc));
            assertEquals(DeliveryStatusType.OPENED, doc.getDeliveryStatus());
            dispatcher.dispatch(List.of(doc));
            dispatcher.dispatch(List.of(doc));
            assertEquals(DeliveryStatusType.FAILED, doc.getDeliveryStatus());
            assertEquals(DeliveryFailureReason.MESSAGE_RATE_EXCEEDED, doc.getFailureReason());
        }
    }

    @Test
    @DisplayName("a batch Expo rejects is sent one by one, so one bad message does not fail the others")
    void rejectedBatchIsSplit() throws IOException {
        try (var fake = new FakeExpoServer()
                .reply(400, "{\"errors\":[{\"code\":\"PUSH_TOO_MANY_EXPERIENCE_IDS\"}]}")
                .reply(200, "{\"data\":[{\"status\":\"ok\",\"id\":\"R-a\"}]}")
                .reply(400, "{\"errors\":[{\"code\":\"VALIDATION_ERROR\"}]}")) {
            var good = message(token("a"), "hi");
            var bad = message(token("b"), "hi");

            dispatcher(fake).dispatch(List.of(good, bad));

            assertEquals(DeliveryStatusType.SENT, good.getDeliveryStatus());
            assertEquals(DeliveryStatusType.FAILED, bad.getDeliveryStatus());
            assertEquals(DeliveryFailureReason.PROVIDER_REJECTED, bad.getFailureReason());
            assertEquals(3, fake.calls().size());
        }
    }

    @Test
    @DisplayName("a refused access token (401) changes nothing and stops the remaining batches: the messages wait for an operator")
    void unauthorizedWaits() throws IOException {
        try (var fake = new FakeExpoServer().reply(401, "{\"errors\":[{\"code\":\"UNAUTHORIZED\"}]}")) {
            var docs = IntStream.range(0, 150).mapToObj(i -> message(token("t" + i), "hi")).toList();

            var changed = dispatcher(fake).dispatch(docs);

            assertTrue(changed.isEmpty());
            assertEquals(1, fake.calls().size(), "the second batch is not even tried");
            assertTrue(docs.stream().allMatch(d -> d.getDeliveryStatus() == DeliveryStatusType.OPENED && d.getDeliveryAttempts() == null));
        }
    }

    @Test
    @DisplayName("an answer with the wrong number of tickets is treated as transient, never matched to the wrong message")
    void ticketCountMismatch() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, "{\"data\":[{\"status\":\"ok\",\"id\":\"R\"}]}")) {
            var a = message(token("a"), "hi");
            var b = message(token("b"), "hi");

            dispatcher(fake).dispatch(List.of(a, b));

            assertEquals(DeliveryStatusType.OPENED, a.getDeliveryStatus());
            assertEquals(1, b.getDeliveryAttempts());
        }
    }

    @Test
    @DisplayName("refuses to dispatch while the channel is off")
    void offThrows() throws IOException {
        try (var fake = new FakeExpoServer()) {
            var off = new ExpoPushProperties(false, fake.url());
            var dispatcher = new ExpoPushDispatcher(new ExpoPushClient(off), off);

            assertFalse(dispatcher.isEnabled());
            assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(List.of()));
            assertEquals(Duration.ofMinutes(15), dispatcher.receiptDelay());
        }
    }

    private static PushNotificationMessageDocument sent(String receipt, LocalDateTime sentAt) {
        var doc = message(token(receipt), "hi");
        doc.setDeliveryStatus(DeliveryStatusType.SENT);
        doc.setProviderReceiptId(receipt);
        doc.setSentAt(sentAt);
        return doc;
    }

    @Test
    @DisplayName("receipts: DeviceNotRegistered turns a SENT message into FAILED/DEVICE_NOT_REGISTERED; an ok receipt only marks it read")
    void receiptsReportDeadTokens() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, """
                {"data":{"R-ok":{"status":"ok"},"R-dead":{"status":"error","message":"x","details":{"error":"DeviceNotRegistered"}}}}""")) {
            var now = LocalDateTime.now(ZoneOffset.UTC);
            var ok = sent("R-ok", now.minusMinutes(20));
            var dead = sent("R-dead", now.minusMinutes(20));

            var changed = dispatcher(fake).checkReceipts(List.of(ok, dead));

            assertEquals(2, changed.size());
            assertEquals(DeliveryStatusType.SENT, ok.getDeliveryStatus());
            assertNotNull(ok.getReceiptCheckedAt());
            assertEquals(DeliveryStatusType.FAILED, dead.getDeliveryStatus());
            assertEquals(DeliveryFailureReason.DEVICE_NOT_REGISTERED, dead.getFailureReason());
            assertNotNull(dead.getReceiptCheckedAt());
        }
    }

    @Test
    @DisplayName("receipts: a missing receipt is asked again, until Expo drops receipts after 24 hours")
    void missingReceipt() throws IOException {
        try (var fake = new FakeExpoServer().reply(200, "{\"data\":{}}")) {
            var now = LocalDateTime.now(ZoneOffset.UTC);
            var fresh = sent("R-1", now.minusMinutes(20));
            var expired = sent("R-2", now.minusHours(25));
            var unknownTime = sent("R-3", null);

            var changed = dispatcher(fake).checkReceipts(List.of(fresh, expired, unknownTime));

            assertEquals(List.of(expired), changed);
            assertNull(fresh.getReceiptCheckedAt());
            assertNull(unknownTime.getReceiptCheckedAt());
            assertNotNull(expired.getReceiptCheckedAt());
            assertEquals(DeliveryStatusType.SENT, expired.getDeliveryStatus());
        }
    }

    @Test
    @DisplayName("receipts: when Expo cannot be reached nothing changes and the receipts are asked again later; ids go in batches of 1000")
    void receiptsUnavailableAndBatching() throws IOException {
        try (var fake = new FakeExpoServer().reply(500, "").reply(200, "{\"data\":{}}")) {
            var now = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20);
            var docs = IntStream.range(0, 1001).mapToObj(i -> sent("R" + i, now)).toList();

            var changed = dispatcher(fake).checkReceipts(docs);

            assertTrue(changed.isEmpty());
            assertEquals(2, fake.calls().size());
            assertTrue(docs.stream().allMatch(d -> d.getReceiptCheckedAt() == null));
        }
    }
}
