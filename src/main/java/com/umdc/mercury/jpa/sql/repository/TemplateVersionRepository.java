package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.TemplateVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersionEntity, UUID> {

    /// The versions of a template, newest first.
    List<TemplateVersionEntity> findByTemplateIdOrderByVersionDesc(UUID templateId);

    Optional<TemplateVersionEntity> findByTemplateIdAndVersion(UUID templateId, Integer version);

    Optional<TemplateVersionEntity> findFirstByTemplateIdOrderByVersionDesc(UUID templateId);

    /// Removes the versions older than {@code version}.
    void deleteByTemplateIdAndVersionLessThan(UUID templateId, Integer version);
}
