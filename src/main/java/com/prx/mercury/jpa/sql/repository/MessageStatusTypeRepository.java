package com.prx.mercury.jpa.sql.repository;

import com.prx.mercury.jpa.sql.entity.MessageStatusTypeEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageStatusTypeRepository extends JpaRepository<MessageStatusTypeEntity, UUID> {
    MessageStatusTypeEntity findByName(@Size(max = 120) @NotNull String name);
}
