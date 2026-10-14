package com.umdc.mercury.security;

import com.umdc.security.exception.CertificateSecurityException;
import com.umdc.security.jwt.JwtConfigProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SessionJwtServiceImpl unit tests")
class SessionJwtServiceImplTest {

    private static final String SECRET =
            "bG9jYWxEZXZTZWNyZXRLZXlGb3JNZXJjdXJ5Tm90Rm9yUHJvZHVjdGlvblVzZTEyMzQ1Ng==";

    private SessionJwtServiceImpl sessionJwtService;
    private SessionJwtClaimsProperties claimsProperties;
    private TokenRevocationService revocationService;
    private BackboneSessionValidator backboneValidator;

    @BeforeEach
    void setUp() {
        var properties = new JwtConfigProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(60_000L);
        claimsProperties = new SessionJwtClaimsProperties();
        claimsProperties.setIssuer("mercury");
        claimsProperties.setAudience("mercury");
        claimsProperties.setAllowMissingClaims(false);
        revocationService = mock(TokenRevocationService.class);
        backboneValidator = mock(BackboneSessionValidator.class);
        sessionJwtService = new SessionJwtServiceImpl(properties, claimsProperties, revocationService, backboneValidator);
    }

    /** A validly-signed, unexpired token with the given (nullable) iss/aud and a fresh jti. */
    private String signedToken(String issuer, String audience) {
        return signedToken("session-token", issuer, audience);
    }

