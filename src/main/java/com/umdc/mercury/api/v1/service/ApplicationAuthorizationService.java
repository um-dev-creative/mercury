package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;

import java.util.UUID;

/**
 * Authorizes a caller against a specific application, using backbone-rest's
 * {@code general.application_role_user} ACL as the single source of truth — Mercury
 * does not maintain its own copy of that relation.
 */
public interface ApplicationAuthorizationService {

    /**
     * Asserts that the caller identified by {@code sessionToken} holds {@code permission}
     * for {@code applicationId}.
     *
     * @param sessionToken  the caller's session token (Mercury- or backbone-signed).
     * @param applicationId the application the permission is scoped to.
     * @param permission    the role or feature name required.
     * @throws ForbiddenException if the token is missing, the permission check fails, or
     *                            the authorization call itself could not be completed
     *                            (fails closed — an unreachable/erroring backbone denies access).
     */
    void assertPermission(String sessionToken, UUID applicationId, String permission);
}
