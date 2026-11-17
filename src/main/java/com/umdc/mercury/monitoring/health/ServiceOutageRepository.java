package com.umdc.mercury.monitoring.health;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ServiceOutageRepository extends JpaRepository<ServiceOutageEntity, UUID> {

    /** The outages still going on. */
    List<ServiceOutageEntity> findByEndedAtIsNull();

    /** Outages that overlap a range that starts at {@code since}: they began after it, are still going on, or ended after it. Newest first. */
    @Query("select o from ServiceOutageEntity o where o.startedAt >= :since or o.endedAt is null or o.endedAt >= :since order by o.startedAt desc")
    List<ServiceOutageEntity> findOverlapping(@Param("since") LocalDateTime since);

    /** Removes the outages that ended before a moment (retention). */
    @Modifying
    @Query("delete from ServiceOutageEntity o where o.endedAt is not null and o.endedAt < :before")
    int deleteEndedBefore(@Param("before") LocalDateTime before);
}
