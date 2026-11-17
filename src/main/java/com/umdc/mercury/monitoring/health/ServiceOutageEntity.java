package com.umdc.mercury.monitoring.health;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** One outage of a service; {@code endedAt} stays {@code null} while it lasts. The cause is a fixed code and sentence, never a raw error text. */
@Entity
@Table(name = "service_outages", schema = "mercury")
public class ServiceOutageEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "service", nullable = false, length = 30)
    private String service;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "cause_code", nullable = false, length = 40)
    private String causeCode;

    @Column(name = "cause_text", nullable = false, length = 200)
    private String causeText;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCauseCode() {
        return causeCode;
    }

    public void setCauseCode(String causeCode) {
        this.causeCode = causeCode;
    }

    public String getCauseText() {
        return causeText;
    }

    public void setCauseText(String causeText) {
        this.causeText = causeText;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
