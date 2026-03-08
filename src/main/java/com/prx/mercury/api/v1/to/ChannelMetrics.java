package com.prx.mercury.api.v1.to;

/**
 * Represents a summary of metrics related to a communication channel, including statistics
 * about sent messages, delivery outcomes, and user interactions.
 *
 * Fields:
 * - totalSent: The total number of messages sent through the channel.
 * - delivered: The total number of messages successfully delivered.
 * - failed: The total number of messages that failed to be delivered.
 * - opened: The total number of messages that were opened by recipients.
 * - clicked: The total number of messages where recipients clicked on links.
 * - deliveryRate: The percentage of sent messages that were successfully delivered.
 * - openRate: The percentage of delivered messages that were opened.
 * - clickRate: The percentage of opened messages where links were clicked.
 *
 * Behavior:
 * - The deliveryRate is calculated as (delivered / totalSent) * 100.0, rounded to two decimal places.
 * - The openRate is calculated as (opened / delivered) * 100.0, rounded to two decimal places.
 * - The clickRate is calculated as (clicked / opened) * 100.0, rounded to two decimal places.
 * - If the relevant denominator (e.g., totalSent, delivered, opened) is zero, the computed rate defaults to 0.0.
 *
 * Constraints:
 * - All metrics are stored as integers, except for the rates which are stored as doubles.
 *
 * Immutability:
 * - This record is immutable, and all calculations for rates are performed in the constructor.
 */
public record ChannelMetrics(
        int totalSent,
        int delivered,
        int failed,
        int opened,
        int clicked,
        double deliveryRate,
        double openRate,
        double clickRate
) {
    public ChannelMetrics {
        deliveryRate = totalSent > 0
                ? Math.round((double) delivered / totalSent * 10000.0) / 100.0
                : 0.0;
        openRate = delivered > 0
                ? Math.round((double) opened / delivered * 10000.0) / 100.0
                : 0.0;
        clickRate = opened > 0
                ? Math.round((double) clicked / opened * 10000.0) / 100.0
                : 0.0;
    }
}
