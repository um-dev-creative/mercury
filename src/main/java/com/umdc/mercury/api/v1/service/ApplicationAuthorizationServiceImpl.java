package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.client.BackbonePermissionClient;
import com.umdc.mercury.client.to.PermissionCheckRequest;
import com.umdc.mercury.client.to.PermissionCheckResponse;
import com.umdc.mercury.api.v1.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class ApplicationAuthorizationServiceImpl implements ApplicationAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationAuthorizationServiceImpl.class);
    private static final String DENIED_MESSAGE = "Caller lacks permission to manage this application";

    private final BackbonePermissionClient backbonePermissionClient;

    public ApplicationAuthorizationServiceImpl(BackbonePermissionClient backbonePermissionClient) {
        this.backbonePermissionClient = backbonePermissionClient;
    }

    @Override
    public void assertPermission(String sessionToken, UUID applicationId, String permission) {
        if (Objects.isNull(sessionToken) || sessionToken.isBlank() || Objects.isNull(applicationId)) {
            logger.warn("Cannot authorize — missing sessionToken or applicationId");
            throw new ForbiddenException(DENIED_MESSAGE);
        }

        PermissionCheckResponse response = callBackbone(sessionToken, applicationId, permission);

        if (Objects.isNull(response) || !response.granted()) {
            String reason = Objects.nonNull(response) ? response.reason() : "no response from backbone";
            logger.warn("Permission denied. applicationId={}, permission={}, reason={}", applicationId, permission, reason);
            throw new ForbiddenException(DENIED_MESSAGE);
        }
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
