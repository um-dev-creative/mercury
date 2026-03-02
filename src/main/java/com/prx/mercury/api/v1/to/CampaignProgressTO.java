package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Transfer Object representing the progress and metrics of a campaign.
 */
public record CampaignProgressTO(
        UUID campaignId,
        String name,
        ChannelTypeTO channelType,
        Integer totalRecipients,
        Integer totalSent,
        Integer delivered,
        Integer failed,
        Integer pending,
        Integer opened,
        Integer clicked,
        double deliveryRate,
        double openRate,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime lastUpdated,
        String status
) {
    public CampaignProgressTO {
        if(Objects.isNull(totalSent)) {
            totalSent = 0;
        }
        int sent = totalSent ;
        int del = delivered != null ? delivered : 0;
        int op = opened != null ? opened : 0;

        deliveryRate = sent > 0
                ? Math.round((double) del / sent * 10000.0) / 100.0
                : 0.0;
        openRate = del > 0
                ? Math.round((double) op / del * 10000.0) / 100.0
                : 0.0;
    }
}
