package com.umdc.mercury.client.to;

import java.util.UUID;

/**
 * Request body for backbone-rest's {@code POST /api/v1/iam/permissions/check}.
 *
 * <p>Mirrors {@code com.umdc.backoffice.v1.iam.permissions.api.to.PermissionCheckRequest}
 * on the backbone-rest side — kept as a local record here since it is not part of a
 * shared library, consistent with how other Feign client DTOs are declared per-client.</p>
 *
 * @param permission    role name (e.g. {@code TEMPLATE_MANAGE}) or feature name to check.
 * @param applicationId application scope; when set and the token carries a resolvable
 *                      {@code uid}, backbone-rest enforces this against the caller's
 *                      {@code general.application_role_user} ACL row for that application.
 * @param sessionToken  the caller's session token (Mercury- or backbone-signed — both are
 *                      valid, since both services sign with the same shared {@code APP_TOKEN_SECRET}).
 */
public record PermissionCheckRequest(String permission, UUID applicationId, String sessionToken) {
}
