package com.umdc.mercury.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** A user of an application linked to a private Telegram chat (T-113). The {@code chatId} never leaves mercury. */
@Entity
@Table(name = "telegram_links", schema = "mercury")
public class TelegramLinkEntity {

    public static final String ACTIVE = "ACTIVE";
    public static final String BLOCKED = "BLOCKED";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "application", nullable = false, length = 100)
    private String application;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "chat_id", nullable = false)
    private long chatId;
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    @Column(name = "language", nullable = false, length = 5)
    private String language;
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(LocalDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
