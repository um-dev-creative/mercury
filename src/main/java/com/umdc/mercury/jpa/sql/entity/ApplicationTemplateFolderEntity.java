package com.umdc.mercury.jpa.sql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/// The folder an application keeps its templates in (`mercury.application_template_folders`). `applicationId` is only an id: Mercury never reads the `general` schema.
@Entity
@Table(name = "application_template_folders", schema = "mercury")
public class ApplicationTemplateFolderEntity {

    @Id
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "folder", nullable = false, length = 64)
    private String folder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ApplicationTemplateFolderEntity() {
        // JPA
    }

    public ApplicationTemplateFolderEntity(UUID applicationId, String folder, LocalDateTime now) {
        this.applicationId = applicationId;
        this.folder = folder;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder, LocalDateTime now) {
        this.folder = folder;
        this.updatedAt = now;
    }
}
