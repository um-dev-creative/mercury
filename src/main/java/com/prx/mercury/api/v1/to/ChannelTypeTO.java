package com.prx.mercury.api.v1.to;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChannelTypeTO(
        UUID id,
        String code,
        String name,
        String description,
        String icon,
        Boolean enabled,
        String implementationClass,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean active
        ) {
}
