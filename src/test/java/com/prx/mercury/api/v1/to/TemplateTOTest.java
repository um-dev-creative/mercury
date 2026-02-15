package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateTOTest {

    @Test
    @DisplayName("Create TemplateTO with valid data")
    void createTemplateTOWithValidData() {
        UUID id = UUID.randomUUID();
        String description = "Template Description";
        String location = "Template Location";
        String fileFormat = "Template File Format";
        TemplateTypeTO templateType = new TemplateTypeTO(UUID.randomUUID(), "Template Type", "Description", LocalDateTime.now(), LocalDateTime.now(), true);
        UUID application = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Boolean isActive = true;

        TemplateTO templateTO = new TemplateTO(
                id, description, location, fileFormat, templateType, application, now, now, isActive
        );

        assertNotNull(templateTO);
        assertEquals(id, templateTO.id());
        assertEquals(description, templateTO.description());
        assertEquals(location, templateTO.location());
        assertEquals(fileFormat, templateTO.fileFormat());
        assertEquals(templateType, templateTO.templateType());
        assertEquals(application, templateTO.application());
        assertEquals(now, templateTO.createdAt());
        assertEquals(now, templateTO.updatedAt());
        assertEquals(isActive, templateTO.isActive());
    }

    @Test
    @DisplayName("Create TemplateTO with null values")
    void createTemplateTOWithNullValues() {
        UUID id = UUID.randomUUID();
        String description = null;
        String location = null;
        String fileFormat = null;
        TemplateTypeTO templateType = null;
        UUID application = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Boolean isActive = null;

        TemplateTO templateTO = new TemplateTO(
                id, description, location, fileFormat, templateType, application, now, now, isActive
        );

        assertNotNull(templateTO);
        assertEquals(id, templateTO.id());
        assertNull(templateTO.description());
        assertNull(templateTO.location());
        assertNull(templateTO.fileFormat());
        assertNull(templateTO.templateType());
        assertEquals(application, templateTO.application());
        assertEquals(now, templateTO.createdAt());
        assertEquals(now, templateTO.updatedAt());
        assertNull(templateTO.isActive());
    }

    @Test
    @DisplayName("TemplateTO toString method")
    void templateTOToString() {
        UUID id = UUID.randomUUID();
        String description = "Template Description";
        String location = "Template Location";
        String fileFormat = "Template File Format";
        TemplateTypeTO templateType = new TemplateTypeTO(UUID.randomUUID(), "Template Type", "Description", LocalDateTime.now(), LocalDateTime.now(), true);
        UUID application = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Boolean isActive = true;

        TemplateTO templateTO = new TemplateTO(
                id, description, location, fileFormat, templateType, application, now, now, isActive
        );

        String expectedString = "TemplateTO{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", fileFormat='" + fileFormat + '\'' +
                ", templateType=" + templateType +
                ", application=" + application +
                ", createdAt=" + now +
                ", updatedAt=" + now +
                ", isActive=" + isActive +
                '}';

        assertEquals(expectedString, templateTO.toString());
    }
}