    private String signedToken(String type, String issuer, String audience) {
        var builder = Jwts.builder()
                .claims(Map.of("uid", UUID.randomUUID().toString(), "type", type))
                .id(UUID.randomUUID().toString())
                .subject("alice")
                .expiration(java.util.Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)));
        if (issuer != null) {
            builder.issuer(issuer);
        }
        if (audience != null) {
            builder.audience().add(audience);
        }
        return builder.compact();
    }

    @Test
    @DisplayName("resolves the uid claim from a validly-signed, unexpired token")
    void getVerifiedUid_validToken_returnsUid() {
        UUID uid = UUID.randomUUID();
        Map<String, String> claims = new HashMap<>();
        claims.put("uid", uid.toString());
        String token = sessionJwtService.generateSessionToken("alice", claims);

        Optional<UUID> result = sessionJwtService.getVerifiedUid(token);

        assertThat(result).contains(uid);
    }

    @Test
    @DisplayName("rejects a token signed with a different secret, even with a well-formed uid claim")
    void getVerifiedUid_wrongSignature_returnsEmpty() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode("YW5vdGhlcldyb25nU2VjcmV0S2V5Rm9yVGVzdGluZzEyMzQ1Njc4OTA="));
        String forged = Jwts.builder()
                .claims(Map.of("uid", UUID.randomUUID().toString(), "type", "session-token"))
                .subject("attacker")
                .signWith(otherKey)
                .compact();

        Optional<UUID> result = sessionJwtService.getVerifiedUid(forged);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("rejects an expired token")
    void getVerifiedUid_expiredToken_returnsEmpty() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        String expired = Jwts.builder()
                .claims(Map.of("uid", UUID.randomUUID().toString(), "type", "session-token"))
                .subject("alice")
                .expiration(java.util.Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        Optional<UUID> result = sessionJwtService.getVerifiedUid(expired);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("rejects a null token")
    void getVerifiedUid_nullToken_returnsEmpty() {
        assertThat(sessionJwtService.getVerifiedUid(null)).isEmpty();
    }

    @Test
    @DisplayName("rejects a blank token")
    void getVerifiedUid_blankToken_returnsEmpty() {
        assertThat(sessionJwtService.getVerifiedUid("   ")).isEmpty();
    }

    @Test
    @DisplayName("rejects a non-JWT garbage string")
    void getVerifiedUid_garbageString_returnsEmpty() {
        assertThat(sessionJwtService.getVerifiedUid("not-a-jwt-at-all")).isEmpty();
    }

    @Test
    @DisplayName("rejects a validly-signed token whose uid claim is not a UUID")
    void getVerifiedUid_malformedUidClaim_returnsEmpty() {
        Map<String, String> claims = new HashMap<>();
        claims.put("uid", "not-a-uuid");
        String token = sessionJwtService.generateSessionToken("alice", claims);

        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("rejects a validly-signed token with no uid claim at all")
    void getVerifiedUid_missingUidClaim_returnsEmpty() {
        String token = sessionJwtService.generateSessionToken("alice", new HashMap<>());

        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("generateSessionToken stamps the configured iss and aud, and a jti")
    void generateSessionToken_stampsIssuerAudienceAndJti() throws CertificateSecurityException {
        claimsProperties.setIssuer("mercury-issuer");
        claimsProperties.setAudience("mercury-audience");

        Claims claims = sessionJwtService.getTokenClaims(sessionJwtService.generateSessionToken("alice", null));

        assertThat(claims.getIssuer()).isEqualTo("mercury-issuer");
        assertThat(claims.getAudience()).containsExactly("mercury-audience");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    @DisplayName("caller-supplied parameters cannot override iss or aud")
    void generateSessionToken_parametersCannotOverrideIssuerAudience() throws CertificateSecurityException {
        Map<String, String> params = new HashMap<>();
        params.put("iss", "attacker");
        params.put("aud", "attacker");

        Claims claims = sessionJwtService.getTokenClaims(sessionJwtService.generateSessionToken("alice", params));

        assertThat(claims.getIssuer()).isEqualTo("mercury");
        assertThat(claims.getAudience()).containsExactly("mercury");
    }

    @Test
    @DisplayName("rejects a validly-signed token with a different iss")
    void getTokenClaims_wrongIssuer_throws() {
        String token = signedToken("someone-else", "mercury");

        assertThatThrownBy(() -> sessionJwtService.getTokenClaims(token))
                .isInstanceOf(CertificateSecurityException.class)
                .hasMessageContaining("issuer");
        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("rejects a validly-signed token whose aud does not include Mercury")
    void getTokenClaims_wrongAudience_throws() {
        String token = signedToken("mercury", "another-service");

        assertThatThrownBy(() -> sessionJwtService.getTokenClaims(token))
                .isInstanceOf(CertificateSecurityException.class)
                .hasMessageContaining("audience");
        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("rejects a token with no iss/aud when missing claims are not allowed")
    void getTokenClaims_missingClaims_strict_throws() {
        String token = signedToken(null, null);

        assertThatThrownBy(() -> sessionJwtService.getTokenClaims(token))
                .isInstanceOf(CertificateSecurityException.class);
        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("accepts a token with no iss/aud during the transition grace period")
    void getTokenClaims_missingClaims_lenient_accepts() {
        claimsProperties.setAllowMissingClaims(true);

        assertThat(sessionJwtService.getVerifiedUid(signedToken(null, null))).isPresent();
    }

    @Test
    @DisplayName("still rejects a PRESENT-but-wrong iss/aud during the grace period")
    void getTokenClaims_wrongClaims_lenient_stillRejects() {
        claimsProperties.setAllowMissingClaims(true);

        assertThat(sessionJwtService.getVerifiedUid(signedToken("someone-else", "mercury"))).isEmpty();
        assertThat(sessionJwtService.getVerifiedUid(signedToken("mercury", "another-service"))).isEmpty();
    }

    @Test
    @DisplayName("rejects a token whose jti was revoked, even though it has not expired")
    void getTokenClaims_revokedJti_throws() {
        String token = signedToken("mercury", "mercury");
        when(revocationService.isRevoked(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        assertThatThrownBy(() -> sessionJwtService.getTokenClaims(token))
                .isInstanceOf(CertificateSecurityException.class)
                .hasMessageContaining("revoked");
        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("fails closed when the revocation store cannot be consulted")
    void getTokenClaims_revocationStoreDown_failsClosed() {
        String token = signedToken("mercury", "mercury");
        when(revocationService.isRevoked(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new DataAccessResourceFailureException("mongo down"));

        assertThatThrownBy(() -> sessionJwtService.getTokenClaims(token))
                .isInstanceOf(CertificateSecurityException.class);
        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("getUsernameFromToken returns the subject of a trusted token and rejects an untrusted one")
    void getUsernameFromToken_enforcesSameGate() {
        assertThat(sessionJwtService.getUsernameFromToken(signedToken("mercury", "mercury"))).isEqualTo("alice");

        String wrongIssuer = signedToken("someone-else", "mercury");
        assertThatThrownBy(() -> sessionJwtService.getUsernameFromToken(wrongIssuer))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("accepts a backbone-issued token whose iss/aud are listed as trusted")
    void getTokenClaims_trustedIssuerAndAudience_accepts() {
        claimsProperties.setTrustedIssuers(java.util.List.of("backbone"));
        claimsProperties.setTrustedAudiences(java.util.List.of("backbone-clients"));

        assertThat(sessionJwtService.getVerifiedUid(signedToken("backbone", "backbone-clients"))).isPresent();
        assertThat(sessionJwtService.getVerifiedUid(signedToken("mercury", "mercury"))).isPresent();
    }

    @Test
    @DisplayName("an issuer or audience that is neither Mercury's nor trusted is still rejected")
    void getTokenClaims_untrustedIssuerAndAudience_rejects() {
        claimsProperties.setTrustedIssuers(java.util.List.of("backbone"));
        claimsProperties.setTrustedAudiences(java.util.List.of("backbone-clients"));

        assertThat(sessionJwtService.getVerifiedUid(signedToken("evil", "backbone-clients"))).isEmpty();
        assertThat(sessionJwtService.getVerifiedUid(signedToken("backbone", "evil"))).isEmpty();
    }

    @Test
    @DisplayName("a null trusted list falls back to Mercury's own issuer/audience only")
    void acceptedValues_nullTrustedLists_ownOnly() {
        claimsProperties.setTrustedIssuers(null);
        claimsProperties.setTrustedAudiences(null);

        assertThat(claimsProperties.acceptedIssuers()).containsExactly("mercury");
        assertThat(claimsProperties.acceptedAudiences()).containsExactly("mercury");
    }

    @Test
    @DisplayName("rejects a backbone refresh token (same key, carries uid) presented as a session-token")
    void getTokenClaims_refreshToken_throws() {
        String refresh = signedToken("refresh-token", "mercury", "mercury");

        assertThatThrownBy(() -> sessionJwtService.getTokenClaims(refresh))
                .isInstanceOf(CertificateSecurityException.class)
                .hasMessageContaining("session token");
        assertThat(sessionJwtService.getVerifiedUid(refresh)).isEmpty();
    }

    @Test
    @DisplayName("accepts a token shaped like backbone-rest's own session-token (default iss/aud)")
    void getVerifiedUid_backboneShapedToken_accepted() {
        claimsProperties.setTrustedIssuers(java.util.List.of("backbone-rest"));
        claimsProperties.setTrustedAudiences(java.util.List.of("backbone-rest-client"));

        assertThat(sessionJwtService.getVerifiedUid(signedToken("backbone-rest", "backbone-rest-client")))
                .isPresent();
    }

    @Test
    @DisplayName("a token Mercury minted itself is not sent to backbone-rest for validation")
    void getTokenClaims_ownIssuer_skipsBackbone() throws CertificateSecurityException {
        sessionJwtService.getTokenClaims(signedToken("mercury", "mercury"));

        verify(backboneValidator, never()).assertActive(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("a backbone-issued token is checked with backbone-rest and accepted when it is still active")
    void getTokenClaims_backboneIssuer_checkedWithBackbone() throws CertificateSecurityException {
        claimsProperties.setTrustedIssuers(java.util.List.of("backbone-rest"));
        String token = signedToken("backbone-rest", "mercury");

        assertThat(sessionJwtService.getVerifiedUid(token)).isPresent();

        verify(backboneValidator).assertActive(token);
    }

    @Test
    @DisplayName("a token backbone-rest reports inactive (logged out) is rejected")
    void getTokenClaims_backboneReportsInactive_rejected() throws CertificateSecurityException {
        claimsProperties.setTrustedIssuers(java.util.List.of("backbone-rest"));
        String token = signedToken("backbone-rest", "mercury");
        doThrow(new CertificateSecurityException("Session token is no longer active in backbone-rest"))
                .when(backboneValidator).assertActive(token);

        assertThatThrownBy(() -> sessionJwtService.getTokenClaims(token))
                .isInstanceOf(CertificateSecurityException.class);
        assertThat(sessionJwtService.getVerifiedUid(token)).isEmpty();
    }

    @Test
    @DisplayName("backbone validation can be switched off")
    void getTokenClaims_backboneValidationDisabled_skipsBackbone() throws CertificateSecurityException {
        claimsProperties.setTrustedIssuers(java.util.List.of("backbone-rest"));
        claimsProperties.setValidateWithBackbone(false);

        sessionJwtService.getTokenClaims(signedToken("backbone-rest", "mercury"));

        verify(backboneValidator, never()).assertActive(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("revokeToken puts the verified token's jti on the denylist until its exp")
    void revokeToken_addsJtiUntilExp() throws CertificateSecurityException {
        String token = sessionJwtService.generateSessionToken("alice", null);
        Claims claims = sessionJwtService.getTokenClaims(token);

        sessionJwtService.revokeToken(token);

        verify(revocationService).revoke(claims.getId(), claims.getExpiration().toInstant());
    }

    @Test
    @DisplayName("revokeToken refuses an untrusted token and never touches the denylist")
    void revokeToken_untrusted_throws() {
        assertThatThrownBy(() -> sessionJwtService.revokeToken(signedToken("someone-else", "mercury")))
                .isInstanceOf(CertificateSecurityException.class);
        verify(revocationService, never()).revoke(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("revokeToken refuses a token with no jti")
    void revokeToken_noJti_throws() {
        String noJti = Jwts.builder()
                .claims(Map.of("uid", UUID.randomUUID().toString(), "type", "session-token"))
                .issuer("mercury").audience().add("mercury").and()
                .subject("alice")
                .expiration(java.util.Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        assertThatThrownBy(() -> sessionJwtService.revokeToken(noJti))
                .isInstanceOf(CertificateSecurityException.class)
                .hasMessageContaining("jti");
    }

    @Test
    @DisplayName("iss and aud are mandatory by default")
    void claimsProperties_strictByDefault() {
        var defaults = new SessionJwtClaimsProperties();

        assertThat(defaults.isAllowMissingClaims()).isFalse();
        assertThat(defaults.isValidateWithBackbone()).isTrue();
    }
}
