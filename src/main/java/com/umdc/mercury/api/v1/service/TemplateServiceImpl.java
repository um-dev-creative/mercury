package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.TemplateNotFoundException;
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
import com.umdc.mercury.jpa.sql.repository.TemplateSpecifications;
import com.umdc.mercury.jpa.sql.repository.TemplateTypeEntityRepository;
import com.umdc.mercury.mapper.TemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String SORT_ASC = "asc";
    private static final String SORT_DESC = "desc";
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("description", "location", "fileFormat", "createdAt", "updatedAt");
    private static final String TEMPLATE_NOT_FOUND_MESSAGE = "Template not found: ";

    private final TemplateRepository templateRepository;
    private final TemplateTypeEntityRepository templateTypeRepository;
    private final SeverityTypeRepository severityTypeRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAuthorizationService applicationAuthorizationService;
    private final TemplateMapper templateMapper;
    private final TemplateUpdateApplier templateUpdateApplier;
    private final String templateManagePermission;

    public TemplateServiceImpl(TemplateRepository templateRepository,
                                TemplateTypeEntityRepository templateTypeRepository,
                                SeverityTypeRepository severityTypeRepository,
                                ApplicationRepository applicationRepository,
                                ApplicationAuthorizationService applicationAuthorizationService,
                                TemplateMapper templateMapper,
                                TemplateUpdateApplier templateUpdateApplier,
                                @Value("${umdc.security.permissions.template-manage:TEMPLATE_MANAGE}") String templateManagePermission) {
        this.templateRepository = templateRepository;
        this.templateTypeRepository = templateTypeRepository;
        this.severityTypeRepository = severityTypeRepository;
        this.applicationRepository = applicationRepository;
        this.applicationAuthorizationService = applicationAuthorizationService;
        this.templateMapper = templateMapper;
        this.templateUpdateApplier = templateUpdateApplier;
        this.templateManagePermission = templateManagePermission;
    }

    @Async
    @Override
    public CompletableFuture<TemplateDetailResponse> createTemplate(CreateTemplateRequest request, UUID requesterId, String sessionToken) {
        logger.info("Creating template. applicationId={}, requesterId={}", request.applicationId(), requesterId);
        applicationAuthorizationService.assertPermission(sessionToken, request.applicationId(), templateManagePermission);

        ApplicationEntity application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + request.applicationId()));
        TemplateTypeEntity templateType = requireActiveTemplateType(request.templateTypeId());
        SeverityTypeEntity severityType = requireActiveSeverityType(request.severityTypeId());

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        TemplateEntity entity = templateMapper.toEntity(request, templateType, application, severityType, now);
        entity = templateRepository.save(entity);

        logger.info("Template created. id={}, applicationId={}", entity.getId(), request.applicationId());
        return CompletableFuture.completedFuture(templateMapper.toDetailResponse(entity));
    }

    @Override
    public TemplateDetailResponse updateTemplate(UUID id, UpdateTemplateRequest request, UUID requesterId, String sessionToken) {
        logger.info("Updating template. id={}, requesterId={}", id, requesterId);
        TemplateEntity entity = requireActiveExisting(id);
        applicationAuthorizationService.assertPermission(sessionToken, entity.getApplication().getId(), templateManagePermission);

        boolean changed = templateUpdateApplier.apply(entity, request);
        if (changed) {
            entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            entity = templateRepository.save(entity);
            logger.info("Template updated. id={}", entity.getId());
        } else {
            logger.info("No mutable fields provided for update. templateId={}", id);
        }
        return templateMapper.toDetailResponse(entity);
    }

    @Override
    public void deleteTemplate(UUID id, UUID requesterId, String sessionToken) {
        logger.info("Soft-delete template request. id={}, requesterId={}", id, requesterId);
        TemplateEntity entity = requireAnyExisting(id);
        applicationAuthorizationService.assertPermission(sessionToken, entity.getApplication().getId(), templateManagePermission);

        if (Boolean.FALSE.equals(entity.getActive())) {
            logger.info("Template already inactive; delete is idempotent. id={}", id);
            return;
        }
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        templateRepository.save(entity);
        logger.info("Template soft-deleted. id={}", id);
    }

    @Override
    public TemplateDetailResponse getById(UUID id, UUID requesterId, String sessionToken) {
        logger.debug("Fetching template by id. id={}", id);
        TemplateEntity entity = requireActiveExisting(id);
        applicationAuthorizationService.assertPermission(sessionToken, entity.getApplication().getId(), templateManagePermission);
        return templateMapper.toDetailResponse(entity);
    }

    @Override
    public TemplateSearchResponse searchTemplates(TemplateSearchCriteria criteria) {
        logger.debug("Searching templates. applicationId={}", criteria.applicationId());
        if (Objects.isNull(criteria.applicationId())) {
            throw new IllegalArgumentException("applicationId is required");
        }
        applicationAuthorizationService.assertPermission(criteria.sessionToken(), criteria.applicationId(), templateManagePermission);

        int page = normalizePage(criteria.page());
        int size = normalizeSize(criteria.size());
        Sort sort = buildSort(criteria.sort());
        Pageable pageable = PageRequest.of(page, size, sort);

        Boolean activeFilter = Objects.isNull(criteria.active()) ? Boolean.TRUE : criteria.active();
        Specification<TemplateEntity> spec = TemplateSpecifications.search(
                criteria.applicationId(), criteria.q(), criteria.templateTypeId(), criteria.severityTypeId(), activeFilter);

        Page<TemplateEntity> result = templateRepository.findAll(spec, pageable);
        List<TemplateDetailResponse> items = result.getContent().stream()
                .map(templateMapper::toDetailResponse)
                .toList();

        return new TemplateSearchResponse(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TemplateEntity requireAnyExisting(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException(TEMPLATE_NOT_FOUND_MESSAGE + id));
    }

    /**
     * Fetches a template by id, treating an inactive (soft-deleted) template the same as a
     * missing one, since reactivation is out of scope.
     */
    private TemplateEntity requireActiveExisting(UUID id) {
        TemplateEntity entity = requireAnyExisting(id);
        if (!Boolean.TRUE.equals(entity.getActive())) {
            throw new TemplateNotFoundException(TEMPLATE_NOT_FOUND_MESSAGE + id);
        }
        return entity;
    }

    private TemplateTypeEntity requireActiveTemplateType(UUID id) {
        TemplateTypeEntity templateType = templateTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template type not found: " + id));
        if (!Boolean.TRUE.equals(templateType.getActive())) {
            throw new IllegalArgumentException("Template type is not active: " + id);
        }
        return templateType;
    }

    private SeverityTypeEntity requireActiveSeverityType(UUID id) {
        SeverityTypeEntity severityType = severityTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Severity type not found: " + id));
        if (!Boolean.TRUE.equals(severityType.getActive())) {
            throw new IllegalArgumentException("Severity type is not active: " + id);
        }
        return severityType;
    }

    private int normalizePage(Integer page) {
        if (Objects.isNull(page)) {
            return 0;
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (Objects.isNull(size)) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }

    private Sort buildSort(String sortSpec) {
        String field = DEFAULT_SORT_FIELD;
        Sort.Direction direction = Sort.Direction.DESC;
        if (Objects.nonNull(sortSpec) && !sortSpec.isBlank()) {
            String[] parts = sortSpec.split(",", 2);
            String candidateField = parts[0].trim();
            if (!ALLOWED_SORT_FIELDS.contains(candidateField)) {
                throw new IllegalArgumentException("Unsupported sort field: " + candidateField);
            }
            field = candidateField;
            String directionToken = parts.length > 1 ? parts[1].trim() : null;
            direction = resolveDirection(directionToken);
        }
        // Deterministic tie-breaker so equal-sort-key rows keep a stable relative order across pages.
        return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Sort.Direction resolveDirection(String directionToken) {
        if (Objects.isNull(directionToken) || directionToken.isBlank()) {
            return Sort.Direction.DESC;
        }
        if (SORT_ASC.equalsIgnoreCase(directionToken)) {
            return Sort.Direction.ASC;
        }
        if (SORT_DESC.equalsIgnoreCase(directionToken)) {
            return Sort.Direction.DESC;
        }
        throw new IllegalArgumentException("Unsupported sort direction: " + directionToken);
    }
}
