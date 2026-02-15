package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageStatusTypeTOTest {

    @Test
    @DisplayName("Create MessageStatusTypeTO with valid data")
    void createMessageStatusTypeTOWithValidData() {
        UUID id = UUID.randomUUID();
        String name = "Status Name";
        String description = "Status Description";
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;

        MessageStatusTypeTO messageStatusTypeTO = new MessageStatusTypeTO(
                id, name, description, now, now, active
        );

        assertNotNull(messageStatusTypeTO);
        assertEquals(id, messageStatusTypeTO.id());
        assertEquals(name, messageStatusTypeTO.name());
        assertEquals(description, messageStatusTypeTO.description());
        assertEquals(now, messageStatusTypeTO.createdAt());
        assertEquals(now, messageStatusTypeTO.updatedAt());
        assertEquals(active, messageStatusTypeTO.active());
    }

    @Test
    @DisplayName("Create MessageStatusTypeTO with null values")
    void createMessageStatusTypeTOWithNullValues() {
        UUID id = UUID.randomUUID();
        String name = null;
        String description = null;
        LocalDateTime now = LocalDateTime.now();
        Boolean active = null;

        MessageStatusTypeTO messageStatusTypeTO = new MessageStatusTypeTO(
                id, name, description, now, now, active
        );

        assertNotNull(messageStatusTypeTO);
        assertEquals(id, messageStatusTypeTO.id());
        assertNull(messageStatusTypeTO.name());
        assertNull(messageStatusTypeTO.description());
        assertEquals(now, messageStatusTypeTO.createdAt());
        assertEquals(now, messageStatusTypeTO.updatedAt());
        assertNull(messageStatusTypeTO.active());
    }

    @Test
    @DisplayName("MessageStatusTypeTO toString method")
    void messageStatusTypeTOToString() {
        UUID id = UUID.randomUUID();
        String name = "Status Name";
        String description = "Status Description";
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;

        MessageStatusTypeTO messageStatusTypeTO = new MessageStatusTypeTO(
                id, name, description, now, now, active
        );

        String expectedString = "MessageStatusTypeTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", active=" + active +
                '}';

        assertEquals(expectedString, messageStatusTypeTO.toString());
    }
}
