package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.ApplicationTemplateFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationTemplateFolderRepository extends JpaRepository<ApplicationTemplateFolderEntity, UUID> {

    /// The application that already uses a folder, if any (a folder belongs to one application).
    Optional<ApplicationTemplateFolderEntity> findByFolder(String folder);
}
