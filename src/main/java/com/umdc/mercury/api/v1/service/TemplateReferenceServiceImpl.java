package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.ReferenceItemResponse;
import com.umdc.mercury.api.v1.to.TemplateDefinedItemResponse;
import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.repository.SeverityTypeRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateTypeEntityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TemplateReferenceServiceImpl implements TemplateReferenceService {

    private final TemplateTypeEntityRepository templateTypeRepository;
    private final SeverityTypeRepository severityTypeRepository;
    private final TemplateDefinedRepository templateDefinedRepository;
    private final ApplicationAuthorizationService applicationAuthorizationService;
    private final String templateReadPermission;

    public TemplateReferenceServiceImpl(TemplateTypeEntityRepository templateTypeRepository,
                                        SeverityTypeRepository severityTypeRepository,
                                        TemplateDefinedRepository templateDefinedRepository,
                                        ApplicationAuthorizationService applicationAuthorizationService,
                                        @Value("${umdc.security.permissions.template-read:${umdc.security.permissions.template-manage:TEMPLATE_MANAGE}}")
                                        String templateReadPermission) {
        this.templateTypeRepository = templateTypeRepository;
        this.severityTypeRepository = severityTypeRepository;
        this.templateDefinedRepository = templateDefinedRepository;
        this.applicationAuthorizationService = applicationAuthorizationService;
        this.templateReadPermission = templateReadPermission;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> listTemplateTypes() {
        return templateTypeRepository.findAll().stream()
                .filter(type -> !Boolean.FALSE.equals(type.getActive()))
                .map(type -> new ReferenceItemResponse(type.getId(), type.getName(), type.getDescription()))
                .sorted(byName())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> listSeverityTypes() {
        return severityTypeRepository.findAll().stream()
                .filter(type -> !Boolean.FALSE.equals(type.getActive()))
                .map(type -> new ReferenceItemResponse(type.getId(), type.getName(), type.getDescription()))
                .sorted(byName())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDefinedItemResponse> listDefined(UUID applicationId, String sessionToken) {
        if (Objects.isNull(applicationId)) {
            throw new IllegalArgumentException("applicationId is required");
        }
        applicationAuthorizationService.assertPermission(sessionToken, applicationId, templateReadPermission);
        return templateDefinedRepository.findByApplicationIdAndIsActiveTrue(applicationId).stream()
                .filter(defined -> Objects.nonNull(defined.getTemplate()) && !Boolean.FALSE.equals(defined.getTemplate().getActive()))
                .map(TemplateReferenceServiceImpl::toItem)
                .sorted(Comparator.comparing(TemplateDefinedItemResponse::description, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private static TemplateDefinedItemResponse toItem(TemplateDefinedEntity defined) {
        TemplateEntity template = defined.getTemplate();
        String typeName = Objects.isNull(template.getTemplateType()) ? null : template.getTemplateType().getName();
        return new TemplateDefinedItemResponse(defined.getId(), template.getId(), template.getDescription(),
                template.getLocation(), template.getFileFormat(), typeName);
    }

    private static Comparator<ReferenceItemResponse> byName() {
        return Comparator.comparing(ReferenceItemResponse::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
