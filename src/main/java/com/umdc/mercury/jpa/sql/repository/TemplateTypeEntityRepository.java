package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.TemplateTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TemplateTypeEntityRepository extends JpaRepository<TemplateTypeEntity, UUID>, JpaSpecificationExecutor<TemplateTypeEntity> {
}
