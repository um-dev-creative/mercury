package com.umdc.mercury.security;

import com.umdc.security.jwt.JwtConfigProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionJwtServiceImpl.getVerifiedUid unit tests")
class SessionJwtServiceImplTest {

    private static final String SECRET =
            "bG9jYWxEZXZTZWNyZXRLZXlGb3JNZXJjdXJ5Tm90Rm9yUHJvZHVjdGlvblVzZTEyMzQ1Ng==";

    private SessionJwtServiceImpl sessionJwtService;

    @BeforeEach
    void setUp() {
        var properties = new JwtConfigProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(60_000L);
        sessionJwtService = new SessionJwtServiceImpl(properties);
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
}
