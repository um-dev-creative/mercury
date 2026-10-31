package com.umdc.mercury.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Per application: last Telegram update handled, and who polls (T-114). */
@Entity
@Table(name = "telegram_inbox_state", schema = "mercury")
public class TelegramInboxStateEntity {

    @Id
    @Column(name = "application", nullable = false, length = 100)
    private String application;
    @Column(name = "last_update_id", nullable = false)
    private long lastUpdateId;
    @Column(name = "lease_owner", length = 64)
    private String leaseOwner;
    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public void setLastUpdateId(long lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }

    public String getLeaseOwner() {
        return leaseOwner;
    }

    public void setLeaseOwner(String leaseOwner) {
        this.leaseOwner = leaseOwner;
    }

    public LocalDateTime getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(LocalDateTime leaseUntil) {
        this.leaseUntil = leaseUntil;
    }
}
