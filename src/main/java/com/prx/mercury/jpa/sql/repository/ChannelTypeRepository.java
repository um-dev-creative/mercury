package com.prx.mercury.jpa.sql.repository;

import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelTypeRepository extends JpaRepository<ChannelTypeEntity, UUID> {

    Optional<ChannelTypeEntity> findByCode(String code);

    List<ChannelTypeEntity> findByEnabledTrue();

    List<ChannelTypeEntity> findByActiveTrue();

    List<ChannelTypeEntity> findByEnabledTrueAndActiveTrue();

    boolean existsByCode(String code);
}
