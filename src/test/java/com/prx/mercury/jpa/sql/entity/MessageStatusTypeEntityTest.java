package com.prx.mercury.jpa.sql.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageStatusTypeEntityTest {

    @Test
    @DisplayName("Create MessageStatusTypeEntity with valid data")
    void createMessageStatusTypeEntityWithValidData() {
        UUID id = UUID.randomUUID();
        String name = "Status Name";
        String description = "Status Description";
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;

        MessageStatusTypeEntity messageStatusTypeEntity = new MessageStatusTypeEntity();
        messageStatusTypeEntity.setId(id);
        messageStatusTypeEntity.setName(name);
        messageStatusTypeEntity.setDescription(description);
        messageStatusTypeEntity.setCreatedAt(now);
        messageStatusTypeEntity.setUpdatedAt(now);
        messageStatusTypeEntity.setActive(active);

        assertNotNull(messageStatusTypeEntity);
        assertEquals(id, messageStatusTypeEntity.getId());
        assertEquals(name, messageStatusTypeEntity.getName());
        assertEquals(description, messageStatusTypeEntity.getDescription());
        assertEquals(now, messageStatusTypeEntity.getCreatedAt());
        assertEquals(now, messageStatusTypeEntity.getUpdatedAt());
        assertEquals(active, messageStatusTypeEntity.getActive());
    }

    @Test
    @DisplayName("Create MessageStatusTypeEntity with null values")
    void createMessageStatusTypeEntityWithNullValues() {
        UUID id = UUID.randomUUID();
        String name = null;
        String description = null;
        LocalDateTime now = LocalDateTime.now();
        Boolean active = null;

        MessageStatusTypeEntity messageStatusTypeEntity = new MessageStatusTypeEntity();
        messageStatusTypeEntity.setId(id);
        messageStatusTypeEntity.setName(name);
        messageStatusTypeEntity.setDescription(description);
        messageStatusTypeEntity.setCreatedAt(now);
        messageStatusTypeEntity.setUpdatedAt(now);
        messageStatusTypeEntity.setActive(active);

        assertNotNull(messageStatusTypeEntity);
        assertEquals(id, messageStatusTypeEntity.getId());
        assertNull(messageStatusTypeEntity.getName());
        assertNull(messageStatusTypeEntity.getDescription());
        assertEquals(now, messageStatusTypeEntity.getCreatedAt());
        assertEquals(now, messageStatusTypeEntity.getUpdatedAt());
        assertNull(messageStatusTypeEntity.getActive());
    }

    @Test
    @DisplayName("MessageStatusTypeEntity toString method")
    void messageStatusTypeEntityToString() {
        UUID id = UUID.randomUUID();
        String name = "Status Name";
        String description = "Status Description";
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;

        MessageStatusTypeEntity messageStatusTypeEntity = new MessageStatusTypeEntity();
        messageStatusTypeEntity.setId(id);
        messageStatusTypeEntity.setName(name);
        messageStatusTypeEntity.setDescription(description);
        messageStatusTypeEntity.setCreatedAt(now);
        messageStatusTypeEntity.setUpdatedAt(now);
        messageStatusTypeEntity.setActive(active);

        String expectedString = "MessageStatusTypeEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", active=" + active +
                '}';

        assertEquals(expectedString, messageStatusTypeEntity.toString());
    }
}
