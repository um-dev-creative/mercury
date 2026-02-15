package com.prx.mercury.jpa.sql.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_record", schema = "mercury")
public class MessageRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ColumnDefault("mercury.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "sender", nullable = false, length = Integer.MAX_VALUE)
    private String sender;

    @NotNull
    @Column(name = "receptor", nullable = false, length = Integer.MAX_VALUE)
    private String to;

    @Column(name = "cc", length = Integer.MAX_VALUE)
    private String cc;

    @Size(max = 255)
    @Column(name = "subject")
    private String subject;

    @NotNull
    @Column(name = "content", nullable = false, length = Integer.MAX_VALUE)
    private String content;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_status_type_id", nullable = false)
    private MessageStatusTypeEntity messageStatusType;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_defined_id", nullable = false)
    private TemplateDefinedEntity templateDefined;

    /**
     * Default constructor.
     */
    public MessageRecordEntity() {
        // Default constructor
    }

    public TemplateDefinedEntity getTemplateDefined() {
        return templateDefined;
    }

    public void setTemplateDefined(TemplateDefinedEntity templateDefined) {
        this.templateDefined = templateDefined;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String emisor) {
        this.sender = emisor;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public MessageStatusTypeEntity getMessageStatusType() {
        return messageStatusType;
    }

    public void setMessageStatusType(MessageStatusTypeEntity messageStatusType) {
        this.messageStatusType = messageStatusType;
    }

}
