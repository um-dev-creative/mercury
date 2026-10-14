package com.umdc.mercury.security;

import com.umdc.commons.constants.keys.AuthKey;
import com.umdc.security.exception.CertificateSecurityException;
import com.umdc.security.jwt.JwtConfigProperties;
import com.umdc.security.service.SessionJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Service class for handling JWT operations related to sessions.
 */
@Service
public class SessionJwtServiceImpl implements SessionJwtService {

    private final JwtConfigProperties jwtConfigProperties;
    private final SessionJwtClaimsProperties claimsProperties;
    private final TokenRevocationService tokenRevocationService;
    private final BackboneSessionValidator backboneSessionValidator;
    private final SecretKey key;
    public static final String USER_ID = "uid";

    /**
     * Constructor to initialize SessionJwtService with JwtConfigProperties.
     *
     * @param jwtConfigProperties    the configuration properties for JWT
     * @param claimsProperties       the expected {@code iss}/{@code aud} claims
     * @param tokenRevocationService the {@code jti} denylist
     * @param backboneSessionValidator checks tokens backbone-rest issued against its own deny-list
     */
    public SessionJwtServiceImpl(JwtConfigProperties jwtConfigProperties,
                                 SessionJwtClaimsProperties claimsProperties,
                                 TokenRevocationService tokenRevocationService,
                                 BackboneSessionValidator backboneSessionValidator) {
        this.jwtConfigProperties = jwtConfigProperties;
        this.claimsProperties = claimsProperties;
        this.tokenRevocationService = tokenRevocationService;
        this.backboneSessionValidator = backboneSessionValidator;
        this.key = generateKey();
    }

    /**
     * Generates a session token for the given username.
     *
     * @param username   the username
     * @param parameters the parameters
     * @return the generated session token
     */
    public String generateSessionToken(String username, Map<String, String> parameters) {
        Instant now = Instant.now();
        Map<String, Object> claims = new ConcurrentHashMap<>();
        // Optional — set before required claims so callers cannot override security-sensitive fields
        if (Objects.nonNull(parameters) && !parameters.isEmpty()) {
            claims.putAll(parameters);
        }
        // Drop caller-supplied iss/aud: the builder below must be their only source, otherwise a
        // caller-supplied aud would be merged into (not replaced by) the configured audience
        claims.remove("iss");
        claims.remove("aud");
        // Required
        claims.put(AuthKey.JTI.value, UUID.randomUUID().toString());
        claims.put(AuthKey.TYPE.value, SESSION_TOKEN_KEY);
        claims.put(AuthKey.IAT.value, now.getEpochSecond());
        claims.put("exp", now.plusMillis(jwtConfigProperties.getExpirationMs()).getEpochSecond());

        return Jwts.builder()
                .claims(claims)
                .issuer(claimsProperties.getIssuer())
                .audience().add(claimsProperties.getAudience()).and()
                .subject(username)
                .signWith(key)
                .compact();
    }

