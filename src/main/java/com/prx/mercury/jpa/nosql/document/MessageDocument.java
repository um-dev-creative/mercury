package com.prx.mercury.jpa.nosql.document;

import com.prx.mercury.constant.ChannelType;
import com.prx.mercury.constant.DeliveryStatusType;
import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "messages")
public abstract class MessageDocument {
    @Id
    private String id;
    private UUID userId;
    private UUID templateDefinedId;
    private ChannelType channelType;
    private DeliveryStatusType deliveryStatus;
    private LocalDateTime sendDate;
    private Map<String, Object> params;
    private UUID campaignId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTemplateDefinedId() {
        return templateDefinedId;
    }

    public void setTemplateDefinedId(UUID templateDefinedId) {
        this.templateDefinedId = templateDefinedId;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public DeliveryStatusType getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatusType deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public LocalDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(LocalDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }
}
