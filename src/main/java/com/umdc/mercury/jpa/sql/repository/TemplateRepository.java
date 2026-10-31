package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID>, JpaSpecificationExecutor<TemplateEntity> {
}
