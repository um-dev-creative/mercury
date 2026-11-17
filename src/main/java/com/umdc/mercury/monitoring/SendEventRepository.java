package com.umdc.mercury.monitoring;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Reads and housekeeping of {@link SendEventEntity}; every read is scoped to one application. */
public interface SendEventRepository extends JpaRepository<SendEventEntity, UUID>, JpaSpecificationExecutor<SendEventEntity> {

    /** Outcomes of an application since a moment, counted per channel, kind and status. */
    @Query("select e.channel as channel, e.kind as kind, e.status as status, count(e) as total from SendEventEntity e "
            + "where e.applicationId = :application and e.occurredAt >= :since group by e.channel, e.kind, e.status")
    List<OutcomeCount> countOutcomes(@Param("application") UUID application, @Param("since") LocalDateTime since);

    /** The most frequent failure causes of an application since a moment. */
    @Query("select e.errorCause as cause, e.channel as channel, count(e) as total from SendEventEntity e "
            + "where e.applicationId = :application and e.occurredAt >= :since and e.status = 'FAILED' "
            + "group by e.errorCause, e.channel order by count(e) desc")
    List<CauseCount> topCauses(@Param("application") UUID application, @Param("since") LocalDateTime since, Pageable limit);

    /**
     * Outcomes grouped in time buckets ({@code unit} is a PostgreSQL {@code date_trunc} unit: minute, hour or day). The optional channel is cast
     * so PostgreSQL can type a null parameter.
     */
    @Query(value = "select date_trunc(:unit, occurred_at) as bucket, status as status, count(*) as total from mercury.send_events "
            + "where application_id = :application and occurred_at >= :since "
            + "and (cast(:channel as varchar) is null or channel = cast(:channel as varchar)) "
            + "and (cast(:kind as varchar) is null or kind = cast(:kind as varchar)) "
            + "group by 1, 2 order by 1", nativeQuery = true)
    List<BucketCount> countByBucket(@Param("application") UUID application, @Param("since") LocalDateTime since,
                                    @Param("unit") String unit, @Param("channel") String channel, @Param("kind") String kind);

    /** Removes the outcomes older than a moment (retention). */
    @Modifying
    @Query("delete from SendEventEntity e where e.occurredAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    /** Count of outcomes per channel, kind and status. */
    interface OutcomeCount {
        String getChannel();

        String getKind();

        String getStatus();

        long getTotal();
    }

    /** Count of failures per cause and channel. */
    interface CauseCount {
        String getCause();

        String getChannel();

        long getTotal();
    }

    /** Count of outcomes of one status inside one time bucket. */
    interface BucketCount {
        LocalDateTime getBucket();

        String getStatus();

        long getTotal();
    }
}
