package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.ChannelConfigEntity;
import com.umdc.mercury.jpa.sql.entity.ChannelTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelConfigRepository extends JpaRepository<ChannelConfigEntity, UUID> {

    Optional<ChannelConfigEntity> findByChannelTypeAndApplicationId(
            ChannelTypeEntity channelType,
            UUID applicationId
    );

    Optional<ChannelConfigEntity> findByChannelType(ChannelTypeEntity channelType);
}
