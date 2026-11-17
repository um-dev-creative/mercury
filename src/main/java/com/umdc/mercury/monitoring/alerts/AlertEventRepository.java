package com.umdc.mercury.monitoring.alerts;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertEventRepository extends JpaRepository<AlertEventEntity, UUID> {

    Optional<AlertEventEntity> findFirstByRuleIdAndStatus(UUID ruleId, String status);

    /** The alerts of an application that started since a moment or still fire, newest first. */
    @Query("select e from AlertEventEntity e where e.applicationId = :application and (e.startedAt >= :since or e.status = 'FIRING') order by e.startedAt desc")
    List<AlertEventEntity> recent(@Param("application") UUID application, @Param("since") LocalDateTime since, Pageable limit);

    long countByApplicationIdAndStatus(UUID applicationId, String status);

    /** The alerts of every application that started since a moment or still fire, newest first. */
    @Query("select e from AlertEventEntity e where (e.startedAt >= :since or e.status = 'FIRING') order by e.startedAt desc")
    List<AlertEventEntity> recentAll(@Param("since") LocalDateTime since, Pageable limit);

    long countByStatus(String status);

    /** Removes the resolved alerts that ended before a moment (retention). */
    @Modifying
    @Query("delete from AlertEventEntity e where e.status = 'RESOLVED' and e.resolvedAt < :before")
    int deleteResolvedBefore(@Param("before") LocalDateTime before);
}
