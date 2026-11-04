package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TemplateDefinedRepository extends JpaRepository<TemplateDefinedEntity, UUID>, JpaSpecificationExecutor<TemplateDefinedEntity> {

    /** Active template definitions of an application. */
    java.util.List<TemplateDefinedEntity> findByApplication_IdAndIsActiveTrue(java.util.UUID applicationId);
}
