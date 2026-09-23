package com.prx.mercury.api.v1.to;

import com.umdc.mercury.api.v1.to.CreateCampaignRequest;
import com.umdc.mercury.api.v1.to.RecipientTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCampaignRequestTest {

    private List<RecipientTO> validRecipients() {
        return List.of(new RecipientTO("recipient@example.com", "Recipient", Map.of()));
    }

    @Test
    @DisplayName("Create CreateCampaignRequest with valid data")
    void createCreateCampaignRequestWithValidData() {
        var templateId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        var scheduledAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
        var recipients = validRecipients();
        var templateParams = Map.<String, Object>of("key", "value");

        CreateCampaignRequest request = new CreateCampaignRequest(
                "Campaign Name",
                "email",
                templateId,
                userId,
                recipients,
                templateParams,
                scheduledAt,
                "PENDING",
                applicationId
        );

        assertEquals("Campaign Name", request.name());
        assertEquals("email", request.channelTypeCode());
        assertEquals(templateId, request.templateId());
        assertEquals(userId, request.userId());
        assertEquals(recipients, request.recipients());
        assertEquals(templateParams, request.templateParams());
        assertEquals(scheduledAt, request.scheduledAt());
        assertEquals("PENDING", request.status());
        assertEquals(applicationId, request.applicationId());
    }

    @Test
    @DisplayName("Create CreateCampaignRequest with null templateParams defaults to empty map")
    void createCreateCampaignRequestWithNullTemplateParams() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Campaign Name",
                "sms",
                UUID.randomUUID(),
                UUID.randomUUID(),
                validRecipients(),
                null,
                null,
                "PENDING",
                UUID.randomUUID()
        );

        assertNotNull(request.templateParams());
        assertTrue(request.templateParams().isEmpty());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when name is null")
    void createCreateCampaignRequestWithNullName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new CreateCampaignRequest(
                        null,
                        "email",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        validRecipients(),
                        null,
                        null,
                        "PENDING",
                        UUID.randomUUID()
                ));

        assertEquals("Campaign name is required", exception.getMessage());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when name is blank")
    void createCreateCampaignRequestWithBlankName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new CreateCampaignRequest(
                        "   ",
                        "email",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        validRecipients(),
                        null,
                        null,
                        "PENDING",
                        UUID.randomUUID()
                ));

        assertEquals("Campaign name is required", exception.getMessage());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when recipients is null")
    void createCreateCampaignRequestWithNullRecipients() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new CreateCampaignRequest(
                        "Campaign Name",
                        "email",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        "PENDING",
                        UUID.randomUUID()
                ));

        assertEquals("At least one recipient is required", exception.getMessage());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when recipients is empty")
    void createCreateCampaignRequestWithEmptyRecipients() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new CreateCampaignRequest(
                        "Campaign Name",
                        "email",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        List.of(),
                        null,
                        null,
                        "PENDING",
                        UUID.randomUUID()
                ));

        assertEquals("At least one recipient is required", exception.getMessage());
    }

    @Test
    @DisplayName("isScheduled returns true when scheduledAt is in the future")
    void isScheduledReturnsTrueWhenScheduledAtIsInTheFuture() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Campaign Name",
                "email",
                UUID.randomUUID(),
                UUID.randomUUID(),
                validRecipients(),
                null,
                LocalDateTime.now(ZoneOffset.UTC).plusHours(1),
                "PENDING",
                UUID.randomUUID()
        );

        assertTrue(request.isScheduled());
    }

    @Test
    @DisplayName("isScheduled returns false when scheduledAt is in the past")
    void isScheduledReturnsFalseWhenScheduledAtIsInThePast() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Campaign Name",
                "email",
                UUID.randomUUID(),
                UUID.randomUUID(),
                validRecipients(),
                null,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1),
                "PENDING",
                UUID.randomUUID()
        );

        assertFalse(request.isScheduled());
    }

    @Test
    @DisplayName("isScheduled returns false when scheduledAt is null")
    void isScheduledReturnsFalseWhenScheduledAtIsNull() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Campaign Name",
                "email",
                UUID.randomUUID(),
                UUID.randomUUID(),
                validRecipients(),
                null,
                null,
                "PENDING",
                UUID.randomUUID()
        );

        assertFalse(request.isScheduled());
    }

    @Test
    @DisplayName("CreateCampaignRequest equals and hashCode")
    void createCreateCampaignRequestEqualsAndHashCode() {
        var templateId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        var recipients = validRecipients();

        CreateCampaignRequest request1 = new CreateCampaignRequest(
                "Campaign Name",
                "email",
                templateId,
                userId,
                recipients,
                Map.of(),
                null,
                "PENDING",
                applicationId
        );
        CreateCampaignRequest request2 = new CreateCampaignRequest(
                "Campaign Name",
                "email",
                templateId,
                userId,
                recipients,
                Map.of(),
                null,
                "PENDING",
                applicationId
        );

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    @DisplayName("CreateCampaignRequest toString contains field values")
    void createCreateCampaignRequestToString() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Campaign Name",
                "email",
                UUID.randomUUID(),
                UUID.randomUUID(),
                validRecipients(),
                Map.of(),
                null,
                "PENDING",
                UUID.randomUUID()
        );

        assertTrue(request.toString().contains("Campaign Name"));
    }
}
