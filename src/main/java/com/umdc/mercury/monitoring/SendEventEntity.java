package com.umdc.mercury.monitoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One delivery outcome (SENT or FAILED) of one message, kept for the monitoring views. The recipient is stored masked and nothing of the
 * body or of a verification code is kept.
 */
@Entity
@Table(name = "send_events", schema = "mercury")
public class SendEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Backbone's application id, resolved from the template definition; {@code null} when it could not be resolved. */
    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "template_defined_id")
    private UUID templateDefinedId;

    @Column(name = "recipient_masked", length = 120)
    private String recipientMasked;

    @Column(name = "error_cause", length = 60)
    private String errorCause;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public UUID getTemplateDefinedId() {
        return templateDefinedId;
    }

    public void setTemplateDefinedId(UUID templateDefinedId) {
        this.templateDefinedId = templateDefinedId;
    }

    public String getRecipientMasked() {
        return recipientMasked;
    }

    public void setRecipientMasked(String recipientMasked) {
        this.recipientMasked = recipientMasked;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }
}
