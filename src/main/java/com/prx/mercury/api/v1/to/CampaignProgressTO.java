package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the progress of a campaign, providing detailed metrics and metadata.
 * This record encapsulates information about the current state of a campaign, including
 * recipient statistics, delivery performance, and status updates.
 *
 * The record calculates delivery and open rates based on the provided metrics.
 * - The delivery rate is the percentage of messages delivered versus total messages sent.
 * - The open rate is the percentage of messages opened versus those delivered.
 *
 * Fields:
 * - campaignId: The unique identifier of the campaign.
 * - name: The name of the campaign.
 * - channelType: The channel associated with the campaign (e.g., email, SMS, etc.).
 * - totalRecipients: The total number of recipients in the campaign.
 * - totalSent: The total number of messages sent in the campaign. Defaults to 0 if null.
 * - delivered: The total number of successfully delivered messages.
 * - failed: The total number of failed delivery attempts.
 * - pending: The total number of messages pending delivery.
 * - opened: The total number of messages opened by recipients.
 * - clicked: The total number of times links in the messages were clicked.
 * - deliveryRate: The calculated percentage of delivered messages out of total sent messages.
 * - openRate: The calculated percentage of opened messages out of delivered messages.
 * - createdAt: The timestamp indicating when the campaign progress object was created.
 * - lastUpdated: The timestamp indicating the last update to the campaign progress.
 * - status: The current status of the campaign's progress (e.g., 'IN_PROGRESS', 'COMPLETED').
 *
 * Behavior:
 * - If totalSent is null, it is initialized to 0.
 * - Delivery rate is computed as (delivered / totalSent) * 100, rounded to two decimal places.
 * - Open rate is computed as (opened / delivered) * 100, rounded to two decimal places.
 * - Fields like delivered, opened, and failed are treated as zero if null.
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
