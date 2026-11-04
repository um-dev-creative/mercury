package com.umdc.mercury.jpa.sql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

/// One saved content of a template (`mercury.template_versions`). `version` counts from 1 per template.
@Entity
@Table(name = "template_versions", schema = "mercury")
public class TemplateVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ColumnDefault("general.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @NotNull
    @Column(name = "version", nullable = false)
    private Integer version;

    @NotNull
    @Column(name = "etag", nullable = false, length = 128)
    private String etag;

    @NotNull
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @NotNull
    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @NotNull
    @Column(name = "reason", nullable = false, length = 20)
    private String reason;

    @Column(name = "created_by")
    private UUID createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Integer sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
