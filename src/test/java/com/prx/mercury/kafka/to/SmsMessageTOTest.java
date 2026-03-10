package com.prx.mercury.kafka.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SmsMessageTOTest {

    @Test
    @DisplayName("SmsMessageTO stores provided values")
    void createsSmsMessageTO() {
        UUID templateDefinedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = Map.of("greeting", "hello");

        SmsMessageTO smsMessageTO = new SmsMessageTO(
                templateDefinedId,
                userId,
                "+15551234567",
                "Your OTP is 123456",
                "Sender",
                sendDate,
                params,
                campaignId
        );

        assertEquals(templateDefinedId, smsMessageTO.templateDefinedId());
        assertEquals(userId, smsMessageTO.userId());
        assertEquals("+15551234567", smsMessageTO.phoneNumber());
        assertEquals("Your OTP is 123456", smsMessageTO.message());
        assertEquals("Sender", smsMessageTO.senderId());
        assertEquals(sendDate, smsMessageTO.sendDate());
        assertEquals(params, smsMessageTO.params());
        assertEquals(campaignId, smsMessageTO.campaignId());
    }

    @Test
    @DisplayName("SmsMessageTO toString includes masked values")
    void smsMessageToString() {
        SmsMessageTO smsMessageTO = new SmsMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "+447700900123",
                "Test message",
                null,
                null,
                Map.of(),
                null
        );

        String representation = smsMessageTO.toString();
        assertTrue(representation.contains("phoneNumber='+447700900123"));
        assertTrue(representation.contains("message='Test message"));
    }
}

