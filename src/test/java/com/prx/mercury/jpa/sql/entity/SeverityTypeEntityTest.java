package com.prx.mercury.jpa.sql.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SeverityTypeEntityTest {

    @Test
    @DisplayName("Create SeverityTypeEntity with valid data")
    void createSeverityTypeEntityWithValidData() {
        UUID id = UUID.randomUUID();
        String name = "Severity Name";
        String description = "Severity Description";
        Instant now = Instant.now();
        Boolean active = true;

        SeverityTypeEntity severityTypeEntity = new SeverityTypeEntity();
        severityTypeEntity.setId(id);
        severityTypeEntity.setName(name);
        severityTypeEntity.setDescription(description);
        severityTypeEntity.setCreatedAt(now);
        severityTypeEntity.setUpdatedAt(now);
        severityTypeEntity.setActive(active);

        assertNotNull(severityTypeEntity);
        assertEquals(id, severityTypeEntity.getId());
        assertEquals(name, severityTypeEntity.getName());
        assertEquals(description, severityTypeEntity.getDescription());
        assertEquals(now, severityTypeEntity.getCreatedAt());
        assertEquals(now, severityTypeEntity.getUpdatedAt());
        assertEquals(active, severityTypeEntity.getActive());
    }

    @Test
    @DisplayName("Create SeverityTypeEntity with null values")
    void createSeverityTypeEntityWithNullValues() {
        UUID id = UUID.randomUUID();
        String name = null;
        String description = null;
        Instant now = Instant.now();
        Boolean active = null;

        SeverityTypeEntity severityTypeEntity = new SeverityTypeEntity();
        severityTypeEntity.setId(id);
        severityTypeEntity.setName(name);
        severityTypeEntity.setDescription(description);
        severityTypeEntity.setCreatedAt(now);
        severityTypeEntity.setUpdatedAt(now);
        severityTypeEntity.setActive(active);

        assertNotNull(severityTypeEntity);
        assertEquals(id, severityTypeEntity.getId());
        assertNull(severityTypeEntity.getName());
        assertNull(severityTypeEntity.getDescription());
        assertEquals(now, severityTypeEntity.getCreatedAt());
        assertEquals(now, severityTypeEntity.getUpdatedAt());
        assertNull(severityTypeEntity.getActive());
    }

    @Test
    @DisplayName("SeverityTypeEntity toString method")
    void severityTypeEntityToString() {
        UUID id = UUID.randomUUID();
        String name = "Severity Name";
        String description = "Severity Description";
        Instant now = Instant.now();
        Boolean active = true;

        SeverityTypeEntity severityTypeEntity = new SeverityTypeEntity();
        severityTypeEntity.setId(id);
        severityTypeEntity.setName(name);
        severityTypeEntity.setDescription(description);
        severityTypeEntity.setCreatedAt(now);
        severityTypeEntity.setUpdatedAt(now);
        severityTypeEntity.setActive(active);

        String expectedString = "SeverityTypeEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", active=" + active +
                '}';

        assertEquals(expectedString, severityTypeEntity.toString());
    }
}
