package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record TemplateDefinedTO(
        UUID id,
        TemplateTO template,
        UUID userId,
        UUID applicationId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime expiredAt,
        Boolean active,
        UUID frequencyTypeId
) {

        @Override
        public String toString() {
                return "TemplateDefinedTO{" +
                        "id=" + id +
                        ", template=" + template +
                        ", userId=" + userId +
                        ", applicationId=" + applicationId +
                        ", createdAt=" + createdAt +
                        ", updatedAt=" + updatedAt +
                        ", expiredAt=" + expiredAt +
                        ", active=" + active +
                        ", frequencyTypeId=" + frequencyTypeId +
                        '}';
        }
}
