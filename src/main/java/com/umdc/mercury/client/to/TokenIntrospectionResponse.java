package com.umdc.mercury.client.to;

import java.util.List;

/**
 * Response of backbone-rest's {@code POST /api/v1/managed-clients/introspect} (RFC 7662: always 200; when the token is not valid
 * {@code active} is {@code false} and the rest is {@code null}).
 *
 * <p>Declared here, not taken from the shared {@code security-oauth} jar, because the published jar's record has no
 * {@code applicationId}: Mercury needs it to know which application a managed client belongs to.</p>
 *
 * @param active        whether the token is valid, not expired and not revoked
 * @param clientId      the managed client (subject)
 * @param clientName    its human-readable name
 * @param scopes        the granted scopes
 * @param issuer        the issuer claim
 * @param exp           expiry, Unix epoch seconds
 * @param iat           issued-at, Unix epoch seconds
 * @param jti           the token identifier
 * @param applicationId the application the client belongs to; {@code null} when an older backbone-rest does not return it
 */
public record TokenIntrospectionResponse(
        boolean active,
        String clientId,
        String clientName,
        List<String> scopes,
        String issuer,
        Long exp,
        Long iat,
        String jti,
        String applicationId
) {
}
