package com.prx.mercury.jpa.entity;

import com.prx.mercury.jpa.sql.entity.TemplateTypeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateTypeEntityTest {

    @Test
    @DisplayName("Create TemplateTypeEntity with valid data")
    void createTemplateTypeEntityWithValidData() {
        TemplateTypeEntity templateTypeEntity = new TemplateTypeEntity();
        UUID id = UUID.randomUUID();
        templateTypeEntity.setId(id);
        templateTypeEntity.setName("Test Name");
        templateTypeEntity.setDescription("Test Description");
        templateTypeEntity.setCreatedAt(LocalDateTime.now());
        templateTypeEntity.setUpdatedAt(LocalDateTime.now());
        templateTypeEntity.setActive(true);

        assertEquals(id, templateTypeEntity.getId());
        assertEquals("Test Name", templateTypeEntity.getName());
        assertEquals("Test Description", templateTypeEntity.getDescription());
        assertNotNull(templateTypeEntity.getCreatedAt());
        assertNotNull(templateTypeEntity.getUpdatedAt());
        assertTrue(templateTypeEntity.getActive());
    }

    @Test
    @DisplayName("Set and get name")
    void setAndGetName() {
        TemplateTypeEntity templateTypeEntity = new TemplateTypeEntity();
        templateTypeEntity.setName("New Name");

        assertEquals("New Name", templateTypeEntity.getName());
    }

    @Test
    @DisplayName("Set and get description")
    void setAndGetDescription() {
        TemplateTypeEntity templateTypeEntity = new TemplateTypeEntity();
        templateTypeEntity.setDescription("New Description");

        assertEquals("New Description", templateTypeEntity.getDescription());
    }

    @Test
    @DisplayName("Set and get created at")
    void setAndGetCreatedAt() {
        TemplateTypeEntity templateTypeEntity = new TemplateTypeEntity();
        LocalDateTime now = LocalDateTime.now();
        templateTypeEntity.setCreatedAt(now);

        assertEquals(now, templateTypeEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Set and get updated at")
    void setAndGetUpdatedAt() {
        TemplateTypeEntity templateTypeEntity = new TemplateTypeEntity();
        LocalDateTime now = LocalDateTime.now();
        templateTypeEntity.setUpdatedAt(now);

        assertEquals(now, templateTypeEntity.getUpdatedAt());
    }

    @Test
    @DisplayName("Set and get is active")
    void setAndGetIsActive() {
        TemplateTypeEntity templateTypeEntity = new TemplateTypeEntity();
        templateTypeEntity.setActive(false);

        assertFalse(templateTypeEntity.getActive());
    }
}
