package com.prx.mercury.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        LocalDateTime scheduledAt,
        @NotNull @NotBlank @NotEmpty
        String status
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
