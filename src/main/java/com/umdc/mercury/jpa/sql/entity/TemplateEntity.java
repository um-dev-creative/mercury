package com.umdc.mercury.jpa.sql.entity;

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
    @ColumnDefault("general.uuid_generate_v4()")
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

    /** The language of this version of the template ({@code es}, {@code en}, {@code pt-BR}); callers name it, so it is a column and not parsed from {@link #location} (D-15). */
    @Size(max = 16)
    @Column(name = "language", length = 16)
    private String language;

    /** The e-mail subject of this version of the template, edited from Iris; may hold FreeMarker placeholders for the message parameters. {@code null}: none defined. */
    @Size(max = 500)
    @Column(name = "subject", length = 500)
    private String subject;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "template_type_id", nullable = false)
    private TemplateTypeEntity templateTypeEntity;

    /** The application (backbone-rest's): only its id is kept, Mercury never reads the general schema. */
    @NotNull
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

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
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
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

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
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


    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    /** A template saved without a language (the template management API, Iris) gets the one its file name ends with, so it stays reachable by name. */
    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    void deriveLanguage() {
        if (language == null || language.isBlank()) {
            language = com.umdc.mercury.ingress.TemplateIdentity.languageOf(location).orElse(null);
        }
    }
}
