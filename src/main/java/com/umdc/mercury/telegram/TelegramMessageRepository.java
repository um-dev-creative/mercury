package com.umdc.mercury.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramMessageRepository extends JpaRepository<TelegramMessageEntity, UUID> {

    Optional<TelegramMessageEntity> findByApplicationAndIdempotencyKey(String application, String idempotencyKey);

    Optional<TelegramMessageEntity> findByIdAndApplication(UUID id, String application);

    /** Due messages, locked so that two instances never take the same one. */
    @Query(value = "select * from mercury.telegram_messages where status = 'QUEUED' and next_attempt_at <= :now "
            + "order by next_attempt_at limit :limit for update skip locked", nativeQuery = true)
    List<TelegramMessageEntity> lockDue(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
