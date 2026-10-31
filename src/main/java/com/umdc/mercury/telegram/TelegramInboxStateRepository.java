package com.umdc.mercury.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TelegramInboxStateRepository extends JpaRepository<TelegramInboxStateEntity, String> {

    @Modifying
    @Query(value = "insert into mercury.telegram_inbox_state (application) values (:application) on conflict do nothing", nativeQuery = true)
    int ensure(@Param("application") String application);

    /** 1 when {@code updateId} is newer than the last handled one (and is now recorded), 0 for a replay. */
    @Modifying
    @Query("update TelegramInboxStateEntity s set s.lastUpdateId = :id where s.application = :application and s.lastUpdateId < :id")
    int advance(@Param("application") String application, @Param("id") long updateId);

    /** 1 when {@code owner} holds the lease now (free, expired or already its own). */
    @Modifying
    @Query("update TelegramInboxStateEntity s set s.leaseOwner = :owner, s.leaseUntil = :until where s.application = :application "
            + "and (s.leaseOwner is null or s.leaseOwner = :owner or s.leaseUntil < :now)")
    int acquire(@Param("application") String application, @Param("owner") String owner,
                @Param("now") LocalDateTime now, @Param("until") LocalDateTime until);
}
