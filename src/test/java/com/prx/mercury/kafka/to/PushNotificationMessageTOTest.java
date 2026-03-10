package com.prx.mercury.kafka.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PushNotificationMessageTOTest {

    @Test
    @DisplayName("priority defaults to normal when blank input")
    void priorityDefaultsToNormal() {
        PushNotificationMessageTO messageTO = new PushNotificationMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "device-token",
                "android",
                "Greetings",
                "Body",
                null,
                null,
                null,
                "",
                1,
                "default",
                LocalDateTime.now(),
                Map.of("key", "value"),
                UUID.randomUUID()
        );

        assertEquals("normal", messageTO.priority());
    }

    @Test
    @DisplayName("toString masks device token content")
    void toStringMasksDeviceToken() {
        PushNotificationMessageTO messageTO = new PushNotificationMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "secret-token",
                "ios",
                "Promo",
                "Body",
                "icon.png",
                null,
                null,
                "high",
                null,
                null,
                LocalDateTime.now(),
                Map.of(),
                null
        );

        String representation = messageTO.toString();
        assertTrue(representation.contains("deviceToken='[HIDDEN]"));
        assertTrue(representation.contains("priority='high"));
    }
}

