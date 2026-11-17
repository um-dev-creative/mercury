package com.umdc.mercury.monitoring.alerts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, UUID> {

    List<AlertRuleEntity> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    List<AlertRuleEntity> findByEnabledTrue();

    long countByApplicationId(UUID applicationId);
}
