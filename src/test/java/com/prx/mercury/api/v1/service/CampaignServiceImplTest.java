package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.exception.CampaignNotFoundException;
import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.CampaignTO;
import com.prx.mercury.api.v1.to.RecipientTO;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.CampaignMetricsEntity;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import com.prx.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.prx.mercury.jpa.sql.repository.CampaignMetricsRepository;
import com.prx.mercury.jpa.sql.repository.CampaignRepository;
import com.prx.mercury.jpa.sql.repository.ChannelTypeRepository;
import com.prx.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.prx.mercury.kafka.to.PushNotificationMessageTO;
import com.prx.mercury.mapper.CampaignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignServiceImpl unit tests")
class CampaignServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignMetricsRepository metricsRepository;

    @Mock
    private ChannelTypeRepository channelTypeRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CampaignMapper campaignMapper;

    @Mock
    private CampaignProgressService campaignProgressService;

    @Mock
    private TemplateDefinedRepository templateDefinedRepository;

    @Mock
    private CampaignMessageFactory messageFactory;

    @InjectMocks
    private CampaignServiceImpl campaignServiceImpl;

    private ChannelTypeEntity defaultChannel;

    @BeforeEach
    void setUp() {
        defaultChannel = new ChannelTypeEntity();
        defaultChannel.setId(UUID.randomUUID());
        defaultChannel.setCode("email");
        defaultChannel.setName("Email");
        defaultChannel.setEnabled(true);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Stubs the template lookup and mapper for tests that exercise the full createCampaign path. */
    private void stubTemplateAndMapper() {
        TemplateDefinedEntity defaultTemplate = new TemplateDefinedEntity();
        defaultTemplate.setId(UUID.randomUUID());
        when(templateDefinedRepository.findById(any(UUID.class))).thenReturn(Optional.of(defaultTemplate));
        when(campaignMapper.toCampaignEntity(
                any(CampaignTO.class), any(ChannelTypeEntity.class), any(TemplateDefinedEntity.class),
                anyInt(), anyString(), any(LocalDateTime.class), anyInt()))
                .thenAnswer(invocation -> new CampaignEntity());
    }

    private CampaignTO makeCampaignTO() {
        List<RecipientTO> recipients = List.of(new RecipientTO("user@example.com", "User", Map.of()));
        return new CampaignTO("Test Campaign", "email", UUID.randomUUID(), UUID.randomUUID(),
                recipients, Map.of("from", "no-reply@x"), null, "DRAFT", UUID.randomUUID());
    }

    private void mockCampaignPersistence(UUID campaignId) {
        when(campaignRepository.save(any(CampaignEntity.class))).thenAnswer(invocation -> {
            CampaignEntity toSave = invocation.getArgument(0);
            toSave.setId(campaignId);
            return toSave;
        });
    }

    // ── CreateCampaign ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCampaign tests")
    class CreateCampaign {

        @Test
        @DisplayName("happy path returns progress and sends messages")
        void createCampaign_success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            when(channelTypeRepository.findByCode("email")).thenReturn(Optional.of(defaultChannel));
            stubTemplateAndMapper();
            mockCampaignPersistence(campaignId);
            when(metricsRepository.save(any(CampaignMetricsEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.topicFor("email")).thenReturn("mercury-email-messages");
            when(messageFactory.createMessage(anyString(), any(UUID.class), any(RecipientTO.class),
                    any(), any(UUID.class))).thenReturn(new Object());
            when(kafkaTemplate.send(anyString(), any(Object.class))).thenReturn(null);
            when(campaignProgressService.getProgress(campaignId)).thenReturn(
                    new CampaignProgressTO(campaignId, "Test Campaign", null, 1, 0, 0, 0, 0, 0, 0,
                            0.0, 0.0, LocalDateTime.now(), LocalDateTime.now(), "DRAFT"));

            CampaignTO request = makeCampaignTO();

            CompletableFuture<CampaignProgressTO> future = campaignServiceImpl.createCampaign(request);
            CampaignProgressTO result = future.get();

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("DRAFT");
            verify(kafkaTemplate).send(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("throws when channel not found")
        void createCampaign_channelNotFound() {
            when(channelTypeRepository.findByCode("email")).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class,
                    () -> campaignServiceImpl.createCampaign(makeCampaignTO()));
        }

        @Test
        @DisplayName("throws when channel disabled or enabled == null")
        void createCampaign_channelEnabledChecks() {
            CampaignTO request = makeCampaignTO();

            ChannelTypeEntity disabled = new ChannelTypeEntity();
            disabled.setCode("email");
            disabled.setEnabled(false);
            when(channelTypeRepository.findByCode("email")).thenReturn(Optional.of(disabled));
            assertThrows(IllegalStateException.class, () -> campaignServiceImpl.createCampaign(request));

            ChannelTypeEntity nullEnabled = new ChannelTypeEntity();
            nullEnabled.setCode("email");
            nullEnabled.setEnabled(null);
            when(channelTypeRepository.findByCode("email")).thenReturn(Optional.of(nullEnabled));
            assertThrows(IllegalStateException.class, () -> campaignServiceImpl.createCampaign(request));
        }

        @Test
        @DisplayName("publishes SMS messages to Kafka")
        void createCampaign_sms() throws Exception {
            ChannelTypeEntity smsChannel = new ChannelTypeEntity();
            smsChannel.setCode("sms");
            smsChannel.setEnabled(true);
            UUID campaignId = UUID.randomUUID();

            when(channelTypeRepository.findByCode("sms")).thenReturn(Optional.of(smsChannel));
            stubTemplateAndMapper();
            mockCampaignPersistence(campaignId);
            when(metricsRepository.save(any(CampaignMetricsEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.topicFor("sms")).thenReturn("mercury-sms-messages");
            when(messageFactory.createMessage(anyString(), any(UUID.class), any(RecipientTO.class),
                    any(), any(UUID.class))).thenReturn(new Object());
            when(kafkaTemplate.send(anyString(), any(Object.class))).thenReturn(null);
            when(campaignProgressService.getProgress(campaignId)).thenReturn(
                    new CampaignProgressTO(campaignId, "SMS Campaign", null, 1, 0, 0, 0, 0, 0, 0,
                            0.0, 0.0, LocalDateTime.now(), LocalDateTime.now(), "DRAFT"));

            CampaignTO smsTo = new CampaignTO("SMS Campaign", "sms", UUID.randomUUID(), UUID.randomUUID(),
                    List.of(new RecipientTO("+15551234567", "Name", Map.of())), Map.of(), null, "DRAFT", UUID.randomUUID());

            CampaignProgressTO result = campaignServiceImpl.createCampaign(smsTo).get();

            assertThat(result).isNotNull();
            verify(kafkaTemplate).send(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("publishes Telegram messages to Kafka")
        void createCampaign_telegram() throws Exception {
            ChannelTypeEntity tgChannel = new ChannelTypeEntity();
            tgChannel.setCode("telegram");
            tgChannel.setEnabled(true);
            UUID campaignId = UUID.randomUUID();

            when(channelTypeRepository.findByCode("telegram")).thenReturn(Optional.of(tgChannel));
            stubTemplateAndMapper();
            mockCampaignPersistence(campaignId);
            when(metricsRepository.save(any(CampaignMetricsEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.topicFor("telegram")).thenReturn("mercury-telegram-messages");
            when(messageFactory.createMessage(anyString(), any(UUID.class), any(RecipientTO.class),
                    any(), any(UUID.class))).thenReturn(new Object());
            when(kafkaTemplate.send(anyString(), any(Object.class))).thenReturn(null);
            when(campaignProgressService.getProgress(campaignId)).thenReturn(
                    new CampaignProgressTO(campaignId, "TG Campaign", null, 1, 0, 0, 0, 0, 0, 0,
                            0.0, 0.0, LocalDateTime.now(), LocalDateTime.now(), "DRAFT"));

            CampaignTO tgTo = new CampaignTO("TG Campaign", "telegram", UUID.randomUUID(), UUID.randomUUID(),
                    List.of(new RecipientTO("123456789", "TG User", Map.of())), Map.of(), null, "DRAFT", UUID.randomUUID());

            CampaignProgressTO result = campaignServiceImpl.createCampaign(tgTo).get();

            assertThat(result).isNotNull();
            verify(kafkaTemplate).send(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("publishes WhatsApp messages to Kafka")
        void createCampaign_whatsapp() throws Exception {
            ChannelTypeEntity waChannel = new ChannelTypeEntity();
            waChannel.setCode("whatsapp");
            waChannel.setEnabled(true);
            UUID campaignId = UUID.randomUUID();

            when(channelTypeRepository.findByCode("whatsapp")).thenReturn(Optional.of(waChannel));
            stubTemplateAndMapper();
            mockCampaignPersistence(campaignId);
            when(metricsRepository.save(any(CampaignMetricsEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.topicFor("whatsapp")).thenReturn("mercury-whatsapp-messages");
            when(messageFactory.createMessage(anyString(), any(UUID.class), any(RecipientTO.class),
                    any(), any(UUID.class))).thenReturn(new Object());
            when(kafkaTemplate.send(anyString(), any(Object.class))).thenReturn(null);
            when(campaignProgressService.getProgress(campaignId)).thenReturn(
                    new CampaignProgressTO(campaignId, "WA Campaign", null, 1, 0, 0, 0, 0, 0, 0,
                            0.0, 0.0, LocalDateTime.now(), LocalDateTime.now(), "DRAFT"));

            CampaignTO waTo = new CampaignTO("WA Campaign", "whatsapp", UUID.randomUUID(), UUID.randomUUID(),
                    List.of(new RecipientTO("+525512345678", "WA User", Map.of())),
                    Map.of(CampaignService.MESSAGE_KEY, "Hello WA"), null, "DRAFT", UUID.randomUUID());

            CampaignProgressTO result = campaignServiceImpl.createCampaign(waTo).get();

            assertThat(result).isNotNull();
            verify(kafkaTemplate).send(anyString(), any(Object.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("publishes Push messages to Kafka with correct topic and message type")
        void createCampaign_push() throws Exception {
            ChannelTypeEntity pushChannel = new ChannelTypeEntity();
            pushChannel.setCode("push");
            pushChannel.setEnabled(true);
            UUID campaignId = UUID.randomUUID();
            PushNotificationMessageTO pushMsg = new PushNotificationMessageTO(
                    UUID.randomUUID(), UUID.randomUUID(), "device-token-abc", "android",
                    "Hi", "Hello Push", null, null, null, null, null, null,
                    LocalDateTime.now(), Map.of(), campaignId);

            when(channelTypeRepository.findByCode("push")).thenReturn(Optional.of(pushChannel));
            stubTemplateAndMapper();
            mockCampaignPersistence(campaignId);
            when(metricsRepository.save(any(CampaignMetricsEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.topicFor("push")).thenReturn("mercury-push-messages");
            when(messageFactory.createMessage(anyString(), any(UUID.class), any(RecipientTO.class),
                    any(), any(UUID.class))).thenReturn(pushMsg);
            when(kafkaTemplate.send(anyString(), any(Object.class))).thenReturn(null);
            when(campaignProgressService.getProgress(campaignId)).thenReturn(
                    new CampaignProgressTO(campaignId, "Push Campaign", null, 1, 0, 0, 0, 0, 0, 0,
                            0.0, 0.0, LocalDateTime.now(), LocalDateTime.now(), "DRAFT"));

            CampaignTO pushTo = new CampaignTO("Push Campaign", "push", UUID.randomUUID(), UUID.randomUUID(),
                    List.of(new RecipientTO("device-token-abc", "Push User", Map.of())),
                    Map.of("title", "Hi", CampaignService.MESSAGE_KEY, "Hello Push", "platform", "android"),
                    null, "DRAFT", UUID.randomUUID());

            CampaignProgressTO result = campaignServiceImpl.createCampaign(pushTo).get();

            assertThat(result).isNotNull();

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), (Object) messageCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo("mercury-push-messages");
            assertThat(messageCaptor.getValue()).isInstanceOf(PushNotificationMessageTO.class);
        }
    }

    // ── GetProgress ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProgress tests")
    class GetProgress {

        @Test
        @DisplayName("returns the expected projection when metrics exist")
        void getProgress_success() {
            UUID id = UUID.randomUUID();
            CampaignProgressTO expected = new CampaignProgressTO(id, "Test", null, 10, 5, 4, 1, 0, 2, 1,
                    0.0, 0.0, LocalDateTime.now(), LocalDateTime.now(), "IN_PROGRESS");
            when(campaignProgressService.getProgress(id)).thenReturn(expected);

            CampaignProgressTO result = campaignServiceImpl.getProgress(id);

            assertAll("progress",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).isSameAs(expected));
        }

        @Test
        @DisplayName("throws when campaign does not exist")
        void getProgress_campaignNotFound() {
            UUID unknown = UUID.randomUUID();
            when(campaignProgressService.getProgress(unknown))
                    .thenThrow(new IllegalArgumentException("Campaign not found: " + unknown));

            assertThrows(IllegalArgumentException.class, () -> campaignServiceImpl.getProgress(unknown));
        }
    }

    // ── GetById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById tests")
    class GetById {

        @Test
        @DisplayName("returns mapped CampaignDetailResponse when campaign exists")
        void getById_success() {
            UUID id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setName("Summer Promo 2026");
            entity.setStatus("DRAFT");
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            ChannelTypeEntity channel = new ChannelTypeEntity();
            channel.setCode("email");
            entity.setChannelType(channel);
            TemplateDefinedEntity template = new TemplateDefinedEntity();
            template.setId(UUID.randomUUID());
            entity.setTemplateDefined(template);

            CampaignDetailResponse expected = new CampaignDetailResponse(
                    id, "Summer Promo 2026", "email", template.getId(),
                    "DRAFT", null, null, now, now, null);

            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));
            when(campaignMapper.toCampaignDetailResponse(entity)).thenReturn(expected);

            CampaignDetailResponse result = campaignServiceImpl.getById(id);

            assertAll("getById",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.id()).isEqualTo(id),
                    () -> assertThat(result.name()).isEqualTo("Summer Promo 2026"),
                    () -> assertThat(result.channelType()).isEqualTo("email"),
                    () -> assertThat(result.status()).isEqualTo("DRAFT")
            );
            verify(campaignRepository).findById(id);
            verify(campaignMapper).toCampaignDetailResponse(entity);
        }

        @Test
        @DisplayName("throws CampaignNotFoundException when campaign does not exist")
        void getById_notFound() {
            UUID unknown = UUID.randomUUID();
            when(campaignRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThrows(CampaignNotFoundException.class,
                    () -> campaignServiceImpl.getById(unknown));
            verify(campaignRepository).findById(unknown);
        }
    }

    // ── GetByUserAndApplication ─────────────────────────────────────────────

    @Nested
    @DisplayName("getByUserIdAndApplicationId tests")
    class GetByUserAndApplication {

        @Test
        @DisplayName("returns mapped list when repository finds campaigns")
        void getByUserAndApplication_success() {
            UUID userId = UUID.randomUUID();
            UUID appId = UUID.randomUUID();
            CampaignEntity e = new CampaignEntity();
            e.setId(UUID.randomUUID());
            e.setName("User Campaign");
            e.setStatus("DRAFT");
            ChannelTypeEntity channel = new ChannelTypeEntity();
            channel.setCode("email");
            e.setChannelType(channel);
            TemplateDefinedEntity template = new TemplateDefinedEntity();
            template.setId(UUID.randomUUID());
            e.setTemplateDefined(template);

            CampaignDetailResponse dto = new CampaignDetailResponse(
                    e.getId(), e.getName(), "email", template.getId(), e.getStatus(), null, null, null, null, null);

            when(campaignRepository.findByCreatedByAndApplicationId(userId, appId)).thenReturn(List.of(e));
            when(campaignMapper.toCampaignDetailResponse(e)).thenReturn(dto);

            List<CampaignDetailResponse> result = campaignServiceImpl.getByUserIdAndApplicationId(userId, appId);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(e.getId());
            verify(campaignRepository).findByCreatedByAndApplicationId(userId, appId);
            verify(campaignMapper).toCampaignDetailResponse(e);
        }

        @Test
        @DisplayName("returns empty list when repository returns none")
        void getByUserAndApplication_empty() {
            UUID userId = UUID.randomUUID();
            UUID appId = UUID.randomUUID();
            when(campaignRepository.findByCreatedByAndApplicationId(userId, appId)).thenReturn(List.of());

            List<CampaignDetailResponse> result = campaignServiceImpl.getByUserIdAndApplicationId(userId, appId);

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
            verify(campaignRepository).findByCreatedByAndApplicationId(userId, appId);
        }
    }
}
