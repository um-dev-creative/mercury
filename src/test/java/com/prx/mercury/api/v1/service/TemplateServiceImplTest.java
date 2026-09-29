package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.TemplateNotFoundException;
import com.umdc.mercury.api.v1.service.ApplicationAuthorizationService;
import com.umdc.mercury.api.v1.service.TemplateSearchCriteria;
import com.umdc.mercury.api.v1.service.TemplateServiceImpl;
import com.umdc.mercury.api.v1.service.TemplateUpdateApplier;
import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateSearchResponse;
import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.entity.SeverityTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateTypeEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import com.umdc.mercury.jpa.sql.repository.SeverityTypeRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateTypeEntityRepository;
import com.umdc.mercury.mapper.TemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateServiceImpl unit tests")
class TemplateServiceImplTest {

    private static final String PERMISSION = "TEMPLATE_MANAGE";
    private static final String SESSION_TOKEN = "session-token-value";
    private static final String FORBIDDEN_MESSAGE = "Caller lacks permission to manage this application";

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateTypeEntityRepository templateTypeRepository;

    @Mock
    private SeverityTypeRepository severityTypeRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationAuthorizationService applicationAuthorizationService;

    @Mock
    private TemplateMapper templateMapper;

    @Mock
    private TemplateUpdateApplier templateUpdateApplier;

    private TemplateServiceImpl service;

    private UUID requesterId;
    private UUID applicationId;
    private UUID templateTypeId;
    private UUID severityTypeId;

    @BeforeEach
    void setUp() {
        service = new TemplateServiceImpl(templateRepository, templateTypeRepository, severityTypeRepository,
                applicationRepository, applicationAuthorizationService, templateMapper, templateUpdateApplier, PERMISSION);

        requesterId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        templateTypeId = UUID.randomUUID();
        severityTypeId = UUID.randomUUID();
    }

    /** applicationAuthorizationService.assertPermission is void — Mockito's default is a no-op (grant). */
    private void stubForbidden() {
        doThrow(new ForbiddenException(FORBIDDEN_MESSAGE))
                .when(applicationAuthorizationService).assertPermission(anyString(), any(), anyString());
    }

    private CreateTemplateRequest createRequest() {
        return new CreateTemplateRequest("desc", "loc", "HTML", templateTypeId, applicationId, severityTypeId);
    }

    private TemplateTypeEntity activeTemplateType() {
        TemplateTypeEntity type = new TemplateTypeEntity();
        type.setId(templateTypeId);
        type.setActive(true);
        return type;
    }

    private SeverityTypeEntity activeSeverityType() {
        SeverityTypeEntity severity = new SeverityTypeEntity();
        severity.setId(severityTypeId);
        severity.setActive(true);
        return severity;
    }

    private TemplateEntity activeEntity(UUID id) {
        TemplateEntity entity = new TemplateEntity();
        entity.setId(id);
        entity.setActive(true);
        entity.setDescription("desc");
        ApplicationEntity application = new ApplicationEntity();
        application.setId(applicationId);
        entity.setApplication(application);
        return entity;
    }

    // ── createTemplate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTemplate tests")
    class CreateTemplate {

        @Test
        @DisplayName("persists an active template and returns the mapped response")
        void createTemplate_success() throws Exception {
            ApplicationEntity application = new ApplicationEntity();
            application.setId(applicationId);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(templateTypeRepository.findById(templateTypeId)).thenReturn(Optional.of(activeTemplateType()));
            when(severityTypeRepository.findById(severityTypeId)).thenReturn(Optional.of(activeSeverityType()));

            UUID newId = UUID.randomUUID();
            when(templateMapper.toEntity(any(), any(), any(), any(), any())).thenAnswer(inv -> new TemplateEntity());
            when(templateRepository.save(any(TemplateEntity.class))).thenAnswer(inv -> {
                TemplateEntity e = inv.getArgument(0);
                e.setId(newId);
                return e;
            });
            TemplateDetailResponse expected = new TemplateDetailResponse(
                    newId, "desc", "loc", "HTML", templateTypeId, "Type", applicationId, severityTypeId, "High",
                    true, LocalDateTime.now(), LocalDateTime.now());
            when(templateMapper.toDetailResponse(any(TemplateEntity.class))).thenReturn(expected);

            CompletableFuture<TemplateDetailResponse> future = service.createTemplate(createRequest(), requesterId, SESSION_TOKEN);
            TemplateDetailResponse result = future.get();

            assertThat(result).isSameAs(expected);
            verify(applicationAuthorizationService).assertPermission(SESSION_TOKEN, applicationId, PERMISSION);
            verify(templateRepository).save(any(TemplateEntity.class));
        }

