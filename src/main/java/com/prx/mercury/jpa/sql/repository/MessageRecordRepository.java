package com.prx.mercury.jpa.sql.repository;

import com.prx.mercury.jpa.sql.entity.MessageRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface MessageRecordRepository extends JpaRepository<MessageRecordEntity, UUID>, JpaSpecificationExecutor<MessageRecordEntity> {

}
