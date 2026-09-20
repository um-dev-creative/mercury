package com.prx.mercury.kafka.consumer.service;

import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerServiceImpl;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("NotificationEventConsumerServiceImpl unit tests")
class NotificationEventConsumerServiceImplTest {

    private final NotificationEventConsumerServiceImpl service = new NotificationEventConsumerServiceImpl();

    private NotificationEventTO eventFor(String channel) {
        return new NotificationEventTO(
                new NotificationEventTO.Header("campaign-1", "message-1", "2026-01-01T00:00:00Z", "trace-1"),
                new NotificationEventTO.Routing(channel, "recipient-1", "+15550001111"),
                new NotificationEventTO.Content("template-1", "welcome", "en", "body", null, "NONE"),
                new NotificationEventTO.Payload(new NotificationEventTO.Context(), new NotificationEventTO.DeliveryInstructions())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "sms", "telegram", "whatsapp", "push"})
    @DisplayName("process routes each known channel without throwing")
    void process_knownChannel_doesNotThrow(String channel) {
        assertDoesNotThrow(() -> service.process(eventFor(channel)));
    }

    @Test
    @DisplayName("process discards the event when the channel code is unknown")
    void process_unknownChannel_discardsSilently() {
        assertDoesNotThrow(() -> service.process(eventFor("fax")));
    }
}
