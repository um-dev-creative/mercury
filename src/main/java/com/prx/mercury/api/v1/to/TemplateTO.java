package com.prx.mercury.api.v1.to;

import java.time.LocalDateTime;
import java.util.UUID;

public record TemplateTO(UUID id,
                         String description,
                         String location,
                         String fileFormat,
                         TemplateTypeTO templateType,
                         UUID application,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt,
                         Boolean isActive) {

    @Override
    public String toString() {
        return "TemplateTO{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", fileFormat='" + fileFormat + '\'' +
                ", templateType=" + templateType +
                ", application=" + application +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                '}';
    }
}
