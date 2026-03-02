package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignTOTest {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private List<RecipientTO> recipients() {
        return List.of(new RecipientTO("user@example.com", "User", null));
    }

    @Test
    @DisplayName("Create CampaignTO with valid data stores all fields correctly")
    void createWithValidData() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1);
        Map<String, Object> params = Map.of("subject", "Hello");

        CampaignTO to = new CampaignTO(
                "My Campaign", "email", TEMPLATE_ID, USER_ID,
                recipients(), params, scheduledAt, "DRAFT"
        );

        assertEquals("My Campaign", to.name());
        assertEquals("email", to.channelTypeCode());
        assertEquals(TEMPLATE_ID, to.templateId());
        assertEquals(USER_ID, to.userId());
        assertEquals(1, to.recipients().size());
        assertEquals(params, to.templateParams());
        assertEquals(scheduledAt, to.scheduledAt());
        assertEquals("DRAFT", to.status());
    }

    @Test
    @DisplayName("Create CampaignTO with null name throws IllegalArgumentException")
    void createWithNullNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new CampaignTO(null, "email", TEMPLATE_ID, USER_ID, recipients(), null, null, "DRAFT"));
    }

    @Test
    @DisplayName("Create CampaignTO with blank name throws IllegalArgumentException")
    void createWithBlankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new CampaignTO("  ", "email", TEMPLATE_ID, USER_ID, recipients(), null, null, "DRAFT"));
    }

    @Test
    @DisplayName("Create CampaignTO with null templateParams defaults to empty map")
    void createWithNullTemplateParamsDefaultsToEmptyMap() {
        CampaignTO to = new CampaignTO("Campaign", "email", TEMPLATE_ID, USER_ID, recipients(), null, null, "DRAFT");
        assertNotNull(to.templateParams());
        assertTrue(to.templateParams().isEmpty());
    }

    @Test
    @DisplayName("isScheduled returns true for future scheduledAt")
    void isScheduledReturnsTrueForFutureDate() {
        CampaignTO to = new CampaignTO("Campaign", "email", TEMPLATE_ID, USER_ID, recipients(),
                null, LocalDateTime.now().plusHours(1), "DRAFT");
        assertTrue(to.isScheduled());
    }

    @Test
    @DisplayName("isScheduled returns false for past scheduledAt")
    void isScheduledReturnsFalseForPastDate() {
        CampaignTO to = new CampaignTO("Campaign", "email", TEMPLATE_ID, USER_ID, recipients(),
                null, LocalDateTime.now().minusHours(1), "DRAFT");
        assertFalse(to.isScheduled());
    }

    @Test
    @DisplayName("isScheduled returns false when scheduledAt is null")
    void isScheduledReturnsFalseWhenNull() {
        CampaignTO to = new CampaignTO("Campaign", "email", TEMPLATE_ID, USER_ID, recipients(), null, null, "DRAFT");
        assertFalse(to.isScheduled());
    }

    @Test
    @DisplayName("Blank status defaults to DRAFT")
    void blankStatusDefaultsToDraft() {
        CampaignTO to = new CampaignTO("Campaign", "email", TEMPLATE_ID, USER_ID, recipients(), null, null, " ");
        assertEquals("DRAFT", to.status());
    }
}
