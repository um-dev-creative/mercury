package com.prx.mercury.kafka.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TelegramMessageTOTest {

    @Test
    @DisplayName("defaults parse mode to HTML and disableNotification to false")
    void defaultsAreApplied() {
        TelegramMessageTO messageTO = new TelegramMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                123456789L,
                "Hello",
                null,
                LocalDateTime.now(),
                Map.of(),
                null,
                null,
                null
        );

        assertEquals("HTML", messageTO.parseMode());
        assertFalse(messageTO.disableNotification());
    }

    @Test
    @DisplayName("custom values override defaults")
    void acceptsCustomValues() {
        TelegramMessageTO messageTO = new TelegramMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                123L,
                "Markdown message",
                "MarkdownV2",
                null,
                null,
                UUID.randomUUID(),
                99L,
                true
        );

        assertEquals("MarkdownV2", messageTO.parseMode());
        assertTrue(messageTO.disableNotification());
        assertEquals(99L, messageTO.replyToMessageId());
        assertTrue(messageTO.toString().contains("parseMode='MarkdownV2"));
    }
}

