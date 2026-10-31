package com.umdc.mercury.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** A pending deep-link token, stored as its SHA-256 hash (T-113). Single use: redeeming deletes it. */
@Entity
@Table(name = "telegram_link_tokens", schema = "mercury")
public class TelegramLinkTokenEntity {

    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "application", nullable = false, length = 100)
    private String application;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "language", nullable = false, length = 5)
    private String language;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
