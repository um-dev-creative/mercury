package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.client.BackbonePermissionClient;
import com.umdc.mercury.client.to.PermissionCheckRequest;
import com.umdc.mercury.client.to.PermissionCheckResponse;
import com.umdc.mercury.api.v1.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class ApplicationAuthorizationServiceImpl implements ApplicationAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationAuthorizationServiceImpl.class);
    private static final String DENIED_MESSAGE = "Caller lacks permission to manage this application";

    private final BackbonePermissionClient backbonePermissionClient;
    private final AdministeringApplication administeringApplication;
    private final String platformAdminPermission;

    public ApplicationAuthorizationServiceImpl(BackbonePermissionClient backbonePermissionClient,
                                               AdministeringApplication administeringApplication,
                                               @Value("${umdc.security.permissions.platform-admin:PLATFORM_ADMIN}") String platformAdminPermission) {
        this.backbonePermissionClient = backbonePermissionClient;
        this.administeringApplication = administeringApplication;
        this.platformAdminPermission = platformAdminPermission;
    }

    @Override
    public void assertPermission(String sessionToken, UUID applicationId, String permission) {
        if (Objects.isNull(sessionToken) || sessionToken.isBlank() || Objects.isNull(applicationId)) {
            logger.warn("Cannot authorize — missing sessionToken or applicationId");
            throw new ForbiddenException(DENIED_MESSAGE);
        }
        if (!holds(sessionToken, applicationId, permission) && !isPlatformAdmin(sessionToken)) {
            logger.warn("Permission denied. applicationId={}, permission={}", applicationId, permission);
            throw new ForbiddenException(DENIED_MESSAGE);
        }
    }

    @Override
    public boolean holds(String sessionToken, UUID applicationId, String permission) {
        if (Objects.isNull(sessionToken) || sessionToken.isBlank() || Objects.isNull(applicationId)) {
            return false;
        }
        PermissionCheckResponse response = callBackbone(sessionToken, applicationId, permission);
        if (Objects.isNull(response) || !response.granted()) {
            logger.debug("Permission not held. applicationId={}, permission={}, reason={}", applicationId, permission,
                    Objects.nonNull(response) ? response.reason() : "no response from backbone");
            return false;
        }
        return true;
    }

    @Override
    public boolean isPlatformAdmin(String sessionToken) {
        if (Objects.isNull(sessionToken) || sessionToken.isBlank()) {
            return false;
        }
        UUID administering;
        try {
            administering = administeringApplication.id(sessionToken);
        } catch (ForbiddenException e) {
            return false;
        }
        return holds(sessionToken, administering, platformAdminPermission);
    }

    /**
     * Calls backbone-rest's permission-check endpoint, failing closed (deny access)
     * on any error — an unreachable or erroring identity service must never be
     * treated as an implicit grant.
     */
    private PermissionCheckResponse callBackbone(String sessionToken, UUID applicationId, String permission) {
        try {
            return backbonePermissionClient.check(new PermissionCheckRequest(permission, applicationId, sessionToken));
        } catch (Exception e) {
            logger.error("Backbone permission check failed. applicationId={}, permission={}: {}",
                    applicationId, permission, e.getMessage(), e);
            return null;
        }
    }
}
