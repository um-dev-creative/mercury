package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request record for updating mutable campaign fields. Fields are optional and
 * only provided values will be applied.
 */
public record UpdateCampaignRequest(
        String name,
        UUID templateId,
        List<RecipientTO> recipients,
        Map<String, Object> templateParams,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime scheduledAt,
        String status,
        UUID applicationId
) {
    // No-arg compact constructor required; validation performed at controller/service level

}

