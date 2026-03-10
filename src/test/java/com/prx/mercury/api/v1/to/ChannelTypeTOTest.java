package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChannelTypeTOTest {

    @Test
    @DisplayName("Create ChannelTypeTO stores all fields correctly")
    void createWithAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ChannelTypeTO to = new ChannelTypeTO(
                id, "email", "Email", "Email channel", "envelope",
                true, "com.example.EmailImpl", now, now, true
        );

        assertEquals(id, to.id());
        assertEquals("email", to.code());
        assertEquals("Email", to.name());
        assertEquals("Email channel", to.description());
        assertEquals("envelope", to.icon());
        assertTrue(to.enabled());
        assertEquals("com.example.EmailImpl", to.implementationClass());
        assertEquals(now, to.createdAt());
        assertEquals(now, to.updatedAt());
        assertTrue(to.active());
    }

    @Test
    @DisplayName("Create ChannelTypeTO with optional fields null")
    void createWithNullOptionalFields() {
        ChannelTypeTO to = new ChannelTypeTO(null, "sms", "SMS", null, null, false, null, null, null, false);

        assertNull(to.id());
        assertEquals("sms", to.code());
        assertEquals("SMS", to.name());
        assertNull(to.description());
        assertNull(to.icon());
        assertFalse(to.enabled());
        assertNull(to.implementationClass());
        assertNull(to.createdAt());
        assertNull(to.updatedAt());
        assertFalse(to.active());
    }

    @Test
    @DisplayName("Two ChannelTypeTOs with same values are equal")
    void equalityBetweenIdenticalRecords() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ChannelTypeTO a = new ChannelTypeTO(id, "email", "Email", null, null, true, null, now, now, true);
        ChannelTypeTO b = new ChannelTypeTO(id, "email", "Email", null, null, true, null, now, now, true);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("ChannelTypeTO toString contains code and name")
    void toStringContainsKeyFields() {
        ChannelTypeTO to = new ChannelTypeTO(null, "telegram", "Telegram", null, null, true, null, null, null, true);
        String str = to.toString();
        assertTrue(str.contains("telegram"));
        assertTrue(str.contains("Telegram"));
    }
}
