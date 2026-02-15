package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageStatusTypeTO(
        UUID id,
        String name,
        String description,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean active
) {

        @Override
        public String toString() {
                return "MessageStatusTypeTO{" +
                        "id=" + id +
                        ", name='" + name + '\'' +
                        ", description='" + description + '\'' +
                        ", createdAt=" + createdAt +
                        ", updatedAt=" + updatedAt +
                        ", active=" + active +
                        '}';
        }
}
