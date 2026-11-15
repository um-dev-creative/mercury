package com.umdc.mercury.mapper;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The campaign of an e-mail must survive Kafka -> MongoDB, or its counters never move. */
class EmailMessageMapperCampaignTest {

    private final EmailMessageMapper mapper = new EmailMessageMapperImpl();

    private EmailMessageTO message(UUID campaign) {
        return new EmailMessageTO(UUID.randomUUID(), UUID.randomUUID(), "f@e.com", List.of(), List.of(), "s", "b", LocalDateTime.now(), Map.of(), campaign);
    }

    @Test
    void theCampaignTravelsFromTheKafkaMessageToTheDocument() {
        UUID campaign = UUID.randomUUID();

        var document = mapper.toEmailMessageDocument(message(campaign));

        assertEquals(campaign, document.campaignId());
        assertEquals(DeliveryStatusType.OPENED, document.deliveryStatus());
    }

    @Test
    void aMessageWithoutCampaignKeepsNone() {
        assertNull(mapper.toEmailMessageDocument(message(null)).campaignId());
        assertNull(new EmailMessageTO(UUID.randomUUID(), UUID.randomUUID(), "f", List.of(), List.of(), "s", "b", null, Map.of()).campaignId());
    }

    @Test
    void theDocumentGoesBackToAKafkaMessageWithItsCampaign() {
        UUID campaign = UUID.randomUUID();

        assertEquals(campaign, mapper.toEmailMessageTO(mapper.toEmailMessageDocument(message(campaign))).campaignId());
    }
}
