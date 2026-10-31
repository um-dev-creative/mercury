package com.umdc.mercury.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramLinkRepository extends JpaRepository<TelegramLinkEntity, UUID> {

    Optional<TelegramLinkEntity> findByApplicationAndUserId(String application, UUID userId);

    Optional<TelegramLinkEntity> findByApplicationAndChatId(String application, long chatId);

    long deleteByApplicationAndUserId(String application, UUID userId);
}
