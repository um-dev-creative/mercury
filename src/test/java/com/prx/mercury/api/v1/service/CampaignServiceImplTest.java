package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.exception.CampaignNotFoundException;
import com.prx.mercury.api.v1.exception.ForbiddenException;
import com.prx.mercury.api.v1.to.*;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

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

    /**
     * Stubs the template lookup and mapper for tests that exercise the full createCampaign path.
     */
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
            var campaign = makeCampaignTO();
            when(channelTypeRepository.findByCode("email")).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class,
                    () -> campaignServiceImpl.createCampaign(campaign));
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

            assertThat(result).isNotNull().hasSize(1);
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

            assertThat(result).isNotNull().isEmpty();
            verify(campaignRepository).findByCreatedByAndApplicationId(userId, appId);
        }
    }

    // ── UpdateCampaign ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCampaign tests")
    class UpdateCampaign {

        @Test
        @DisplayName("partial update changes mutable fields and persists")
        void partialUpdate_changesFields() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setName("OldName");
            entity.setCreatedBy(userId);
            entity.setTotalRecipients(1);
            CampaignDetailResponse campaign = new CampaignDetailResponse(
                    UUID.randomUUID(),
                    "NewName",
                    "email",
                    UUID.randomUUID(),
                    "SCHEDULED",
                    1,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now(), LocalDateTime.now(),
                    Map.of("k", "v")
            );

            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));
            when(campaignRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(campaignMapper.toCampaignDetailResponse(entity)).thenReturn(campaign);

            UpdateCampaignRequest req = new UpdateCampaignRequest("NewName", null, null, Map.of("k", "v"), LocalDateTime.now().plusDays(1), "SCHEDULED", null);

            var resp = campaignServiceImpl.updateCampaign(id, req, userId);

            assertThat(resp).isNotNull();
            verify(campaignRepository).save(any(CampaignEntity.class));
        }

        @Test
        @DisplayName("throws ForbiddenException when requester is not owner")
        void forbidden_whenNotOwner() {
            UUID id = UUID.randomUUID();
            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setCreatedBy(UUID.randomUUID());
            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));

            UpdateCampaignRequest req = new UpdateCampaignRequest(null, null, null, null, null, null, null);
            var otherId = UUID.randomUUID();

            assertThrows(ForbiddenException.class, () -> campaignServiceImpl.updateCampaign(id, req, otherId));
        }

        @Test
        @DisplayName("full update applies all mutable fields and persists")
        void fullUpdate_appliesAllFields() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID newTemplateId = UUID.randomUUID();

            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setName("OldName");
            entity.setCreatedBy(userId);
            entity.setTotalRecipients(1);

            TemplateDefinedEntity templateEntity = new TemplateDefinedEntity();
            templateEntity.setId(newTemplateId);

            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));
            when(templateDefinedRepository.findById(newTemplateId)).thenReturn(Optional.of(templateEntity));
            when(campaignRepository.save(any(CampaignEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            List<RecipientTO> recipients = List.of(
                    new RecipientTO("a@example.com", "A", Map.of()),
                    new RecipientTO("a@example.com", "A2", Map.of()),
                    new RecipientTO("b@example.com", "B", Map.of())
            );

            UpdateCampaignRequest req = new UpdateCampaignRequest(
                    "NewName",
                    newTemplateId,
                    recipients,
                    Map.of("p", "v"),
                    LocalDateTime.now().plusDays(2),
                    "SCHEDULED",
                    UUID.randomUUID()
            );

            CampaignDetailResponse expectedDto = new CampaignDetailResponse(id, "NewName", "email", newTemplateId, "SCHEDULED", 2, req.scheduledAt(), LocalDateTime.now(), LocalDateTime.now(), Map.of());
            when(campaignMapper.toCampaignDetailResponse(any(CampaignEntity.class))).thenReturn(expectedDto);

            CampaignDetailResponse result = campaignServiceImpl.updateCampaign(id, req, userId);

            assertThat(result).isSameAs(expectedDto);

            ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
            verify(campaignRepository).save(captor.capture());
            CampaignEntity saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("NewName");
            assertThat(saved.getTemplateDefined()).isSameAs(templateEntity);
            assertThat(saved.getTotalRecipients()).isEqualTo(2);
            assertThat(saved.getUpdatedBy()).isEqualTo(userId);
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws when template id does not exist")
        void templateNotFound_throws() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID missingTemplate = UUID.randomUUID();

            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setCreatedBy(userId);

            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));
            when(templateDefinedRepository.findById(missingTemplate)).thenReturn(Optional.empty());

            UpdateCampaignRequest req = new UpdateCampaignRequest(null, missingTemplate, null, null, null, null, null);

            assertThrows(IllegalArgumentException.class, () -> campaignServiceImpl.updateCampaign(id, req, userId));
        }

        @Test
        @DisplayName("throws when recipients dedupe results in empty list")
        void recipientsDedup_empty_throws() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setCreatedBy(userId);

            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));

            // recipients with only blank identifiers
            List<RecipientTO> badRecipients = List.of(new RecipientTO("", "X", Map.of()), new RecipientTO(null, "Y", Map.of()));

            UpdateCampaignRequest req = new UpdateCampaignRequest(null, null, badRecipients, null, null, null, null);

            assertThrows(IllegalArgumentException.class, () -> campaignServiceImpl.updateCampaign(id, req, userId));
        }

        @Test
        @DisplayName("no mutable fields provided does not persist and returns mapping")
        void noMutableFields_noSave_returnsMapped() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setCreatedBy(userId);

            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));

            CampaignDetailResponse mapped = new CampaignDetailResponse(id, "Old", "email", null, null, null, null, null, null, Map.of());
            when(campaignMapper.toCampaignDetailResponse(entity)).thenReturn(mapped);

            UpdateCampaignRequest req = new UpdateCampaignRequest(null, null, null, null, null, null, null);

            CampaignDetailResponse resp = campaignServiceImpl.updateCampaign(id, req, userId);

            assertThat(resp).isSameAs(mapped);
            verify(campaignRepository, never()).save(any(CampaignEntity.class));
        }

        @Test
        @DisplayName("allows update when createdBy is null (no owner)")
        void allowsUpdate_whenCreatedByNull() {
            UUID id = UUID.randomUUID();

            CampaignEntity entity = new CampaignEntity();
            entity.setId(id);
            entity.setCreatedBy(null);

            when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));
            when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateCampaignRequest req = new UpdateCampaignRequest("NameX", null, null, null, null, null, null);

            CampaignDetailResponse mapped = new CampaignDetailResponse(id, "NameX", "email", null, null, null, null, null, null, Map.of());
            when(campaignMapper.toCampaignDetailResponse(any(CampaignEntity.class))).thenReturn(mapped);

            CampaignDetailResponse out = campaignServiceImpl.updateCampaign(id, req, UUID.randomUUID());

            assertThat(out).isSameAs(mapped);
            verify(campaignRepository).save(any(CampaignEntity.class));
        }

    }

    @Nested
    @DisplayName("toggleCampaign tests")
    class ToggleCampaignTests {

        @Test
        @DisplayName("successfully disables a campaign when requester is owner")
        void toggle_disable_success() {
            UUID campaignId = UUID.randomUUID();
            CampaignEntity entity = new CampaignEntity();
            entity.setId(campaignId);
            UUID owner = UUID.randomUUID();
            entity.setCreatedBy(owner);
            entity.setTemplateDefined(new TemplateDefinedEntity());
            entity.setEnabled(true);

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity));
            when(campaignRepository.save(any(CampaignEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            campaignServiceImpl.toggleCampaign(campaignId, false, owner);

            assertThat(entity.getEnabled()).isFalse();
            verify(campaignRepository).save(entity);
        }

        @Test
        @DisplayName("throws CampaignNotFoundException when campaign missing")
        void toggle_notFound() {
            UUID unknown = UUID.randomUUID();
            var requestId = UUID.randomUUID();
            when(campaignRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThrows(CampaignNotFoundException.class, () -> campaignServiceImpl.toggleCampaign(unknown, false, requestId));
        }

        @Test
        @DisplayName("throws ForbiddenException when requester is not owner")
        void toggle_forbidden() {
            UUID campaignId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            CampaignEntity entity = new CampaignEntity();
            entity.setId(campaignId);
            entity.setCreatedBy(UUID.randomUUID());
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity));

            assertThrows(ForbiddenException.class, () -> campaignServiceImpl.toggleCampaign(campaignId, false, requestId));
        }

        @Test
        @DisplayName("throws IllegalStateException when enabling a campaign without template")
        void toggle_enable_missingTemplate() {
            UUID campaignId = UUID.randomUUID();
            CampaignEntity entity = new CampaignEntity();
            entity.setId(campaignId);
            UUID owner = UUID.randomUUID();
            entity.setCreatedBy(owner);
            entity.setTemplateDefined(null);
            entity.setEnabled(false);
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity));

            assertThrows(IllegalStateException.class, () -> campaignServiceImpl.toggleCampaign(campaignId, true, owner));
        }
    }
}
