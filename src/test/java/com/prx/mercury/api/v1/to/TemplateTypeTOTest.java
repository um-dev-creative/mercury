package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateTypeTOTest {

    @Test
    @DisplayName("Create TemplateTypeTO with valid data")
    void createTemplateTypeTOWithValidData() {
        UUID id = UUID.randomUUID();
        String name = "Template Name";
        String description = "Template Description";
        LocalDateTime now = LocalDateTime.now();
        Boolean isActive = true;

        TemplateTypeTO templateTypeTO = new TemplateTypeTO(
                id, name, description, now, now, isActive
        );

        assertNotNull(templateTypeTO);
        assertEquals(id, templateTypeTO.id());
        assertEquals(name, templateTypeTO.name());
        assertEquals(description, templateTypeTO.description());
        assertEquals(now, templateTypeTO.createdAt());
        assertEquals(now, templateTypeTO.updatedAt());
        assertEquals(isActive, templateTypeTO.isActive());
    }

    @Test
    @DisplayName("Create TemplateTypeTO with null values")
    void createTemplateTypeTOWithNullValues() {
        UUID id = UUID.randomUUID();
        String name = null;
        String description = null;
        LocalDateTime now = LocalDateTime.now();
        Boolean isActive = null;

        TemplateTypeTO templateTypeTO = new TemplateTypeTO(
                id, name, description, now, now, isActive
        );

        assertNotNull(templateTypeTO);
        assertEquals(id, templateTypeTO.id());
        assertNull(templateTypeTO.name());
        assertNull(templateTypeTO.description());
        assertEquals(now, templateTypeTO.createdAt());
        assertEquals(now, templateTypeTO.updatedAt());
        assertNull(templateTypeTO.isActive());
    }

    @Test
    @DisplayName("TemplateTypeTO toString method")
    void templateTypeTOToString() {
        UUID id = UUID.randomUUID();
        String name = "Template Name";
        String description = "Template Description";
        LocalDateTime now = LocalDateTime.now();
        Boolean isActive = true;

        TemplateTypeTO templateTypeTO = new TemplateTypeTO(
                id, name, description, now, now, isActive
        );

        String expectedString = "TemplateTypeTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", isActive=" + isActive +
                '}';

        assertEquals(expectedString, templateTypeTO.toString());
    }
}
