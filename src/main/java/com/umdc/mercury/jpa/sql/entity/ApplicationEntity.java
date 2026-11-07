package com.umdc.mercury.jpa.sql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "application", schema = "general")
public class ApplicationEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Short code of the application ({@code iris}, {@code MRCY}...); read-only here, backbone-rest owns the table. */
    @Column(name = "code_name", insertable = false, updatable = false)
    private String codeName;

    /** Display name; read-only here, backbone-rest owns the table. */
    @Column(name = "name", insertable = false, updatable = false)
    private String name;

    /** Whether backbone-rest has the application enabled; read-only here. */
    @Column(name = "active", insertable = false, updatable = false)
    private boolean active;

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

    public String getCodeName() {
        return codeName;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

}
