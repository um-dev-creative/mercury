package com.umdc.mercury.kafka.listener;

import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationEventListenerTest {

    private static final NotificationEventTO.Header HEADER =
            new NotificationEventTO.Header("campaign", "message", "2026-10-31T00:00:00Z", "trace");
    private static final NotificationEventTO.Routing ROUTING =
            new NotificationEventTO.Routing("SMS", "recipient", "+34600111222");
    private static final NotificationEventTO.Content CONTENT =
            new NotificationEventTO.Content("template", "name", "es", "body", null, "NONE");
    private static final NotificationEventTO.Payload PAYLOAD =
            new NotificationEventTO.Payload(Map.of(), Map.of());

    private final NotificationEventConsumerService service = mock(NotificationEventConsumerService.class);
    private final NotificationEventListener listener = new NotificationEventListener(service);

    @Test
    void completeEventIsProcessed() {
        NotificationEventTO event = new NotificationEventTO(HEADER, ROUTING, CONTENT, PAYLOAD);

        listener.listen(event);

        verify(service).process(event);
    }

    @Test
    void nullEventIsDiscarded() {
        listener.listen(null);

        verify(service, never()).process(any());
    }

    @Test
    void eventWithoutHeaderIsDiscarded() {
        listener.listen(new NotificationEventTO(null, ROUTING, CONTENT, PAYLOAD));

        verify(service, never()).process(any());
    }

    @Test
    void eventWithoutRoutingIsDiscarded() {
        listener.listen(new NotificationEventTO(HEADER, null, CONTENT, PAYLOAD));

        verify(service, never()).process(any());
    }

    @Test
    void eventWithoutContentIsDiscarded() {
        listener.listen(new NotificationEventTO(HEADER, ROUTING, null, PAYLOAD));

        verify(service, never()).process(any());
    }
}
