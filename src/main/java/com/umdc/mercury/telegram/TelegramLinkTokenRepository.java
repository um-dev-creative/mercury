package com.umdc.mercury.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkTokenEntity, String> {

    boolean existsByApplicationAndUserIdAndExpiresAtAfter(String application, UUID userId, LocalDateTime now);

    long deleteByApplicationAndUserId(String application, UUID userId);

    /** Deletes the token and says whether this call was the one that did (single use, safe under a race). */
    @Modifying
    @Query("delete from TelegramLinkTokenEntity t where t.tokenHash = :hash")
    int consume(@Param("hash") String hash);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
