package com.prx.mercury.jpa.sql.repository;

import com.prx.mercury.jpa.sql.entity.TemplateDefinedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TemplateDefinedRepository extends JpaRepository<TemplateDefinedEntity, UUID>, JpaSpecificationExecutor<TemplateDefinedEntity> {
}
