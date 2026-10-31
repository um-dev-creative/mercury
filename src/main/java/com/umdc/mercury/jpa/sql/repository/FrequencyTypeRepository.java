package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.FrequencyTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for {@link FrequencyTypeEntity}.
 */
public interface FrequencyTypeRepository extends JpaRepository<FrequencyTypeEntity, UUID> {
}
