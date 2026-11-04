package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.TemplateNotFoundException;
import com.umdc.mercury.api.v1.to.TemplateVersionResponse;
import com.umdc.mercury.jpa.sql.entity.TemplateVersionEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateVersionsTest {

    @Mock private TemplateVersionRepository repository;

    private TemplateVersions versions;
    private final UUID templateId = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        versions = new TemplateVersions(repository, 3);
    }

    private TemplateVersionEntity entity(int version, String content) {
        TemplateVersionEntity entity = new TemplateVersionEntity();
        entity.setTemplateId(templateId);
        entity.setVersion(version);
        entity.setEtag("e" + version);
        entity.setContent(content);
        entity.setSizeBytes(content.length());
        entity.setReason("SAVE");
        entity.setCreatedBy(user);
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return entity;
    }

    @Test
    void theFirstRecordIsVersionOneWithItsSizeInBytes() {
        when(repository.findFirstByTemplateIdOrderByVersionDesc(templateId)).thenReturn(Optional.empty());

        int version = versions.append(templateId, "é", "abc", TemplateVersions.SAVE, user);

        ArgumentCaptor<TemplateVersionEntity> saved = ArgumentCaptor.forClass(TemplateVersionEntity.class);
        verify(repository).save(saved.capture());
        assertThat(version).isEqualTo(1);
        assertThat(saved.getValue().getVersion()).isEqualTo(1);
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(2);
        assertThat(saved.getValue().getReason()).isEqualTo("SAVE");
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(user);
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
        verify(repository, never()).deleteByTemplateIdAndVersionLessThan(any(), anyInt());
    }

    @Test
    void theNextRecordFollowsTheLatest() {
        when(repository.findFirstByTemplateIdOrderByVersionDesc(templateId)).thenReturn(Optional.of(entity(2, "x")));

        assertThat(versions.append(templateId, "y", "e3", TemplateVersions.RESTORE, user)).isEqualTo(3);
    }

    @Test
    void olderVersionsBeyondTheLimitAreDeleted() {
        when(repository.findFirstByTemplateIdOrderByVersionDesc(templateId)).thenReturn(Optional.of(entity(5, "x")));

        versions.append(templateId, "y", "e6", TemplateVersions.SAVE, user);

        // keeps 6, 5 and 4
        verify(repository).deleteByTemplateIdAndVersionLessThan(templateId, 4);
    }

    @Test
    void theLimitIsAtLeastOne() {
        TemplateVersions tiny = new TemplateVersions(repository, 0);
        when(repository.findFirstByTemplateIdOrderByVersionDesc(templateId)).thenReturn(Optional.of(entity(1, "x")));

        tiny.append(templateId, "y", "e2", TemplateVersions.SAVE, null);

        verify(repository).deleteByTemplateIdAndVersionLessThan(templateId, 2);
    }

    @Test
    void hasAnyReflectsTheHistory() {
        when(repository.findFirstByTemplateIdOrderByVersionDesc(templateId)).thenReturn(Optional.empty()).thenReturn(Optional.of(entity(1, "x")));

        assertThat(versions.hasAny(templateId)).isFalse();
        assertThat(versions.hasAny(templateId)).isTrue();
    }

    @Test
    void theListHasNoContent() {
        when(repository.findByTemplateIdOrderByVersionDesc(templateId)).thenReturn(List.of(entity(2, "dos"), entity(1, "uno")));

        List<TemplateVersionResponse> list = versions.list(templateId);

        assertThat(list).extracting(TemplateVersionResponse::version).containsExactly(2, 1);
        assertThat(list).extracting(TemplateVersionResponse::content).containsOnlyNulls();
        assertThat(list.get(0).sizeBytes()).isEqualTo(3);
    }

    @Test
    void oneVersionHasItsContent() {
        when(repository.findByTemplateIdAndVersion(templateId, 1)).thenReturn(Optional.of(entity(1, "uno")));

        TemplateVersionResponse version = versions.get(templateId, 1);

        assertThat(version.content()).isEqualTo("uno");
        assertThat(version.etag()).isEqualTo("e1");
        assertThat(version.createdBy()).isEqualTo(user);
    }

    @Test
    void aMissingVersionIs404() {
        when(repository.findByTemplateIdAndVersion(templateId, 9)).thenReturn(Optional.empty());

        assertThrows(TemplateNotFoundException.class, () -> versions.get(templateId, 9));
    }
}
