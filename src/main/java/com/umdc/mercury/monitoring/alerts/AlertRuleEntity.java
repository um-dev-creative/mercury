package com.umdc.mercury.monitoring.alerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** One alert rule of an application. */
@Entity
@Table(name = "alert_rules", schema = "mercury")
public class AlertRuleEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "threshold", nullable = false)
    private BigDecimal threshold;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes;

    @Column(name = "min_volume", nullable = false)
    private int minVolume;

    @Column(name = "service", length = 30)
    private String service;

    @Column(name = "notify_emails", length = 1500)
    private String notifyEmails;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
    public int getMinVolume() { return minVolume; }
    public void setMinVolume(int minVolume) { this.minVolume = minVolume; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getNotifyEmails() { return notifyEmails; }
    public void setNotifyEmails(String notifyEmails) { this.notifyEmails = notifyEmails; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
