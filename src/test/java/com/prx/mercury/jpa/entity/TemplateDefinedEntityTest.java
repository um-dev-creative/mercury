package com.prx.mercury.jpa.entity;

import com.prx.mercury.jpa.sql.entity.ApplicationEntity;
import com.prx.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.prx.mercury.jpa.sql.entity.TemplateEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateDefinedEntityTest {

    @Test
    @DisplayName("Create TemplateUsageEntity with valid data")
    void createTemplateUsageEntityWithValidData() {
        TemplateDefinedEntity templateDefinedEntity = new TemplateDefinedEntity();
        UUID id = UUID.randomUUID();
        templateDefinedEntity.setId(id);
        templateDefinedEntity.setTemplate(new TemplateEntity());
        templateDefinedEntity.setUserId(UUID.randomUUID());
        templateDefinedEntity.setApplication(new ApplicationEntity());
        templateDefinedEntity.setCreatedAt(LocalDateTime.now());
        templateDefinedEntity.setUpdatedAt(LocalDateTime.now());
        templateDefinedEntity.setExpiredAt(LocalDateTime.now().plusDays(1));
        templateDefinedEntity.setIsActive(true);

        assertEquals(id, templateDefinedEntity.getId());
        assertNotNull(templateDefinedEntity.getTemplate());
        assertNotNull(templateDefinedEntity.getUserId());
        assertNotNull(templateDefinedEntity.getApplication());
        assertNotNull(templateDefinedEntity.getCreatedAt());
        assertNotNull(templateDefinedEntity.getUpdatedAt());
        assertNotNull(templateDefinedEntity.getExpiredAt());
        assertTrue(templateDefinedEntity.getIsActive());
    }

    @Test
    @DisplayName("Set and get template")
    void setAndGetTemplate() {
        TemplateDefinedEntity templateUsageEntity = new TemplateDefinedEntity();
        TemplateEntity templateEntity = new TemplateEntity();
        templateUsageEntity.setTemplate(templateEntity);

        assertEquals(templateEntity, templateUsageEntity.getTemplate());
    }

    @Test
    @DisplayName("Set and get user ID")
    void setAndGetUserId() {
        TemplateDefinedEntity templateUsageEntity = new TemplateDefinedEntity();
        UUID userId = UUID.randomUUID();
        templateUsageEntity.setUserId(userId);

        assertEquals(userId, templateUsageEntity.getUserId());
    }

    @Test
    @DisplayName("Set and get application")
    void setAndGetApplication() {
        TemplateDefinedEntity templateUsageEntity = new TemplateDefinedEntity();
        ApplicationEntity applicationEntity = new ApplicationEntity();
        templateUsageEntity.setApplication(applicationEntity);

        assertEquals(applicationEntity, templateUsageEntity.getApplication());
    }

    @Test
    @DisplayName("Set and get created at")
    void setAndGetCreatedAt() {
        TemplateDefinedEntity templateUsageEntity = new TemplateDefinedEntity();
        LocalDateTime now = LocalDateTime.now();
        templateUsageEntity.setCreatedAt(now);

        assertEquals(now, templateUsageEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Set and get updated at")
    void setAndGetUpdatedAt() {
        TemplateDefinedEntity templateUsageEntity = new TemplateDefinedEntity();
        LocalDateTime now = LocalDateTime.now();
        templateUsageEntity.setUpdatedAt(now);

        assertEquals(now, templateUsageEntity.getUpdatedAt());
    }

    @Test
    @DisplayName("Set and get expired at")
    void setAndGetExpiredAt() {
        TemplateDefinedEntity templateUsageEntity = new TemplateDefinedEntity();
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        templateUsageEntity.setExpiredAt(now);

        assertEquals(now, templateUsageEntity.getExpiredAt());
    }

    @Test
    @DisplayName("Set and get is active")
    void setAndGetIsActive() {
        TemplateDefinedEntity templateUsageEntity = new TemplateDefinedEntity();
        templateUsageEntity.setIsActive(false);

        assertFalse(templateUsageEntity.getIsActive());
    }
}
