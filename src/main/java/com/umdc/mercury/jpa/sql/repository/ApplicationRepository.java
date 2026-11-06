package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

    /// Applications whose `code_name` equals the given one, ignoring case.
    List<ApplicationEntity> findAllByCodeNameIgnoreCase(String codeName);
}