        @Test
        @DisplayName("throws ForbiddenException when caller lacks permission for the application")
        void createTemplate_forbidden() {
            stubForbidden();

            assertThrows(ForbiddenException.class, () -> service.createTemplate(createRequest(), requesterId, SESSION_TOKEN));
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when application does not exist")
        void createTemplate_applicationNotFound() {
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.createTemplate(createRequest(), requesterId, SESSION_TOKEN));
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when template type does not exist")
        void createTemplate_templateTypeNotFound() {
            ApplicationEntity application = new ApplicationEntity();
            application.setId(applicationId);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(templateTypeRepository.findById(templateTypeId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.createTemplate(createRequest(), requesterId, SESSION_TOKEN));
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when template type is inactive")
        void createTemplate_templateTypeInactive() {
            ApplicationEntity application = new ApplicationEntity();
            application.setId(applicationId);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            TemplateTypeEntity inactive = activeTemplateType();
            inactive.setActive(false);
            when(templateTypeRepository.findById(templateTypeId)).thenReturn(Optional.of(inactive));

            assertThrows(IllegalArgumentException.class, () -> service.createTemplate(createRequest(), requesterId, SESSION_TOKEN));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when severity type does not exist")
        void createTemplate_severityTypeNotFound() {
            ApplicationEntity application = new ApplicationEntity();
            application.setId(applicationId);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(templateTypeRepository.findById(templateTypeId)).thenReturn(Optional.of(activeTemplateType()));
            when(severityTypeRepository.findById(severityTypeId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.createTemplate(createRequest(), requesterId, SESSION_TOKEN));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when severity type is inactive")
        void createTemplate_severityTypeInactive() {
            ApplicationEntity application = new ApplicationEntity();
            application.setId(applicationId);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(templateTypeRepository.findById(templateTypeId)).thenReturn(Optional.of(activeTemplateType()));
            SeverityTypeEntity inactive = activeSeverityType();
            inactive.setActive(false);
            when(severityTypeRepository.findById(severityTypeId)).thenReturn(Optional.of(inactive));

            assertThrows(IllegalArgumentException.class, () -> service.createTemplate(createRequest(), requesterId, SESSION_TOKEN));
        }
    }

    // ── updateTemplate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTemplate tests")
    class UpdateTemplate {

        @Test
        @DisplayName("applies changes, bumps updatedAt and persists")
        void updateTemplate_success() {
            UUID id = UUID.randomUUID();
            TemplateEntity entity = activeEntity(id);
            when(templateRepository.findById(id)).thenReturn(Optional.of(entity));
            when(templateUpdateApplier.apply(any(TemplateEntity.class), any(UpdateTemplateRequest.class))).thenReturn(true);
            when(templateRepository.save(any(TemplateEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            TemplateDetailResponse mapped = new TemplateDetailResponse(
                    id, "new", "loc", "HTML", null, null, null, null, null, true, null, null);
            when(templateMapper.toDetailResponse(entity)).thenReturn(mapped);

            TemplateDetailResponse result = service.updateTemplate(
                    id, new UpdateTemplateRequest("new", null, null, null, null), requesterId, SESSION_TOKEN);

            assertThat(result).isSameAs(mapped);
            assertThat(entity.getUpdatedAt()).isNotNull();
            verify(applicationAuthorizationService).assertPermission(SESSION_TOKEN, applicationId, PERMISSION);
            verify(templateRepository).save(entity);
        }

        @Test
        @DisplayName("does not persist when no mutable field changed")
        void updateTemplate_noChange_noSave() {
            UUID id = UUID.randomUUID();
            TemplateEntity entity = activeEntity(id);
            when(templateRepository.findById(id)).thenReturn(Optional.of(entity));
            when(templateUpdateApplier.apply(any(TemplateEntity.class), any(UpdateTemplateRequest.class))).thenReturn(false);
            TemplateDetailResponse mapped = new TemplateDetailResponse(
                    id, "desc", null, null, null, null, null, null, null, true, null, null);
            when(templateMapper.toDetailResponse(entity)).thenReturn(mapped);

            TemplateDetailResponse result = service.updateTemplate(
                    id, new UpdateTemplateRequest(null, null, null, null, null), requesterId, SESSION_TOKEN);

            assertThat(result).isSameAs(mapped);
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TemplateNotFoundException for an unknown id")
        void updateTemplate_notFound() {
            UUID id = UUID.randomUUID();
            when(templateRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(TemplateNotFoundException.class,
                    () -> service.updateTemplate(id, new UpdateTemplateRequest(null, null, null, null, null), requesterId, SESSION_TOKEN));
            verify(applicationAuthorizationService, never()).assertPermission(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("throws TemplateNotFoundException for an inactive template (no reactivation)")
        void updateTemplate_inactiveTreatedAsNotFound() {
            UUID id = UUID.randomUUID();
            TemplateEntity entity = activeEntity(id);
            entity.setActive(false);
            when(templateRepository.findById(id)).thenReturn(Optional.of(entity));

            assertThrows(TemplateNotFoundException.class,
                    () -> service.updateTemplate(id, new UpdateTemplateRequest(null, null, null, null, null), requesterId, SESSION_TOKEN));
        }

        @Test
        @DisplayName("throws ForbiddenException when caller lacks permission for the application")
        void updateTemplate_forbidden() {
            UUID id = UUID.randomUUID();
            when(templateRepository.findById(id)).thenReturn(Optional.of(activeEntity(id)));
            stubForbidden();

            assertThrows(ForbiddenException.class,
                    () -> service.updateTemplate(id, new UpdateTemplateRequest(null, null, null, null, null), requesterId, SESSION_TOKEN));
            verify(templateRepository, never()).save(any());
        }
    }

    // ── deleteTemplate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTemplate tests")
    class DeleteTemplate {

        @Test
        @DisplayName("soft-deletes an active template")
        void deleteTemplate_success() {
            UUID id = UUID.randomUUID();
            TemplateEntity entity = activeEntity(id);
            when(templateRepository.findById(id)).thenReturn(Optional.of(entity));
            when(templateRepository.save(any(TemplateEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deleteTemplate(id, requesterId, SESSION_TOKEN);

            assertThat(entity.getActive()).isFalse();
            assertThat(entity.getUpdatedAt()).isNotNull();
            verify(applicationAuthorizationService).assertPermission(SESSION_TOKEN, applicationId, PERMISSION);
            verify(templateRepository).save(entity);
        }

        @Test
        @DisplayName("is idempotent: deleting an already-inactive template does not persist again")
        void deleteTemplate_idempotent() {
            UUID id = UUID.randomUUID();
            TemplateEntity entity = activeEntity(id);
            entity.setActive(false);
            when(templateRepository.findById(id)).thenReturn(Optional.of(entity));

            service.deleteTemplate(id, requesterId, SESSION_TOKEN);

            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TemplateNotFoundException for an unknown id")
        void deleteTemplate_notFound() {
            UUID id = UUID.randomUUID();
            when(templateRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(TemplateNotFoundException.class, () -> service.deleteTemplate(id, requesterId, SESSION_TOKEN));
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ForbiddenException when caller lacks permission for the application")
        void deleteTemplate_forbidden() {
            UUID id = UUID.randomUUID();
            when(templateRepository.findById(id)).thenReturn(Optional.of(activeEntity(id)));
            stubForbidden();

            assertThrows(ForbiddenException.class, () -> service.deleteTemplate(id, requesterId, SESSION_TOKEN));
            verify(templateRepository, never()).save(any());
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById tests")
    class GetById {

        @Test
        @DisplayName("returns the mapped response for an active template")
        void getById_success() {
            UUID id = UUID.randomUUID();
            TemplateEntity entity = activeEntity(id);
            when(templateRepository.findById(id)).thenReturn(Optional.of(entity));
            TemplateDetailResponse mapped = new TemplateDetailResponse(
                    id, "desc", null, null, null, null, null, null, null, true, null, null);
            when(templateMapper.toDetailResponse(entity)).thenReturn(mapped);

            TemplateDetailResponse result = service.getById(id, requesterId, SESSION_TOKEN);

            assertThat(result).isSameAs(mapped);
            verify(applicationAuthorizationService).assertPermission(SESSION_TOKEN, applicationId, PERMISSION);
        }

        @Test
        @DisplayName("throws TemplateNotFoundException for an unknown id")
        void getById_notFound() {
            UUID id = UUID.randomUUID();
            when(templateRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(TemplateNotFoundException.class, () -> service.getById(id, requesterId, SESSION_TOKEN));
        }

        @Test
        @DisplayName("throws TemplateNotFoundException for an inactive template")
        void getById_inactive_notFound() {
            UUID id = UUID.randomUUID();
            TemplateEntity entity = activeEntity(id);
            entity.setActive(false);
            when(templateRepository.findById(id)).thenReturn(Optional.of(entity));

            assertThrows(TemplateNotFoundException.class, () -> service.getById(id, requesterId, SESSION_TOKEN));
        }

        @Test
        @DisplayName("throws ForbiddenException when caller lacks permission for the application")
        void getById_forbidden() {
            UUID id = UUID.randomUUID();
            when(templateRepository.findById(id)).thenReturn(Optional.of(activeEntity(id)));
            stubForbidden();

            assertThrows(ForbiddenException.class, () -> service.getById(id, requesterId, SESSION_TOKEN));
        }
    }

    // ── searchTemplates ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchTemplates tests")
    class SearchTemplates {

        @SuppressWarnings("unchecked")
        private void stubEmptyPage() {
            when(templateRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
        }

        private TemplateSearchCriteria criteria(String q, UUID templateTypeId, UUID severityTypeId, Boolean active,
                                                 Integer page, Integer size, String sort) {
            return new TemplateSearchCriteria(
                    applicationId, requesterId, SESSION_TOKEN, q, templateTypeId, severityTypeId, active, page, size, sort);
        }

        @Test
        @DisplayName("applies default page/size/active and returns a stable envelope")
        void search_defaults() {
            stubEmptyPage();

            TemplateSearchResponse result = service.searchTemplates(criteria(null, null, null, null, null, null, null));

            assertThat(result.items()).isEmpty();
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);
            verify(applicationAuthorizationService).assertPermission(SESSION_TOKEN, applicationId, PERMISSION);
        }

        @Test
        @DisplayName("maps repository page content through the mapper")
        void search_mapsResults() {
            TemplateEntity e1 = activeEntity(UUID.randomUUID());
            TemplateEntity e2 = activeEntity(UUID.randomUUID());
            when(templateRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(e1, e2)));
            TemplateDetailResponse r1 = new TemplateDetailResponse(e1.getId(), "d1", null, null, null, null, null, null, null, true, null, null);
            TemplateDetailResponse r2 = new TemplateDetailResponse(e2.getId(), "d2", null, null, null, null, null, null, null, true, null, null);
            when(templateMapper.toDetailResponse(e1)).thenReturn(r1);
            when(templateMapper.toDetailResponse(e2)).thenReturn(r2);

            TemplateSearchResponse result = service.searchTemplates(criteria("welcome", null, null, null, 0, 10, "description,asc"));

            assertThat(result.items()).containsExactly(r1, r2);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when applicationId is null")
        void search_missingApplicationId() {
            TemplateSearchCriteria criteria = new TemplateSearchCriteria(
                    null, requesterId, SESSION_TOKEN, null, null, null, null, null, null, null);

            assertThrows(IllegalArgumentException.class, () -> service.searchTemplates(criteria));
            verify(applicationAuthorizationService, never()).assertPermission(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for a negative page")
        void search_negativePage() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchTemplates(criteria(null, null, null, null, -1, null, null)));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when size is below the minimum")
        void search_sizeTooSmall() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchTemplates(criteria(null, null, null, null, null, 0, null)));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when size exceeds the safe maximum")
        void search_sizeTooLarge() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchTemplates(criteria(null, null, null, null, null, 101, null)));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for an unsupported sort field")
        void search_unsupportedSortField() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchTemplates(criteria(null, null, null, null, null, null, "unknownField,asc")));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for an unsupported sort direction")
        void search_unsupportedSortDirection() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchTemplates(criteria(null, null, null, null, null, null, "createdAt,sideways")));
        }

        @Test
        @DisplayName("accepts an ascending sort direction and field-only sort specs")
        void search_ascendingAndFieldOnlySort() {
            stubEmptyPage();

            assertThat(service.searchTemplates(criteria(null, null, null, null, null, null, "updatedAt,asc"))).isNotNull();
            assertThat(service.searchTemplates(criteria(null, null, null, null, null, null, "fileFormat"))).isNotNull();
        }

        @Test
        @DisplayName("explicit active=false is honored (still gated by caller authorization)")
        void search_explicitInactiveFilter() {
            ArgumentCaptor<Specification<TemplateEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
            stubEmptyPage();

            service.searchTemplates(criteria(null, null, null, false, null, null, null));

            verify(templateRepository).findAll(specCaptor.capture(), any(Pageable.class));
            assertThat(specCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("throws ForbiddenException when caller lacks permission for the application")
        void search_forbidden() {
            stubForbidden();

            assertThrows(ForbiddenException.class,
                    () -> service.searchTemplates(criteria(null, null, null, null, null, null, null)));
            verify(templateRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("checks the application-scoped permission with the request's applicationId and session token")
        void search_authorizesAgainstRequestedApplication() {
            stubEmptyPage();

            service.searchTemplates(criteria(null, null, null, null, null, null, null));

            verify(applicationAuthorizationService).assertPermission(eq(SESSION_TOKEN), eq(applicationId), anyString());
        }
    }
}