    /**
     * Retrieves the claims from the given token, after verifying its HS256 signature and
     * expiration, that it is a {@code type=session-token} (not e.g. a refresh token), that its
     * {@code iss}/{@code aud} match this service's configured values (see
     * {@link SessionJwtClaimsProperties}), that its {@code jti} has not been revoked here, and, for
     * a token backbone-rest issued, that backbone-rest itself still considers it active.
     * This is the single trust gate: {@code SessionJwtInterceptor} (via the default
     * {@code isValid}) and {@link #getVerifiedUid(String)} both go through it. The cause is
     * never surfaced to callers beyond the exception message, so the HTTP layer can answer
     * generically.
     *
     * @param token the JWT token
     * @return the claims contained in the token
     * @throws CertificateSecurityException if the token is malformed, tampered, expired, issued
     *                                      by or for someone else, revoked, or the denylist
     *                                      cannot be consulted (fails closed)
     */
    @Override
    public Claims getTokenClaims(String token) throws CertificateSecurityException {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new CertificateSecurityException(e.getMessage(), e);
        }
        // Backbone signs its 7-day refresh tokens (type=refresh-token, also carrying uid) with the
        // same key: only a session-token may stand in for one here
        if (!SESSION_TOKEN_KEY.equals(claims.get(AuthKey.TYPE.value))) {
            throw new CertificateSecurityException("Token is not a session token");
        }
        validateIssuerAndAudience(claims);
        try {
            if (tokenRevocationService.isRevoked(claims.getId())) {
                throw new CertificateSecurityException("Session token has been revoked");
            }
        } catch (DataAccessException e) {
            throw new CertificateSecurityException("Unable to verify session token revocation status", e);
        }
        // A token Mercury did not mint carries backbone-rest's own deny-list: honour a logout there
        if (claimsProperties.isValidateWithBackbone() && !claimsProperties.getIssuer().equals(claims.getIssuer())) {
            backboneSessionValidator.assertActive(token);
        }
        return claims;
    }

    private void validateIssuerAndAudience(Claims claims) throws CertificateSecurityException {
        String issuer = claims.getIssuer();
        if (Objects.isNull(issuer) ? !claimsProperties.isAllowMissingClaims()
                : !claimsProperties.acceptedIssuers().contains(issuer)) {
            throw new CertificateSecurityException("Session token issuer is not accepted");
        }
        Set<String> audience = claims.getAudience();
        boolean audienceMissing = Objects.isNull(audience) || audience.isEmpty();
        if (audienceMissing ? !claimsProperties.isAllowMissingClaims()
                : audience.stream().noneMatch(claimsProperties.acceptedAudiences()::contains)) {
            throw new CertificateSecurityException("Session token audience is not accepted");
        }
    }

    /**
     * Retrieves the username from the given token.
     *
     * @param token the JWT token
     * @return the username contained in the token
     * @throws IllegalArgumentException if the token is not trusted (see {@link #getTokenClaims})
     */
    public String getUsernameFromToken(String token) {
        try {
            return getTokenClaims(token).getSubject();
        } catch (CertificateSecurityException e) {
            throw new IllegalArgumentException("Invalid session token", e);
        }
    }

    /**
     * Revokes {@code token} (after verifying it) by adding its {@code jti} to the denylist until
     * its own {@code exp}. Only affects this service: it does not log the user out of backbone-rest.
     *
     * @throws CertificateSecurityException if the token is not trusted or has no {@code jti}
     */
    public void revokeToken(String token) throws CertificateSecurityException {
        Claims claims = getTokenClaims(token);
        String jti = claims.getId();
        if (Objects.isNull(jti) || jti.isBlank()) {
            throw new CertificateSecurityException("Session token has no jti and cannot be revoked");
        }
        tokenRevocationService.revoke(jti, claims.getExpiration().toInstant());
    }

    /**
     * Resolves the {@code uid} claim from {@code token} after verifying its signature and
     * expiration against the shared HS256 secret, returning {@link Optional#empty()} for
     * any missing, expired, tampered, or malformed token — including one that never went
     * through this app's own {@code SessionJwtInterceptor} (e.g. a caller-supplied token on
     * an {@code @SkipSessionValidation} endpoint, or a token minted by backbone-rest under
     * the same shared secret). Callers that need a caller-supplied identity claim for an
     * authorization decision must resolve it here, never via the unverified
     * {@code com.umdc.commons.util.JwtUtil.getUidFromToken}, which only base64-decodes the
     * payload without checking the signature at all.
     *
     * @param token the raw session token to verify
     * @return the verified {@code uid} claim, or empty if the token could not be trusted
     */
    public Optional<UUID> getVerifiedUid(String token) {
        if (Objects.isNull(token) || token.isBlank()) {
            return Optional.empty();
        }
        try {
            String uid = getTokenClaims(token).get(USER_ID, String.class);
            return Objects.isNull(uid) ? Optional.empty() : Optional.of(UUID.fromString(uid));
        } catch (CertificateSecurityException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Generates a SecretKey from the JWT configuration properties.
     *
     * @return the generated SecretKey
     */
    private SecretKey generateKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfigProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
