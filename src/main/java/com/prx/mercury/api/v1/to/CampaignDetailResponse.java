package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response record for a single campaign detail lookup.
 *
 * <p>Returned by {@code GET /api/v1/campaigns/{id}}.</p>
 *
 * @param id               Unique identifier of the campaign.
 * @param name             Human-readable campaign name.
 * @param channelType      Channel type code (e.g. {@code email}, {@code sms}).
 * @param templateId       UUID of the associated template definition.
 * @param status           Current lifecycle status (e.g. {@code DRAFT}, {@code IN_PROGRESS}).
 * @param totalRecipients  Total number of recipients enrolled in the campaign.
 * @param scheduledAt      Optional scheduled execution date/time; {@code null} for immediate campaigns.
 * @param createdAt        Timestamp when the campaign record was created.
 * @param updatedAt        Timestamp of the last update to the campaign record.
 * @param metadata         Optional free-form metadata stored with the campaign.
 */
public record CampaignDetailResponse(
        UUID id,
        String name,
        String channelType,
        UUID templateId,
        String status,
        Integer totalRecipients,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime scheduledAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime updatedAt,
        Map<String, Object> metadata
) {
}
