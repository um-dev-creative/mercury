package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.TemplateApplicationResponse;
import com.umdc.mercury.client.to.BackboneApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class TemplateApplicationServiceImpl implements TemplateApplicationService {

    private final ApplicationDirectory applications;
    private final ApplicationAuthorizationService authorization;
    private final String templateManagePermission;

    public TemplateApplicationServiceImpl(ApplicationDirectory applications, ApplicationAuthorizationService authorization,
                                          @Value("${umdc.security.permissions.template-manage:TEMPLATE_MANAGE}") String templateManagePermission) {
        this.applications = applications;
        this.authorization = authorization;
        this.templateManagePermission = templateManagePermission;
    }

    @Override
    public List<TemplateApplicationResponse> listManageable(String sessionToken) {
        boolean all = authorization.isPlatformAdmin(sessionToken);
        return applications.active(sessionToken).stream()
                .filter(application -> all || authorization.holds(sessionToken, application.id(), templateManagePermission))
                .map(TemplateApplicationServiceImpl::toResponse)
                .sorted(Comparator.comparing(TemplateApplicationResponse::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private static TemplateApplicationResponse toResponse(BackboneApplication application) {
        return new TemplateApplicationResponse(application.id(), application.name(), application.codeName());
    }
}
