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
     * @param permission    the role or feature name required. A platform administrator ({@link #isPlatformAdmin}) is granted it in every application.
     * @throws ForbiddenException if the token is missing, the permission check fails, or
     *                            the authorization call itself could not be completed
     *                            (fails closed — an unreachable/erroring backbone denies access).
     */
    void assertPermission(String sessionToken, UUID applicationId, String permission);

    /**
     * Whether the caller is a platform administrator: it holds the platform-admin permission
     * ({@code umdc.security.permissions.platform-admin}, default {@code PLATFORM_ADMIN}) in the administering application (Iris).
     * A platform administrator manages every application and its templates. Fails closed: any doubt answers {@code false}.
     *
     * @param sessionToken the caller's session token
     * @return true only when backbone grants the platform-admin permission
     */
    boolean isPlatformAdmin(String sessionToken);

    /**
     * Whether backbone grants {@code permission} to the caller in {@code applicationId} itself, without the platform-administrator
     * exception of {@link #assertPermission}. Fails closed.
     *
     * @param sessionToken  the caller's session token
     * @param applicationId the application the permission is scoped to
     * @param permission    the role or feature name required
     * @return true only when backbone grants it
     */
    boolean holds(String sessionToken, UUID applicationId, String permission);
}
