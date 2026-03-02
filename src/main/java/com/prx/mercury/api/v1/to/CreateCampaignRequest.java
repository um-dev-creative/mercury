package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request record for creating a new campaign.
 *
 * @param name            The campaign name. Must not be null or blank.
 * @param channelTypeCode The channel type code (e.g. {@code email}, {@code sms}, {@code telegram}).
 *                        Must not be null or blank.
 * @param templateId      The UUID of the template to use. Must not be null.
 * @param userId          The UUID of the user creating the campaign. Must not be null.
 * @param recipients      The list of recipients for the campaign. Must not be null or empty.
 * @param templateParams  Optional key-value parameters used for template rendering.
 * @param scheduledAt     Optional date/time at which the campaign should be executed.
 *                        When {@code null} the campaign is executed immediately.
 */
public record CreateCampaignRequest(
        @NotNull @NotBlank @NotEmpty
        String name,
        @NotNull @NotBlank
        String channelTypeCode,
        @NotNull
        UUID templateId,
        @NotNull
        UUID userId,
        @NotNull @NotEmpty
        List<RecipientTO> recipients,
        Map<String, Object> templateParams,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime scheduledAt,
        @NotNull @NotEmpty
        String status,
        @NotNull
        UUID applicationId
) {

    /**
     * Compact constructor that applies default values.
     *
     * @throws IllegalArgumentException if {@code name} is null or blank.
     */
    public CreateCampaignRequest {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }
        if (templateParams == null) {
            templateParams = Map.of();
        }
    }

    /**
     * Returns {@code true} when the campaign is scheduled for a future date/time.
     *
     * @return {@code true} if {@code scheduledAt} is set and is in the future.
     */
    public boolean isScheduled() {
        return scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
    }
}
