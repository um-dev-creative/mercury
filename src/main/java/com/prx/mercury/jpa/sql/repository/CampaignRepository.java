package com.prx.mercury.jpa.sql.repository;

import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID> {

    Page<CampaignEntity> findByUserId(UUID userId, Pageable pageable);

    Page<CampaignEntity> findByChannelType(ChannelTypeEntity channelType, Pageable pageable);

    Page<CampaignEntity> findByStatus(String status, Pageable pageable);

    @Query("SELECT c FROM CampaignEntity c WHERE c.channelType = :channelType AND c.status = :status")
    Page<CampaignEntity> findByChannelTypeAndStatus(
            ChannelTypeEntity channelType,
            String status,
            Pageable pageable
    );
}
