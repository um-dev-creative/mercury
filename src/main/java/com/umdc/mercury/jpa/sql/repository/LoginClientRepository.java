package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.LoginClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoginClientRepository extends JpaRepository<LoginClientEntity, UUID> {

    /// The login client registered under this alias, active or not.
    Optional<LoginClientEntity> findByAlias(String alias);
}
