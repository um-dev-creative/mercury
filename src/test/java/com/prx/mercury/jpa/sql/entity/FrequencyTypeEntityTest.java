package com.prx.mercury.jpa.sql.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FrequencyTypeEntityTest {

    @Test
    @DisplayName("Create FrequencyTypeEntity with valid data")
    void createFrequencyTypeEntityWithValidData() {
        UUID id = UUID.randomUUID();
        String name = "Frequency Name";
        String description = "Frequency Description";
        Instant now = Instant.now();
        Boolean active = true;

        FrequencyTypeEntity frequencyTypeEntity = new FrequencyTypeEntity();
        frequencyTypeEntity.setId(id);
        frequencyTypeEntity.setName(name);
        frequencyTypeEntity.setDescription(description);
        frequencyTypeEntity.setCreatedAt(now);
        frequencyTypeEntity.setUpdatedAt(now);
        frequencyTypeEntity.setActive(active);

        assertNotNull(frequencyTypeEntity);
        assertEquals(id, frequencyTypeEntity.getId());
        assertEquals(name, frequencyTypeEntity.getName());
        assertEquals(description, frequencyTypeEntity.getDescription());
        assertEquals(now, frequencyTypeEntity.getCreatedAt());
        assertEquals(now, frequencyTypeEntity.getUpdatedAt());
        assertEquals(active, frequencyTypeEntity.getActive());
    }

    @Test
    @DisplayName("Create FrequencyTypeEntity with null values")
    void createFrequencyTypeEntityWithNullValues() {
        UUID id = UUID.randomUUID();
        String name = null;
        String description = null;
        Instant now = Instant.now();
        Boolean active = null;

        FrequencyTypeEntity frequencyTypeEntity = new FrequencyTypeEntity();
        frequencyTypeEntity.setId(id);
        frequencyTypeEntity.setName(name);
        frequencyTypeEntity.setDescription(description);
        frequencyTypeEntity.setCreatedAt(now);
        frequencyTypeEntity.setUpdatedAt(now);
        frequencyTypeEntity.setActive(active);

        assertNotNull(frequencyTypeEntity);
        assertEquals(id, frequencyTypeEntity.getId());
        assertNull(frequencyTypeEntity.getName());
        assertNull(frequencyTypeEntity.getDescription());
        assertEquals(now, frequencyTypeEntity.getCreatedAt());
        assertEquals(now, frequencyTypeEntity.getUpdatedAt());
        assertNull(frequencyTypeEntity.getActive());
    }

    @Test
    @DisplayName("FrequencyTypeEntity toString method")
    void frequencyTypeEntityToString() {
        UUID id = UUID.randomUUID();
        String name = "Frequency Name";
        String description = "Frequency Description";
        Instant now = Instant.now();
        Boolean active = true;

        FrequencyTypeEntity frequencyTypeEntity = new FrequencyTypeEntity();
        frequencyTypeEntity.setId(id);
        frequencyTypeEntity.setName(name);
        frequencyTypeEntity.setDescription(description);
        frequencyTypeEntity.setCreatedAt(now);
        frequencyTypeEntity.setUpdatedAt(now);
        frequencyTypeEntity.setActive(active);

        String expectedString = "FrequencyTypeEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", active=" + active +
                '}';

        assertEquals(expectedString, frequencyTypeEntity.toString());
    }
}
