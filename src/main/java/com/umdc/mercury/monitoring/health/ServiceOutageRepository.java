package com.umdc.mercury.monitoring.health;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ServiceOutageRepository extends JpaRepository<ServiceOutageEntity, UUID> {

    /** The filter shared by {@link #search} and its count. Empty {@code service}/{@code status} and a {@code pattern} of {@code %} match all. */
    String SEARCH_WHERE = " from ServiceOutageEntity o where o.startedAt < :to and (o.endedAt is null or o.endedAt >= :from)"
            + " and (:service = '' or o.service = :service) and (:status = '' or o.status = :status)"
            + " and (:pattern = '%' or lower(o.service) like :pattern escape '!' or lower(o.causeCode) like :pattern escape '!'"
            + " or lower(o.causeText) like :pattern escape '!')";

    /** The outages still going on. */
    List<ServiceOutageEntity> findByEndedAtIsNull();

    /**
     * One page of the outages that overlap {@code [from, to)} (they began before {@code to} and are still going on or ended at or after
     * {@code from}), newest first. {@code pattern} is a lower-case {@code LIKE} pattern whose wildcards in the searched text are escaped with
     * {@code !}.
     */
    @Query(value = "select o" + SEARCH_WHERE + " order by o.startedAt desc", countQuery = "select count(o)" + SEARCH_WHERE)
    Page<ServiceOutageEntity> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("service") String service,
                                     @Param("status") String status, @Param("pattern") String pattern, Pageable pageable);

    /** Removes the outages that ended before a moment (retention). */
    @Modifying
    @Query("delete from ServiceOutageEntity o where o.endedAt is not null and o.endedAt < :before")
    int deleteEndedBefore(@Param("before") LocalDateTime before);
}
