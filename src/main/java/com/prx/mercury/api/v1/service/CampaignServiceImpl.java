package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.CampaignTO;
import com.prx.mercury.api.v1.to.RecipientTO;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.CampaignMetricsEntity;
import com.prx.mercury.jpa.sql.repository.CampaignMetricsRepository;
import com.prx.mercury.jpa.sql.repository.CampaignRepository;
import com.prx.mercury.jpa.sql.repository.ChannelTypeRepository;
import com.prx.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.prx.mercury.mapper.CampaignMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CampaignServiceImpl implements CampaignService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignServiceImpl.class);
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String DEFAULT_STATUS = "DRAFT";

    private final CampaignRepository campaignRepository;
    private final CampaignMetricsRepository metricsRepository;
    private final ChannelTypeRepository channelTypeRepository;
    private final TemplateDefinedRepository templateDefinedRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CampaignMapper campaignMapper;
    private final CampaignProgressService campaignProgressService;
    private final CampaignMessageFactory messageFactory;

    public CampaignServiceImpl(
            CampaignRepository campaignRepository,
            CampaignMetricsRepository metricsRepository,
            ChannelTypeRepository channelTypeRepository,
            TemplateDefinedRepository templateDefinedRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            CampaignMapper campaignMapper,
            CampaignProgressService campaignProgressService,
            CampaignMessageFactory messageFactory) {
        this.campaignRepository = campaignRepository;
        this.metricsRepository = metricsRepository;
        this.channelTypeRepository = channelTypeRepository;
        this.templateDefinedRepository = templateDefinedRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.campaignMapper = campaignMapper;
        this.campaignProgressService = campaignProgressService;
        this.messageFactory = messageFactory;
    }

    @Async
    @Override
    public CompletableFuture<CampaignProgressTO> createCampaign(CampaignTO campaignTO) {
        validateRecipients(campaignTO);

        // 1. Validate channel exists and is enabled
        var channelType = channelTypeRepository.findByCode(campaignTO.channelTypeCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Channel type not found: " + campaignTO.channelTypeCode()));

        if (Objects.isNull(channelType.getEnabled()) || Boolean.FALSE.equals(channelType.getEnabled())) {
            throw new IllegalStateException("Channel type is disabled: " + channelType.getName());
        }

        var templateDefined = templateDefinedRepository.findById(campaignTO.templateId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Template not found: " + campaignTO.templateId()));

        // 2. Persist campaign
        LocalDateTime now = LocalDateTime.now();
        CampaignEntity campaign = campaignMapper.toCampaignEntity(
                campaignTO, channelType, templateDefined,
                DEFAULT_BATCH_SIZE, resolveStatus(campaignTO.status()), now,
                campaignTO.recipients().size());
        campaign = campaignRepository.save(campaign);

        // 3. Create initial metrics record
        CampaignMetricsEntity metrics = new CampaignMetricsEntity();
        metrics.setCampaign(campaign);
        metricsRepository.save(metrics);

        // 4. Build and publish per-recipient Kafka messages
        String topic = messageFactory.topicFor(channelType.getCode());
        UUID campaignId = campaign.getId();

        for (RecipientTO recipient : campaignTO.recipients()) {
            Object message = messageFactory.createMessage(
                    channelType.getCode(),
                    campaignId,
                    recipient,
                    campaignTO.templateParams(),
                    campaignTO.templateId());
            kafkaTemplate.send(topic, message);
        }

        return CompletableFuture.completedFuture(campaignProgressService.getProgress(campaignId));
    }

    @Override
    public CampaignProgressTO getProgress(UUID campaignId) {
        return campaignProgressService.getProgress(campaignId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validateRecipients(CampaignTO campaignTO) {
        if (campaignTO.recipients() == null || campaignTO.recipients().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one recipient is required to create a campaign");
        }
    }

    private String resolveStatus(String status) {
        return status == null || status.isBlank() ? DEFAULT_STATUS : status;
    }

}
