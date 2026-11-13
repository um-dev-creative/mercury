package com.umdc.mercury.security;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.security.jwt.JwtConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Exchanging a Backbone M2M token for a Mercury M2M token")
class MachineTokenExchangeTest {

    private static final String SECRET = "bG9jYWxEZXZTZWNyZXRLZXlGb3JNZXJjdXJ5Tm90Rm9yUHJvZHVjdGlvblVzZTEyMzQ1Ng==";
    private static final UUID APP = UUID.fromString("81398159-1785-46a5-15ae-f491fd1dc509");
    private static final String CLIENT = "c1f0f0a0-0000-4000-8000-000000000001";

    private SessionJwtServiceImpl jwt;
    private MachineTokenExchange exchange;

    @BeforeEach
    void setUp() {
        var properties = new JwtConfigProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(60_000L);
        var claims = new SessionJwtClaimsProperties();
        claims.setIssuer("mercury");
        claims.setAudience("mercury");
        claims.setTrustedIssuers(List.of("backbone-rest"));
        claims.setTrustedAudiences(List.of("backbone-rest-client"));
        claims.setValidateWithBackbone(false);
        jwt = new SessionJwtServiceImpl(properties, claims, mock(TokenRevocationService.class), mock(BackboneSessionValidator.class));
        exchange = new MachineTokenExchange(jwt);
    }

    private static OAuth2AuthenticatedPrincipal principal(Object application, String... scopes) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", CLIENT);
        attributes.put("client_name", "zerobound-rest");
        if (application != null) {
            attributes.put(ApplicationAwareTokenIntrospector.APPLICATION_ID_CLAIM, application);
        }
        List<org.springframework.security.core.GrantedAuthority> authorities = java.util.Arrays.stream(scopes)
                .<org.springframework.security.core.GrantedAuthority>map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)).toList();
        return new DefaultOAuth2AuthenticatedPrincipal(CLIENT, attributes, authorities);
    }

    @Test
    @DisplayName("the token carries the client, its application and only the Mercury scopes")
    void issuesAScopedToken() {
        var response = exchange.exchange(principal(APP.toString(), "mercury:message:read", "mercury:message:write", "other:scope"));

        var caller = jwt.getMachineCaller(response.token()).orElseThrow();
        assertThat(caller.clientId()).isEqualTo(CLIENT);
        assertThat(caller.clientName()).isEqualTo("zerobound-rest");
        assertThat(caller.applicationId()).isEqualTo(APP);
        assertThat(caller.scopes()).containsExactlyInAnyOrder("mercury:message:read", "mercury:message:write");
        assertThat(jwt.getVerifiedActorId(response.token())).contains(UUID.fromString(CLIENT));
    }

    @Test
    @DisplayName("a client Backbone does not tie to an application gets nothing (an older backbone-rest, or a bad value)")
    void refusesAClientWithoutAnApplication() {
        assertThatThrownBy(() -> exchange.exchange(principal(null, "mercury:message:write"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> exchange.exchange(principal("not-a-uuid", "mercury:message:write"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> exchange.exchange(principal(42, "mercury:message:write"))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("a client with no Mercury scope gets nothing")
    void refusesAClientWithoutAMercuryScope() {
        assertThatThrownBy(() -> exchange.exchange(principal(APP.toString(), "other:scope"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> exchange.exchange(principal(APP.toString()))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("a read-only client gets a read-only token")
    void keepsTheClientsOwnScopes() {
        var response = exchange.exchange(principal(APP.toString(), "mercury:message:read"));

        assertThat(jwt.getMachineCaller(response.token()).orElseThrow().allows(true)).isFalse();
        assertThat(jwt.getMachineCaller(response.token()).orElseThrow().allows(false)).isTrue();
    }
}
