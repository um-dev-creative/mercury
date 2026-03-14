package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.exception.CampaignNotFoundException;
import com.prx.mercury.api.v1.exception.ForbiddenException;
import com.prx.mercury.api.v1.to.*;
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
import java.util.*;
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
        logger.info("Creating campaign. name={}, channelTypeCode={}, templateId={}, userId={}",
                campaignTO.name(), campaignTO.channelTypeCode(), campaignTO.templateId(), campaignTO.userId());

        validateRecipients(campaignTO);

        // 1. Validate channel exists and is enabled
        var channelType = channelTypeRepository.findByCode(campaignTO.channelTypeCode())
                .orElseThrow(() -> {
                    logger.warn("Channel type not found. channelTypeCode={}", campaignTO.channelTypeCode());
                    return new IllegalArgumentException("Channel type not found: " + campaignTO.channelTypeCode());
                });

        logger.debug("Resolved channel type. code={}, enabled={}", channelType.getCode(), channelType.getEnabled());

        if (Objects.isNull(channelType.getEnabled()) || Boolean.FALSE.equals(channelType.getEnabled())) {
            logger.warn("Channel type is disabled. code={}, name={}", channelType.getCode(), channelType.getName());
            throw new IllegalStateException("Channel type is disabled: " + channelType.getName());
        }

        var templateDefined = templateDefinedRepository.findById(campaignTO.templateId())
                .orElseThrow(() -> {
                    logger.warn("Template not found. templateId={}", campaignTO.templateId());
                    return new IllegalArgumentException("Template not found: " + campaignTO.templateId());
                });

        logger.debug("Resolved template. templateId={}", templateDefined.getId());

        // 2. Persist campaign
        LocalDateTime now = LocalDateTime.now();
        CampaignEntity campaign = campaignMapper.toCampaignEntity(
                campaignTO, channelType, templateDefined,
                DEFAULT_BATCH_SIZE, resolveStatus(campaignTO.status()), now,
                campaignTO.recipients().size());
        campaign = campaignRepository.save(campaign);

        logger.info("Saved campaign. id={}, name={}", campaign.getId(), campaign.getName());

        // 3. Create initial metrics record
        CampaignMetricsEntity metrics = new CampaignMetricsEntity();
        metrics.setCampaign(campaign);
        metricsRepository.save(metrics);

        // 4. Build and publish per-recipient Kafka messages
        String topic = messageFactory.topicFor(channelType.getCode());
        UUID campaignId = campaign.getId();

        logger.debug("Publishing campaign messages. campaignId={}, topic={}, recipients={}",
                campaignId, topic, campaignTO.recipients().size());

        for (RecipientTO recipient : campaignTO.recipients()) {
            Object message = messageFactory.createMessage(
                    channelType.getCode(),
                    campaignId,
                    recipient,
                    campaignTO.templateParams(),
                    campaignTO.templateId());
            kafkaTemplate.send(topic, message);
        }

        logger.info("Campaign created successfully. campaignId={}, status={}", campaignId, campaign.getStatus());
        return CompletableFuture.completedFuture(campaignProgressService.getProgress(campaignId));
    }

    @Override
    public CampaignProgressTO getProgress(UUID campaignId) {
        logger.debug("Fetching campaign progress. campaignId={}", campaignId);
        return campaignProgressService.getProgress(campaignId);
    }

    @Override
    public CampaignDetailResponse getById(UUID id) {
        logger.debug("Fetching campaign by id. id={}", id);
        CampaignEntity entity = campaignRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Campaign not found. id={}", id);
                    return new CampaignNotFoundException("Campaign not found: " + id);
                });
        logger.debug("Campaign found. id={}, name={}, status={}", entity.getId(), entity.getName(), entity.getStatus());
        return campaignMapper.toCampaignDetailResponse(entity);
    }

    @Override
    public CampaignDetailResponse updateCampaign(UUID campaignId, UpdateCampaignRequest updateRequest, UUID requesterId) {
        logger.info("Updating campaign. campaignId={}, requesterId={}", campaignId, requesterId);

        CampaignEntity entity = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignId));

        // simple permission check: only owner can update (createdBy) or same application admin - placeholder
        if (Objects.nonNull(entity.getCreatedBy()) && !entity.getCreatedBy().equals(requesterId)) {
            // In real app, more sophisticated role checks would be applied
            logger.warn("Requester {} is not owner of campaign {}", requesterId, campaignId);
            throw new ForbiddenException("Caller lacks permission to update this campaign");
        }

        boolean changed = false;

        if (Objects.nonNull(updateRequest.name()) && !updateRequest.name().isBlank()) {
            entity.setName(updateRequest.name());
            changed = true;
        }

        if (Objects.nonNull(updateRequest.templateId())) {
            var template = templateDefinedRepository.findById(updateRequest.templateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + updateRequest.templateId()));
            entity.setTemplateDefined(template);
            changed = true;
        }

        if (Objects.nonNull(updateRequest.applicationId())) {
            entity.setApplicationId(updateRequest.applicationId());
            changed = true;
        }

        if (Objects.nonNull(updateRequest.scheduledAt())) {
            entity.setScheduledAt(updateRequest.scheduledAt());
            changed = true;
        }

        if (Objects.nonNull(updateRequest.status()) && !updateRequest.status().isBlank()) {
            entity.setStatus(updateRequest.status());
            changed = true;
        }

        if (Objects.nonNull(updateRequest.templateParams())) {
            Map<String, Object> metadata = entity.getMetadata();
            if (metadata == null) {
                metadata = new LinkedHashMap<>();
            }
            // Store template parameters under a simple key to avoid complex nested types
            metadata.put("templateParams", updateRequest.templateParams());
            entity.setMetadata(metadata);
            changed = true;
        }

        if (Objects.nonNull(updateRequest.recipients())) {
            // deduplicate recipients by identifier
            List<RecipientTO> recipients = updateRequest.recipients();
            Set<String> seen = new LinkedHashSet<>();
            List<RecipientTO> deduped = new ArrayList<>();
            for (RecipientTO r : recipients) {
                if (r == null || r.identifier() == null || r.identifier().isBlank()) continue;
                if (seen.add(r.identifier())) {
                    deduped.add(r);
                }
            }
            if (deduped.isEmpty()) {
                throw new IllegalArgumentException("At least one recipient is required");
            }
            entity.setTotalRecipients(deduped.size());
            changed = true;
        }

        if (changed) {
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setUpdatedBy(requesterId);
            entity = campaignRepository.save(entity);
            logger.info("Campaign updated. id={}, updatedBy={}", entity.getId(), requesterId);
        } else {
            logger.info("No mutable fields provided for update. campaignId={}", campaignId);
        }

        return campaignMapper.toCampaignDetailResponse(entity);
    }

    @Override
    public List<CampaignDetailResponse> getByUserIdAndApplicationId(UUID userId, UUID applicationId) {
        logger.debug("Fetching campaigns by user and application. userId={}, applicationId={}", userId, applicationId);
        List<CampaignEntity> entities = campaignRepository.findByCreatedByAndApplicationId(userId, applicationId);
        return entities.stream().map(campaignMapper::toCampaignDetailResponse).toList();
    }


    private void validateRecipients(CampaignTO campaignTO) {
        if (campaignTO.recipients().isEmpty()) {
            logger.warn("Campaign creation failed due to missing recipients. name={}, channelTypeCode={}",
                    campaignTO.name(), campaignTO.channelTypeCode());
            throw new IllegalArgumentException(
                    "At least one recipient is required to create a campaign");
        }
        logger.debug("Recipients validated. count={}", campaignTO.recipients().size());
    }

    private String resolveStatus(String status) {
        String resolvedStatus = Objects.isNull(status) || status.isBlank() ? DEFAULT_STATUS : status;
        logger.debug("Resolved campaign status. inputStatus={}, resolvedStatus={}", status, resolvedStatus);
        return resolvedStatus;
    }

}
