package com.umdc.mercury.security;

import com.umdc.mercury.client.BackboneIntrospectionClient;
import com.umdc.mercury.client.to.TokenIntrospectionRequest;
import com.umdc.mercury.client.to.TokenIntrospectionResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates a Backbone-issued M2M token by asking backbone-rest's introspection endpoint (validity, including revocation, is only
 * authoritative at Backbone) and keeps what Backbone says about the client: scopes, name and the <b>application it belongs to</b>
 * ({@link #APPLICATION_ID_CLAIM}).
 *
 * <p>Same behavior as the shared library's {@code BackboneOpaqueTokenIntrospector} plus the application claim. Mercury owns this
 * class so it does not depend on a {@code security-oauth} release that carries the claim.</p>
 */
@Component
public class ApplicationAwareTokenIntrospector implements OpaqueTokenIntrospector {

    /** Claim with the UUID of the application the managed client belongs to. */
    public static final String APPLICATION_ID_CLAIM = "application_id";

    private final BackboneIntrospectionClient backbone;

    public ApplicationAwareTokenIntrospector(BackboneIntrospectionClient backbone) {
        this.backbone = backbone;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        TokenIntrospectionResponse response;
        try {
            response = backbone.introspect(new TokenIntrospectionRequest(token));
        } catch (Exception e) {
            throw new OAuth2IntrospectionException("Unable to reach backbone introspection endpoint", e);
        }
        if (Objects.isNull(response) || !response.active()) {
            throw new BadOpaqueTokenException("Token is not active");
        }
        List<String> scopes = Objects.isNull(response.scopes()) ? List.of() : response.scopes();
        Map<String, Object> claims = new HashMap<>();
        claims.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, response.active());
        claims.put(OAuth2TokenIntrospectionClaimNames.SCOPE, scopes);
        putIfPresent(claims, OAuth2TokenIntrospectionClaimNames.CLIENT_ID, response.clientId());
        putIfPresent(claims, OAuth2TokenIntrospectionClaimNames.JTI, response.jti());
        putIfPresent(claims, "client_name", response.clientName());
        putIfPresent(claims, APPLICATION_ID_CLAIM, response.applicationId());
        if (Objects.nonNull(response.exp())) {
            claims.put(OAuth2TokenIntrospectionClaimNames.EXP, Instant.ofEpochSecond(response.exp()));
        }
        if (Objects.nonNull(response.iat())) {
            claims.put(OAuth2TokenIntrospectionClaimNames.IAT, Instant.ofEpochSecond(response.iat()));
        }
        List<GrantedAuthority> authorities = scopes.stream()
                .<GrantedAuthority>map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
        return new OAuth2IntrospectionAuthenticatedPrincipal(response.clientId(), claims, authorities);
    }

    private static void putIfPresent(Map<String, Object> claims, String name, Object value) {
        if (Objects.nonNull(value)) {
            claims.put(name, value);
        }
    }
}
