package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.CampaignMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMetricsRepository extends JpaRepository<CampaignMetricsEntity, UUID> {

    Optional<CampaignMetricsEntity> findByCampaign_Id(UUID campaignId);

    /**
     * Atomically increments the sent counter. Written as an UPDATE rather than a
     * load-modify-save because channel messages are processed concurrently, and a
     * read-modify-write would lose increments. {@code coalesce} guards rows whose
     * counters were inserted as NULL.
     *
     * @return the number of metrics rows updated (0 when the campaign has none)
     */
    @Modifying
    @Transactional
    @Query("update CampaignMetricsEntity m set m.totalSent = coalesce(m.totalSent, 0) + 1, "
            + "m.lastUpdated = :now where m.campaign.id = :campaignId")
    int incrementTotalSent(@Param("campaignId") UUID campaignId, @Param("now") LocalDateTime now);

    /**
     * Atomically increments the failed counter.
     *
     * @return the number of metrics rows updated (0 when the campaign has none)
     */
    @Modifying
    @Transactional
    @Query("update CampaignMetricsEntity m set m.failed = coalesce(m.failed, 0) + 1, "
            + "m.lastUpdated = :now where m.campaign.id = :campaignId")
    int incrementFailed(@Param("campaignId") UUID campaignId, @Param("now") LocalDateTime now);
}
