package com.prx.mercury.api.v1.to;

import com.prx.mercury.constant.ChannelType;

import java.util.Map;

public record CampaignProgress(
        int total,
        int sent,
        int delivered,
        int failed,
        int pending,
        Map<ChannelType, ChannelMetrics> byChannel
) {}
