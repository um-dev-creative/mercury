package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateDefinedTOTest {

    @Test
    @DisplayName("Create TemplateDefinedTO with valid data")
    void createTemplateDefinedTOWithValidData() {
        UUID id = UUID.randomUUID();
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "Description", "Location", "File Format", new TemplateTypeTO(UUID.randomUUID(), "Type", "Description", LocalDateTime.now(), LocalDateTime.now(), true), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), true);
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;
        UUID frequencyTypeId = UUID.randomUUID();

        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(
                id, template, userId, applicationId, now, now, now.plusDays(1), active, frequencyTypeId
        );

        assertNotNull(templateDefinedTO);
        assertEquals(id, templateDefinedTO.id());
        assertEquals(template, templateDefinedTO.template());
        assertEquals(userId, templateDefinedTO.userId());
        assertEquals(applicationId, templateDefinedTO.applicationId());
        assertEquals(now, templateDefinedTO.createdAt());
        assertEquals(now, templateDefinedTO.updatedAt());
        assertEquals(now.plusDays(1), templateDefinedTO.expiredAt());
        assertEquals(active, templateDefinedTO.active());
        assertEquals(frequencyTypeId, templateDefinedTO.frequencyTypeId());
    }

    @Test
    @DisplayName("Create TemplateDefinedTO with null values")
    void createTemplateDefinedTOWithNullValues() {
        UUID id = UUID.randomUUID();
        TemplateTO template = null;
        UUID userId = null;
        UUID applicationId = null;
        LocalDateTime now = LocalDateTime.now();
        Boolean active = null;
        UUID frequencyTypeId = null;

        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(
                id, template, userId, applicationId, now, now, now.plusDays(1), active, frequencyTypeId
        );

        assertNotNull(templateDefinedTO);
        assertEquals(id, templateDefinedTO.id());
        assertNull(templateDefinedTO.template());
        assertNull(templateDefinedTO.userId());
        assertNull(templateDefinedTO.applicationId());
        assertEquals(now, templateDefinedTO.createdAt());
        assertEquals(now, templateDefinedTO.updatedAt());
        assertEquals(now.plusDays(1), templateDefinedTO.expiredAt());
        assertNull(templateDefinedTO.active());
        assertNull(templateDefinedTO.frequencyTypeId());
    }

    @Test
    @DisplayName("TemplateDefinedTO toString method")
    void templateDefinedTOToString() {
        UUID id = UUID.randomUUID();
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "Description", "Location", "File Format", new TemplateTypeTO(UUID.randomUUID(), "Type", "Description", LocalDateTime.now(), LocalDateTime.now(), true), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), true);
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;
        UUID frequencyTypeId = UUID.randomUUID();

        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(
                id, template, userId, applicationId, now, now, now.plusDays(1), active, frequencyTypeId
        );

        String expectedString = "TemplateDefinedTO{" +
                "id=" + id +
                ", template=" + template +
                ", userId=" + userId +
                ", applicationId=" + applicationId +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", expiredAt=" + now.plusDays(1) +
                ", active=" + active +
                ", frequencyTypeId=" + frequencyTypeId +
                '}';

        assertEquals(expectedString, templateDefinedTO.toString());
    }
}
