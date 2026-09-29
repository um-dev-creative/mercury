package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
}
