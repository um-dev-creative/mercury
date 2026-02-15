package com.prx.mercury.jpa.sql.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "templates", schema = "mercury")
public class TemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ColumnDefault("mercury.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "description", nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @Size(max = 500)
    @NotNull
    @Column(name = "location", nullable = false, length = 500)
    private String location;

    @Size(max = 50)
    @NotNull
    @Column(name = "file_format", nullable = false, length = 50)
    private String fileFormat;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "template_type_id", nullable = false)
    private TemplateTypeEntity templateTypeEntity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationEntity applicationEntity;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ColumnDefault("true")
    @Column(name = "active")
    private Boolean active;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "severity_type_id", nullable = false)
    private SeverityTypeEntity severityTypeEntity;

    /**
     * Default constructor.
     */
    public TemplateEntity() {
        // Default constructor
    }

    public SeverityTypeEntity getSeverityType() {
        return severityTypeEntity;
    }

    public void setSeverityType(SeverityTypeEntity severityTypeEntity) {
        this.severityTypeEntity = severityTypeEntity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public TemplateTypeEntity getTemplateType() {
        return templateTypeEntity;
    }

    public void setTemplateType(TemplateTypeEntity templateTypeEntity) {
        this.templateTypeEntity = templateTypeEntity;
    }

    public ApplicationEntity getApplication() {
        return applicationEntity;
    }

    public void setApplication(ApplicationEntity applicationEntity) {
        this.applicationEntity = applicationEntity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean isActive) {
        this.active = isActive;
    }

}
