package com.prx.mercury.jpa.sql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_metrics", schema = "mercury")
public class CampaignMetricsEntity {
    @Id
    @ColumnDefault("mercury.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

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
    private Instant lastUpdated;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
