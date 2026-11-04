package com.umdc.mercury.jpa.sql.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateVersionEntityTest {

    @Test
    void holdsWhatItIsGiven() {
        UUID id = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        LocalDateTime at = LocalDateTime.of(2026, 11, 4, 10, 0);
        TemplateVersionEntity entity = new TemplateVersionEntity();

        entity.setId(id);
        entity.setTemplateId(templateId);
        entity.setVersion(3);
        entity.setEtag("abc");
        entity.setContent("hola");
        entity.setSizeBytes(4);
        entity.setReason("SAVE");
        entity.setCreatedBy(user);
        entity.setCreatedAt(at);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTemplateId()).isEqualTo(templateId);
        assertThat(entity.getVersion()).isEqualTo(3);
        assertThat(entity.getEtag()).isEqualTo("abc");
        assertThat(entity.getContent()).isEqualTo("hola");
        assertThat(entity.getSizeBytes()).isEqualTo(4);
        assertThat(entity.getReason()).isEqualTo("SAVE");
        assertThat(entity.getCreatedBy()).isEqualTo(user);
        assertThat(entity.getCreatedAt()).isEqualTo(at);
    }
}
