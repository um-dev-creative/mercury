package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a communication channel type with associated metadata and configuration.
 *
 * Fields:
 * - id: The unique identifier for the channel type.
 * - code: A short code representing the channel type (e.g., "EMAIL", "SMS").
 * - name: The name of the channel type.
 * - description: A detailed description of the channel type.
 * - icon: The path or reference to an icon representing the channel type.
 * - enabled: Indicates whether the channel type is enabled for use.
 * - implementationClass: The fully qualified class name implementing the behavior for the channel type.
 * - createdAt: The timestamp indicating when the channel type was created.
 * - updatedAt: The timestamp indicating when the channel type was last updated.
 * - active: Indicates whether the channel type is currently active.
 *
 * Immutability:
 * - This record is immutable, and all fields are initialized during instantiation.
 */
public record ChannelTypeTO(
        UUID id,
        String code,
        String name,
        String description,
        String icon,
        Boolean enabled,
        String implementationClass,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime updatedAt,
        Boolean active
        ) {
}
