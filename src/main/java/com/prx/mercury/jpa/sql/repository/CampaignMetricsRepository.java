package com.prx.mercury.jpa.sql.repository;

import com.prx.mercury.jpa.sql.entity.CampaignMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMetricsRepository extends JpaRepository<CampaignMetricsEntity, UUID> {

    Optional<CampaignMetricsEntity> findByCampaign_Id(UUID campaignId);
}
