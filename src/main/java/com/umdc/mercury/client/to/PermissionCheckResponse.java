package com.umdc.mercury.client.to;

/**
 * Response body from backbone-rest's {@code POST /api/v1/iam/permissions/check}.
 *
 * @param granted    {@code true} if the caller holds the requested permission.
 * @param permission the permission string that was evaluated.
 * @param reason     human-readable explanation of the result (denial reason, or "Permission granted").
 */
public record PermissionCheckResponse(boolean granted, String permission, String reason) {
}
