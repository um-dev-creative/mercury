package com.prx.mercury.jpa.entity;

import com.prx.mercury.jpa.sql.entity.ApplicationEntity;
import com.prx.mercury.jpa.sql.entity.TemplateEntity;
import com.prx.mercury.jpa.sql.entity.TemplateTypeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateEntityTest {

    @Test
    @DisplayName("Create TemplateEntity with valid data")
    void createTemplateEntityWithValidData() {
        TemplateEntity templateEntity = new TemplateEntity();
        UUID id = UUID.randomUUID();
        templateEntity.setId(id);
        templateEntity.setDescription("Test Description");
        templateEntity.setLocation("Test Location");
        templateEntity.setFileFormat("Test Format");
        templateEntity.setTemplateType(new TemplateTypeEntity());
        templateEntity.setApplication(new ApplicationEntity());
        templateEntity.setCreatedAt(LocalDateTime.now());
        templateEntity.setUpdatedAt(LocalDateTime.now());
        templateEntity.setActive(true);

        assertEquals(id, templateEntity.getId());
        assertEquals("Test Description", templateEntity.getDescription());
        assertEquals("Test Location", templateEntity.getLocation());
        assertEquals("Test Format", templateEntity.getFileFormat());
        assertNotNull(templateEntity.getTemplateType());
        assertNotNull(templateEntity.getApplication());
        assertNotNull(templateEntity.getCreatedAt());
        assertNotNull(templateEntity.getUpdatedAt());
        assertTrue(templateEntity.getActive());
    }

    @Test
    @DisplayName("Set and get description")
    void setAndGetDescription() {
        TemplateEntity templateEntity = new TemplateEntity();
        templateEntity.setDescription("New Description");

        assertEquals("New Description", templateEntity.getDescription());
    }

    @Test
    @DisplayName("Set and get location")
    void setAndGetLocation() {
        TemplateEntity templateEntity = new TemplateEntity();
        templateEntity.setLocation("New Location");

        assertEquals("New Location", templateEntity.getLocation());
    }

    @Test
    @DisplayName("Set and get file format")
    void setAndGetFileFormat() {
        TemplateEntity templateEntity = new TemplateEntity();
        templateEntity.setFileFormat("New Format");

        assertEquals("New Format", templateEntity.getFileFormat());
    }

    @Test
    @DisplayName("Set and get created at")
    void setAndGetCreatedAt() {
        TemplateEntity templateEntity = new TemplateEntity();
        LocalDateTime now = LocalDateTime.now();
        templateEntity.setCreatedAt(now);

        assertEquals(now, templateEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Set and get updated at")
    void setAndGetUpdatedAt() {
        TemplateEntity templateEntity = new TemplateEntity();
        LocalDateTime now = LocalDateTime.now();
        templateEntity.setUpdatedAt(now);

        assertEquals(now, templateEntity.getUpdatedAt());
    }

    @Test
    @DisplayName("Set and get is active")
    void setAndGetIsActive() {
        TemplateEntity templateEntity = new TemplateEntity();
        templateEntity.setActive(false);

        assertFalse(templateEntity.getActive());
    }
}
