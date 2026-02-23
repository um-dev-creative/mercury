package com.prx.mercury.jpa.sql.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_metrics", schema = "mercury")
public class CampaignMetricsEntity {
    @Id
    @ColumnDefault("mercury.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private CampaignEntity campaign;

    @ColumnDefault("0")
    @Column(name = "total_sent")
    private Integer totalSent;

    @ColumnDefault("0")
    @Column(name = "delivered")
    private Integer delivered;

    @ColumnDefault("0")
    @Column(name = "failed")
    private Integer failed;

    @ColumnDefault("0")
    @Column(name = "opened")
    private Integer opened;

    @ColumnDefault("0")
    @Column(name = "clicked")
    private Integer clicked;

    @ColumnDefault("0")
    @Column(name = "bounced")
    private Integer bounced;

    @ColumnDefault("0")
    @Column(name = "unsubscribed")
    private Integer unsubscribed;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CampaignEntity getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignEntity campaign) {
        this.campaign = campaign;
    }

    public Integer getTotalSent() {
        return totalSent;
    }

    public void setTotalSent(Integer totalSent) {
        this.totalSent = totalSent;
    }

    public Integer getDelivered() {
        return delivered;
    }

    public void setDelivered(Integer delivered) {
        this.delivered = delivered;
    }

    public Integer getFailed() {
        return failed;
    }

    public void setFailed(Integer failed) {
        this.failed = failed;
    }

    public Integer getOpened() {
        return opened;
    }

    public void setOpened(Integer opened) {
        this.opened = opened;
    }

    public Integer getClicked() {
        return clicked;
    }

    public void setClicked(Integer clicked) {
        this.clicked = clicked;
    }

    public Integer getBounced() {
        return bounced;
    }

    public void setBounced(Integer bounced) {
        this.bounced = bounced;
    }

    public Integer getUnsubscribed() {
        return unsubscribed;
    }

    public void setUnsubscribed(Integer unsubscribed) {
        this.unsubscribed = unsubscribed;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
