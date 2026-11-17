package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TemplateDefinedRepository extends JpaRepository<TemplateDefinedEntity, UUID>, JpaSpecificationExecutor<TemplateDefinedEntity> {

    /** Active template definitions of an application. */
    java.util.List<TemplateDefinedEntity> findByApplicationIdAndIsActiveTrue(java.util.UUID applicationId);

    /** Every definition (active or not) of a template. */
    java.util.List<TemplateDefinedEntity> findByTemplateId(java.util.UUID templateId);

    /** The ids of every definition (active or not) of an application: what ties a queued message to the application it belongs to. */
    @org.springframework.data.jpa.repository.Query("select t.id from TemplateDefinedEntity t where t.applicationId = :application")
    java.util.List<java.util.UUID> findIdsByApplicationId(@org.springframework.data.repository.query.Param("application") java.util.UUID application);
}
