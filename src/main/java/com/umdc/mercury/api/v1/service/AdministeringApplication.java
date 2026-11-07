package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/// The backbone-rest application whose roles administer Mercury (Iris). It is found by its `code_name`
/// (`umdc.security.login-clients.admin-application`, default `iris`) in the applications table, so no id has to be copied between
/// services or environments; `umdc.security.login-clients.admin-application-id` forces a UUID instead. It fails closed: when it
/// cannot be resolved to exactly one application nobody is an administrator.
@Component
public class AdministeringApplication {

    static final String NOT_CONFIGURED = "The administering application is not configured";
    private static final Logger LOGGER = LoggerFactory.getLogger(AdministeringApplication.class);
    private static final int EXACTLY_ONE = 1;

    private final ApplicationRepository applications;
    private final String applicationId;
    private final String codeName;

    public AdministeringApplication(ApplicationRepository applications,
                                    @Value("${umdc.security.login-clients.admin-application-id:}") String applicationId,
                                    @Value("${umdc.security.login-clients.admin-application:iris}") String codeName) {
        this.applications = applications;
        this.applicationId = applicationId;
        this.codeName = codeName;
    }

    /// The id of the administering application.
    ///
    /// @throws ForbiddenException when it is not configured, or the `code_name` matches none or several applications
    public UUID id() {
        if (Objects.nonNull(applicationId) && !applicationId.isBlank()) {
            try {
                return UUID.fromString(applicationId.trim());
            } catch (IllegalArgumentException e) {
                LOGGER.error("umdc.security.login-clients.admin-application-id is not a UUID (value '{}'): remove LOGIN_CLIENTS_ADMIN_APPLICATION_ID "
                        + "so the application is found by code_name, or set the application's UUID", applicationId.trim());
                throw new ForbiddenException(NOT_CONFIGURED, e);
            }
        }
        if (Objects.isNull(codeName) || codeName.isBlank()) {
            LOGGER.warn("Administration refused: umdc.security.login-clients.admin-application is empty");
            throw new ForbiddenException(NOT_CONFIGURED);
        }
        List<ApplicationEntity> found = applications.findAllByCodeNameIgnoreCase(codeName.trim());
        if (found.size() != EXACTLY_ONE) {
            LOGGER.warn("Administration refused: {} applications have the code_name '{}' (exactly one is needed)", found.size(), codeName.trim());
            throw new ForbiddenException(NOT_CONFIGURED);
        }
        return found.get(0).getId();
    }
}
