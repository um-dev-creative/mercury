package com.prx.mercury.api.v1.to;

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
