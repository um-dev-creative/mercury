package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.api.v1.to.CampaignTO;
import com.prx.mercury.api.v1.to.ChannelTypeTO;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import com.prx.mercury.jpa.sql.entity.TemplateDefinedEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(config = MapperAppConfig.class, uses = {ChannelTypeMapper.class})
public interface CampaignMapper {

    @Mapping(target = "channelTypeCode", source = "channelType.code")
    @Mapping(target = "templateId", ignore = true)
    CampaignTO toCampaignTO(CampaignEntity entity);

    /**
     * Maps a {@link CampaignEntity} to a {@link CampaignDetailResponse} for the
     * {@code GET /api/v1/campaigns/{id}} endpoint.
     *
     * @param entity the stored campaign entity; must not be {@code null}.
     * @return a fully populated detail response record.
     */
    @Mapping(target = "channelType", source = "channelType.code")
    @Mapping(target = "templateId", source = "templateDefined.id")
    CampaignDetailResponse toCampaignDetailResponse(CampaignEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "batchSize", source = "batchSize")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE)")
    @Mapping(target = "name", source = "campaignTO.name")
    @Mapping(target = "channelType", source = "channelType")
    @Mapping(target = "templateDefined", source = "templateDefined")
    @Mapping(target = "createdBy", source = "campaignTO.userId")
    @Mapping(target = "totalRecipients", source = "totalRecipients")
    @Mapping(target = "updatedBy", source = "campaignTO.userId")
    @Mapping(target = "scheduledAt", source = "campaignTO.scheduledAt")
    @Mapping(target = "applicationId", source = "campaignTO.applicationId")
    @Mapping(target = "metadata", expression = "java(buildMetadataDocument(campaignTO, channelType, batchSize, status, now, totalRecipients))")
    CampaignEntity toCampaignEntity(CampaignTO campaignTO, ChannelTypeEntity channelType, TemplateDefinedEntity templateDefined,
                                    int batchSize, String status, LocalDateTime now, int totalRecipients);

    // Added: delegate ChannelTypeEntity -> ChannelTypeTO mapping through ChannelTypeMapper
    ChannelTypeTO toChannelTypeTO(ChannelTypeEntity entity);

    default ChannelTypeEntity getChannelType(String code) {
        if (code == null) {
            return null;
        }
        ChannelTypeEntity entity = new ChannelTypeEntity();
        entity.setCode(code);
        return entity;
    }

    default Map<String, Object> buildMetadataDocument(CampaignTO campaignTO,
                                                      ChannelTypeEntity channelType,
                                                      int batchSize,
                                                      String status,
                                                      LocalDateTime createdAt,
                                                      int totalRecipients) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        Map<String, Object> channelSection = new LinkedHashMap<>();
        channelSection.put("code", channelType.getCode());
        channelSection.put("name", channelType.getName());
        channelSection.put("implementationClass", channelType.getImplementationClass());
        channelSection.put("enabled", channelType.getEnabled());
        metadata.put("channel", channelSection);

        Map<String, Object> templateSection = new LinkedHashMap<>();
        templateSection.put("id", campaignTO.templateId());
        templateSection.put("params", campaignTO.templateParams());
        metadata.put("template", templateSection);

        Map<String, Object> schedulingSection = new LinkedHashMap<>();
        schedulingSection.put("scheduled", campaignTO.isScheduled());
        schedulingSection.put("scheduledAt", campaignTO.scheduledAt());
        metadata.put("scheduling", schedulingSection);

        Map<String, Object> executionSection = new LinkedHashMap<>();
        executionSection.put("status", status);
        executionSection.put("totalRecipients", totalRecipients);
        executionSection.put("batchSize", batchSize);
        executionSection.put("createdAt", createdAt);
        metadata.put("execution", executionSection);

        Map<String, Object> auditSection = new LinkedHashMap<>();
        auditSection.put("requestedBy", campaignTO.userId());
        metadata.put("audit", auditSection);

        return metadata;
    }
}
