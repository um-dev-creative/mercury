package com.umdc.mercury.security;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.security.jwt.JwtConfigProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Only Mercury-minted backend tokens reach /mail and /verification-code")
class MachineTokenInterceptorTest {

    private static final String SECRET =
            "bG9jYWxEZXZTZWNyZXRLZXlGb3JNZXJjdXJ5Tm90Rm9yUHJvZHVjdGlvblVzZTEyMzQ1Ng==";

    private SessionJwtServiceImpl jwt;
    private MachineTokenInterceptor interceptor;

    @BeforeEach
    void setUp() {
        var properties = new JwtConfigProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(60_000L);
        var claims = new SessionJwtClaimsProperties();
        claims.setIssuer("mercury");
        claims.setAudience("mercury");
        claims.setTrustedIssuers(java.util.List.of("backbone-rest"));
        claims.setTrustedAudiences(java.util.List.of("backbone-rest-client"));
        claims.setValidateWithBackbone(false);
        jwt = new SessionJwtServiceImpl(properties, claims, mock(TokenRevocationService.class),
                mock(BackboneSessionValidator.class));
        interceptor = new MachineTokenInterceptor(jwt);
    }

    private String token(String issuer, String audience, String uid, String subject) {
        var builder = Jwts.builder().claims(Map.of("type", "session-token")).id(UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)));
        if (uid != null) {
            builder.claim("uid", uid);
        }
        if (subject != null) {
            builder.subject(subject);
        }
        return builder.issuer(issuer).audience().add(audience).and().compact();
    }

    private MockHttpServletRequest requestWith(String token) {
        var request = new MockHttpServletRequest("POST", "/api/v1/mail");
        if (token != null) {
            request.addHeader("session-token", token);
        }
        return request;
    }

    @Test
    @DisplayName("a token minted by /auth/token passes and names its caller")
    void machineTokenPasses() {
        var request = requestWith(jwt.generateSessionToken("zerobound", Map.of()));

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(request.getAttribute(MachineTokenInterceptor.CALLER)).isEqualTo("zerobound");
    }

    @Test
    @DisplayName("a backbone-rest user session is refused even though the generic gate trusts it")
    void backboneUserSessionIsRefused() throws Exception {
        var userToken = token("backbone-rest", "backbone-rest-client", UUID.randomUUID().toString(), "alice");

        assertThat(jwt.getTokenClaims(userToken)).isNotNull();   // trusted by the generic gate...
        assertThatThrownBy(() -> interceptor.preHandle(requestWith(userToken), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(InvalidSessionTokenException.class);   // ...but not here
    }

    @Test
    @DisplayName("a token with Mercury's iss/aud that carries a uid is a user, not a backend")
    void tokenWithUidIsRefused() {
        var withUid = token("mercury", "mercury", UUID.randomUUID().toString(), "alice");

        assertThatThrownBy(() -> interceptor.preHandle(requestWith(withUid), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(InvalidSessionTokenException.class);
    }

    @Test
    @DisplayName("missing, blank, garbage and subject-less tokens are refused")
    void unusableTokensAreRefused() {
        var noSubject = token("mercury", "mercury", null, null);
        for (String bad : new String[]{null, "", "  ", "not-a-jwt", noSubject}) {
            assertThatThrownBy(() -> interceptor.preHandle(requestWith(bad), new MockHttpServletResponse(), new Object()))
                    .isInstanceOf(InvalidSessionTokenException.class);
        }
    }

    @Test
    @DisplayName("the configurer guards exactly /mail and /verification-code")
    void configurerRegistersTheInterceptor() {
        var registry = new org.springframework.web.servlet.config.annotation.InterceptorRegistry();
        new MachineTokenWebConfigurer(interceptor).addInterceptors(registry);

        assertThat(registry).extracting("registrations").asList().hasSize(1);
    }
}
