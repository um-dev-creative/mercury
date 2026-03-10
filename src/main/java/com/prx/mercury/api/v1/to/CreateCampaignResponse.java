package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response record returned after a campaign creation request.
 *
 * @param campaignId      The unique identifier assigned to the newly created campaign.
 * @param name            The name of the campaign.
 * @param status          The initial status of the campaign (e.g. {@code DRAFT}, {@code IN_PROGRESS}).
 * @param totalRecipients The total number of recipients registered for this campaign.
 * @param createdAt       The date/time at which the campaign was created.
 * @param scheduledAt     The date/time at which the campaign is scheduled to run,
 *                        or {@code null} if it is executed immediately.
 */
public record CreateCampaignResponse(
        UUID campaignId,
        String name,
        String status,
        Integer totalRecipients,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime scheduledAt
) {
}
