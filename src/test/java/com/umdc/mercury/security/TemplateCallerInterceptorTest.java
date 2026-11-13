package com.umdc.mercury.security;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.security.jwt.JwtConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Who may call /api/v1/templates: a person with the service Bearer, or an application with its Mercury M2M token")
class TemplateCallerInterceptorTest {

    private static final String SECRET = "bG9jYWxEZXZTZWNyZXRLZXlGb3JNZXJjdXJ5Tm90Rm9yUHJvZHVjdGlvblVzZTEyMzQ1Ng==";
    private static final UUID APP = UUID.fromString("81398159-1785-46a5-15ae-f491fd1dc509");

    private SessionJwtServiceImpl jwt;
    private TemplateCallerInterceptor interceptor;

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
        interceptor = new TemplateCallerInterceptor(jwt);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String method, String token) {
        var request = new MockHttpServletRequest(method, "/api/v1/templates");
        if (token != null) {
            request.addHeader("session-token", token);
        }
        return request;
    }

    private String machine(Set<String> scopes) {
        return jwt.generateMachineToken(UUID.randomUUID().toString(), "svc", APP, scopes);
    }

    @Test
    @DisplayName("a person of Iris: the service Bearer was already authenticated, so the call passes untouched")
    void aBearerAuthenticatedCallPasses() {
        var principal = new DefaultOAuth2AuthenticatedPrincipal("client", Map.of("sub", "client"), List.of());
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "t", Instant.now(), Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, token, List.of()));

        assertThat(interceptor.preHandle(request("POST", "any"), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    @DisplayName("an application with the write scope may change templates and is recorded as the caller")
    void aMachineWithTheWriteScopePasses() {
        var request = request("POST", machine(Set.of(MachineCaller.SCOPE_WRITE)));

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(request.getAttribute(MachineTokenInterceptor.CALLER)).isEqualTo(APP.toString());
    }

    @Test
    @DisplayName("a read-only application may look at templates but not change them")
    void aReadOnlyMachineMayOnlyLook() {
        var readOnly = machine(Set.of(MachineCaller.SCOPE_READ));

        assertThat(interceptor.preHandle(request("GET", readOnly), new MockHttpServletResponse(), new Object())).isTrue();
        for (String method : new String[]{"POST", "PUT", "DELETE"}) {
            assertThatThrownBy(() -> interceptor.preHandle(request(method, readOnly), new MockHttpServletResponse(), new Object()))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Test
    @DisplayName("a person's session token with no service Bearer is refused: leaving the header out must not skip that layer")
    void aPersonWithoutTheBearerIsRefused() {
        var person = jwt.generateSessionToken("alice", Map.of("uid", UUID.randomUUID().toString()));

        assertThatThrownBy(() -> interceptor.preHandle(request("POST", person), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(InvalidSessionTokenException.class);
    }

    @Test
    @DisplayName("no token, or an anonymous authentication, is refused")
    void anonymousIsRefused() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anonymous", "n/a"));

        assertThatThrownBy(() -> interceptor.preHandle(request("GET", null), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(InvalidSessionTokenException.class);
    }
}
