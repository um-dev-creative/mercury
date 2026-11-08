package com.umdc.mercury.jpa.sql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Reference to a backbone-rest application, enough to hold the foreign key of Mercury's own rows. Mercury never reads or writes
 * {@code general.application} (backbone-rest owns the {@code general} schema): there is no repository for it, and instances are
 * built from the id.
 */
@Entity
@Table(name = "application", schema = "general")
public class ApplicationEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Default constructor.
     */
    public ApplicationEntity() {
        // Default contructor
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
