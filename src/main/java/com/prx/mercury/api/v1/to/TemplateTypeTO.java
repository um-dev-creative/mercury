package com.prx.mercury.api.v1.to;

import java.time.LocalDateTime;
import java.util.UUID;

public record TemplateTypeTO(UUID id,
                             String name,
                             String description,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt,
                             Boolean isActive) {

    @Override
    public String toString() {
        return "TemplateTypeTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                '}';
    }
}
