package com.prx.mercury.api.v1.to;

import com.prx.mercury.constant.ChannelType;

import java.util.Map;

/**
 * Represents the progress of a campaign, providing metrics for overall performance and channel-specific statistics.
 *
 * This record encapsulates information about the total number of events and their distribution across
 * different statuses such as sent, delivered, failed, and pending for a given campaign.
 * It also provides detailed metrics for each communication channel involved in the campaign.
 *
 * Fields:
 * - total: The total number of campaign actions or messages.
 * - sent: The number of messages sent successfully.
 * - delivered: The number of messages delivered to recipients.
 * - failed: The number of messages that failed to be sent or delivered.
 * - pending: The number of messages still pending to be sent or processed.
 * - byChannel: A map containing metrics for each communication channel.
 *
 * Behavior:
 * - The `byChannel` field associates a `ChannelType` enum with its corresponding `ChannelMetrics`,
 *   enabling detailed tracking and analysis for each communication channel.
 */
public record CampaignProgress(
        int total,
        int sent,
        int delivered,
        int failed,
        int pending,
        Map<ChannelType, ChannelMetrics> byChannel
) {}
