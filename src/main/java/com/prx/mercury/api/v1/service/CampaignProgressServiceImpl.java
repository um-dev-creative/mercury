package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.CampaignMetricsEntity;
import com.prx.mercury.jpa.sql.repository.CampaignMetricsRepository;
import com.prx.mercury.jpa.sql.repository.CampaignRepository;
import com.prx.mercury.mapper.CampaignMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CampaignProgressServiceImpl implements CampaignProgressService {

    private final CampaignRepository campaignRepository;
    private final CampaignMetricsRepository metricsRepository;
    private final CampaignMapper campaignMapper;

    public CampaignProgressServiceImpl(CampaignRepository campaignRepository,
                                       CampaignMetricsRepository metricsRepository,
                                       CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.metricsRepository = metricsRepository;
        this.campaignMapper = campaignMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignProgressTO getProgress(UUID campaignId) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        CampaignMetricsEntity metrics = metricsRepository.findByCampaign_Id(campaignId)
                .orElseGet(() -> {
                    CampaignMetricsEntity newMetrics = new CampaignMetricsEntity();
                    newMetrics.setCampaign(campaign);
                    return newMetrics;
                });

        int total = campaign.getTotalRecipients() != null ? campaign.getTotalRecipients() : 0;
        int sent = metrics.getTotalSent() != null ? metrics.getTotalSent() : 0;
        int failed = metrics.getFailed() != null ? metrics.getFailed() : 0;
        int pending = total - (sent + failed);

        return new CampaignProgressTO(
                campaign.getId(),
                campaign.getName(),
                campaignMapper.toChannelTypeTO(campaign.getChannelType()),
                campaign.getTotalRecipients(),
                metrics.getTotalSent(),
                metrics.getDelivered(),
                metrics.getFailed(),
                pending,
                metrics.getOpened(),
                metrics.getClicked(),
                0.0,
                0.0,
                campaign.getCreatedAt(),
                metrics.getLastUpdated(),
                campaign.getStatus()
        );
    }
}

