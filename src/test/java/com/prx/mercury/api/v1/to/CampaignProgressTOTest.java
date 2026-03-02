package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignProgressTOTest {

    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    private ChannelTypeTO channelTypeTO() {
        return new ChannelTypeTO(UUID.randomUUID(), "email", "Email", null, null, true, null, NOW, NOW, true);
    }

    @Test
    @DisplayName("Create CampaignProgressTO stores all fields correctly")
    void createWithValidData() {
        CampaignProgressTO to = new CampaignProgressTO(
                CAMPAIGN_ID, "My Campaign", channelTypeTO(),
                100, 80, 70, 5, 15, 30, 10,
                0.0, 0.0, NOW, NOW, "IN_PROGRESS"
        );

        assertEquals(CAMPAIGN_ID, to.campaignId());
        assertEquals("My Campaign", to.name());
        assertEquals(100, to.totalRecipients());
        assertEquals(80, to.totalSent());
        assertEquals(70, to.delivered());
        assertEquals(5, to.failed());
        assertEquals(15, to.pending());
        assertEquals(30, to.opened());
        assertEquals(10, to.clicked());
        assertEquals(NOW, to.createdAt());
        assertEquals(NOW, to.lastUpdated());
        assertEquals("IN_PROGRESS", to.status());
    }

    @Test
    @DisplayName("Delivery rate is correctly computed when totalSent > 0")
    void deliveryRateComputedCorrectly() {
        CampaignProgressTO to = new CampaignProgressTO(
                CAMPAIGN_ID, "Campaign", channelTypeTO(),
                100, 100, 80, 0, 0, 0, 0,
                0.0, 0.0, NOW, NOW, "DONE"
        );
        assertEquals(80.0, to.deliveryRate());
    }

    @Test
    @DisplayName("Delivery rate is zero when totalSent is zero")
    void deliveryRateIsZeroWhenSentIsZero() {
        CampaignProgressTO to = new CampaignProgressTO(
                CAMPAIGN_ID, "Campaign", channelTypeTO(),
                100, 0, 0, 0, 100, 0, 0,
                0.0, 0.0, NOW, NOW, "PENDING"
        );
        assertEquals(0.0, to.deliveryRate());
    }

    @Test
    @DisplayName("Open rate is correctly computed when delivered > 0")
    void openRateComputedCorrectly() {
        CampaignProgressTO to = new CampaignProgressTO(
                CAMPAIGN_ID, "Campaign", channelTypeTO(),
                100, 100, 100, 0, 0, 50, 0,
                0.0, 0.0, NOW, NOW, "DONE"
        );
        assertEquals(50.0, to.openRate());
    }

    @Test
    @DisplayName("Open rate is zero when delivered is zero")
    void openRateIsZeroWhenDeliveredIsZero() {
        CampaignProgressTO to = new CampaignProgressTO(
                CAMPAIGN_ID, "Campaign", channelTypeTO(),
                100, 0, 0, 0, 100, 0, 0,
                0.0, 0.0, NOW, NOW, "PENDING"
        );
        assertEquals(0.0, to.openRate());
    }

    @Test
    @DisplayName("Delivery rate is rounded to two decimal places")
    void deliveryRateRoundedToTwoDecimals() {
        // 1 delivered / 3 sent = 33.33%
        CampaignProgressTO to = new CampaignProgressTO(
                CAMPAIGN_ID, "Campaign", channelTypeTO(),
                3, 3, 1, 0, 0, 0, 0,
                0.0, 0.0, NOW, NOW, "DONE"
        );
        assertEquals(33.33, to.deliveryRate());
    }

    @Test
    @DisplayName("Null totalSent and delivered are treated as zero for rate calculations")
    void nullSentAndDeliveredTreatedAsZero() {
        CampaignProgressTO to = new CampaignProgressTO(
                CAMPAIGN_ID, "Campaign", channelTypeTO(),
                100, null, null, null, 100, null, null,
                0.0, 0.0, NOW, NOW, "PENDING"
        );
        assertEquals(0.0, to.deliveryRate());
        assertEquals(0.0, to.openRate());
    }
}
