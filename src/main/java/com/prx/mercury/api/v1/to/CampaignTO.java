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
 * Represents a campaign with specific attributes required for execution and tracking.
 * This record is immutable and encapsulates information about the campaign's metadata,
 * recipients, template details, and scheduling.
 *
 * Fields:
 * - name: The name of the campaign. Cannot be null, empty, or blank.
 * - channelTypeCode: The channel through which the campaign will be executed (e.g., email, sms, telegram).
 * - templateId: The unique identifier of the template used for the campaign.
 * - userId: The unique identifier of the user who created or owns the campaign.
 * - recipients: A list of recipients for the campaign. Each recipient includes an identifier and optional custom parameters.
 * - templateParams: A map of parameters to be injected into the template. Defaults to an empty map if not provided.
 * - scheduledAt: The timestamp indicating when the campaign is scheduled to be executed. Optional.
 * - status: The current status of the campaign (e.g., 'DRAFT', 'SENT'). Defaults to 'DRAFT' if not specified.
 *
 * Constraints and Validations:
 * - The name must not be null, empty, or blank.
 * - The channelTypeCode must not be null or blank.
 * - The templateId and userId must not be null.
 * - The recipients list must not be null or empty.
 * - The status must not be null, empty, or blank.
 *
 * Behavior:
 * - If templateParams is null, it is initialized as an empty map.
 * - If the status is null or blank, it defaults to "DRAFT".
 * - Allows checking if the campaign is scheduled based on the current time and the scheduledAt value.
 */
public record CampaignTO(
        @NotNull @NotBlank @NotEmpty
        String name,
        @NotNull @NotBlank
        String channelTypeCode, // 'email', 'sms', 'telegram', etc.
        @NotNull
        UUID templateId,
        @NotNull
        UUID userId,
        @NotNull @NotEmpty
        List<RecipientTO> recipients,
        Map<String, Object> templateParams,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime scheduledAt,
        @NotNull @NotBlank @NotEmpty
        String status,
        UUID applicationId
) {

    private static final String DEFAULT_STATUS = "DRAFT";

    public CampaignTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }

        if (templateParams == null) {
            templateParams = Map.of();
        }

        if (status == null || status.isBlank()) {
            status = DEFAULT_STATUS;
        }
    }

    public boolean isScheduled() {
        return scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
    }
}
