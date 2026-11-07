package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.TemplateApplicationResponse;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TemplateApplicationServiceImpl implements TemplateApplicationService {

    private final ApplicationRepository applications;
    private final ApplicationAuthorizationService authorization;
    private final String templateManagePermission;

    public TemplateApplicationServiceImpl(ApplicationRepository applications, ApplicationAuthorizationService authorization,
                                          @Value("${umdc.security.permissions.template-manage:TEMPLATE_MANAGE}") String templateManagePermission) {
        this.applications = applications;
        this.authorization = authorization;
        this.templateManagePermission = templateManagePermission;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateApplicationResponse> listManageable(String sessionToken) {
        boolean all = authorization.isPlatformAdmin(sessionToken);
        return applications.findAllByActiveTrue().stream()
                .filter(application -> all || authorization.holds(sessionToken, application.getId(), templateManagePermission))
                .map(TemplateApplicationServiceImpl::toResponse)
                .sorted(Comparator.comparing(TemplateApplicationResponse::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private static TemplateApplicationResponse toResponse(ApplicationEntity application) {
        return new TemplateApplicationResponse(application.getId(), application.getName(), application.getCodeName());
    }
}
