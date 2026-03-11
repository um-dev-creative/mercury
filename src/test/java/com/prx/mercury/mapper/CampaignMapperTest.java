package com.prx.mercury.mapper;

import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import com.prx.mercury.jpa.sql.entity.TemplateDefinedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("CampaignMapper unit tests")
class CampaignMapperTest {

    private final CampaignMapper mapper = Mappers.getMapper(CampaignMapper.class);

    private UUID campaignId;
    private UUID templateId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 2, 20, 12, 34, 56);
    }

    private CampaignEntity buildEntity(boolean withMetadata) {
        ChannelTypeEntity channel = new ChannelTypeEntity();
        channel.setCode("email");
        channel.setName("Email");
        channel.setEnabled(true);

        TemplateDefinedEntity template = new TemplateDefinedEntity();
        template.setId(templateId);

        CampaignEntity entity = new CampaignEntity();
        entity.setId(campaignId);
        entity.setName("Summer Promo 2026");
        entity.setStatus("DRAFT");
        entity.setTotalRecipients(42);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now.plusDays(1));
        entity.setScheduledAt(now.plusDays(5));
        entity.setChannelType(channel);
        entity.setTemplateDefined(template);
        if (withMetadata) {
            entity.setMetadata(Map.of("ownerId", "1a24da99-78d0-4221-96fa-64aa79a47bd1"));
        }
        return entity;
    }

    // ── toCampaignDetailResponse ──────────────────────────────────────────────

    @Nested
    @DisplayName("toCampaignDetailResponse")
    class ToCampaignDetailResponse {

        @Test
        @DisplayName("maps all scalar fields from entity correctly")
        void mapsScalarFields() {
            CampaignDetailResponse result = mapper.toCampaignDetailResponse(buildEntity(false));

            assertAll("scalar field mapping",
                    () -> assertThat(result.id()).isEqualTo(campaignId),
                    () -> assertThat(result.name()).isEqualTo("Summer Promo 2026"),
                    () -> assertThat(result.status()).isEqualTo("DRAFT"),
                    () -> assertThat(result.totalRecipients()).isEqualTo(42),
                    () -> assertThat(result.createdAt()).isEqualTo(now),
                    () -> assertThat(result.updatedAt()).isEqualTo(now.plusDays(1)),
                    () -> assertThat(result.scheduledAt()).isEqualTo(now.plusDays(5))
            );
        }

        @Test
        @DisplayName("maps channelType code from nested ChannelTypeEntity")
        void mapsChannelTypeCode() {
            CampaignDetailResponse result = mapper.toCampaignDetailResponse(buildEntity(false));

            assertThat(result.channelType()).isEqualTo("email");
        }

        @Test
        @DisplayName("maps templateId from nested TemplateDefinedEntity")
        void mapsTemplateId() {
            CampaignDetailResponse result = mapper.toCampaignDetailResponse(buildEntity(false));

            assertThat(result.templateId()).isEqualTo(templateId);
        }

        @Test
        @DisplayName("maps metadata when present")
        void mapsMetadataWhenPresent() {
            CampaignDetailResponse result = mapper.toCampaignDetailResponse(buildEntity(true));

            assertThat(result.metadata())
                    .isNotNull()
                    .containsKey("ownerId");
        }

        @Test
        @DisplayName("metadata is null when entity has no metadata")
        void metadataIsNullWhenAbsent() {
            CampaignDetailResponse result = mapper.toCampaignDetailResponse(buildEntity(false));

            assertNull(result.metadata());
        }

        @Test
        @DisplayName("returns null when entity is null")
        void returnsNullForNullEntity() {
            CampaignDetailResponse result = mapper.toCampaignDetailResponse(null);

            assertNull(result);
        }
    }
}
