package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.SeverityTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SeverityTypeRepository extends JpaRepository<SeverityTypeEntity, UUID> {
}
