package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.CampaignTO;
import com.prx.mercury.api.v1.to.RecipientTO;
import com.prx.mercury.jpa.nosql.document.MessageDocument;
import com.prx.mercury.jpa.nosql.repository.MessageNSRepository;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.CampaignMetricsEntity;
import com.prx.mercury.jpa.sql.repository.CampaignMetricsRepository;
import com.prx.mercury.jpa.sql.repository.CampaignRepository;
import com.prx.mercury.jpa.sql.repository.ChannelTypeRepository;
import com.prx.mercury.mapper.CampaignMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CampaignServiceImpl {

    private final CampaignRepository campaignRepository;
    private final CampaignMetricsRepository metricsRepository;
    private final ChannelTypeRepository channelTypeRepository;
    private final MessageNSRepository messageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CampaignMapper campaignMapper;

    public CampaignServiceImpl(
            CampaignRepository campaignRepository,
            CampaignMetricsRepository metricsRepository,
            ChannelTypeRepository channelTypeRepository,
            MessageNSRepository messageRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.metricsRepository = metricsRepository;
        this.channelTypeRepository = channelTypeRepository;
        this.messageRepository = messageRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.campaignMapper = campaignMapper;
    }

    @Async
    public CompletableFuture<CampaignProgressTO> createCampaign(CampaignTO request) {
        // 1. Validar que el canal existe y está habilitado
        var channelType = channelTypeRepository.findByCode(request.channelTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("Channel type not found: " + request.channelTypeCode()));

        if (!channelType.getEnabled()) {
            throw new IllegalStateException("Channel type is disabled: " + channelType.getName());
        }

        // 2. Crear campaña
        CampaignEntity campaign = new CampaignEntity();
        campaign.setName(request.name());
        campaign.setChannelType(channelType);
        campaign.setUserId(request.userId());
        campaign.setStatus("IN_PROGRESS");
        campaign.setTotalRecipients(request.recipients().size());
        campaign.setBatchSize(request.batchSize() != null ? request.batchSize() : 100);
        campaign.setCreatedAt(LocalDateTime.now());
        campaignRepository.save(campaign);

        // 3. Crear registro de métricas inicial
        CampaignMetricsEntity metrics = new CampaignMetricsEntity();
        metrics.setCampaign(campaign);
        metricsRepository.save(metrics);

        // 4. Procesar mensajes y publicar en Kafka
        String topic = getTopicForChannelCode(channelType.getCode());

        for (RecipientTO recipient : request.recipients()) {
            MessageDocument message = createMessageForChannel(
                    channelType.getCode(),
                    campaign.getId(),
                    recipient,
                    request.templateParams(),
                    request.templateId()
            );

            kafkaTemplate.send(topic, message);
        }

        return CompletableFuture.completedFuture(getProgress(campaign.getId()));
    }

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
                0.0, // se calcula en el constructor del record
                0.0,
                campaign.getCreatedAt(),
                metrics.getLastUpdated(),
                campaign.getStatus()
        );
    }

    private String getTopicForChannelCode(String channelCode) {
        return "mercury-" + channelCode + "-messages";
    }

    private MessageDocument createMessageForChannel(
            String channelCode,
            UUID campaignId,
            RecipientTO recipient,
            Map<String, Object> params,
            UUID templateId) {
        // Implementar factory pattern para crear mensajes según canal
        return null; // TODO: implementar
    }
}
