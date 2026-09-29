package com.umdc.mercury.security;

import com.umdc.commons.constants.keys.AuthKey;
import com.umdc.security.exception.CertificateSecurityException;
import com.umdc.security.jwt.JwtConfigProperties;
import com.umdc.security.service.SessionJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Service class for handling JWT operations related to sessions.
 */
@Service
public class SessionJwtServiceImpl implements SessionJwtService {

    private final JwtConfigProperties jwtConfigProperties;
    private final SecretKey key;
    public static final String USER_ID = "uid";

    /**
     * Constructor to initialize SessionJwtService with JwtConfigProperties.
     *
     * @param jwtConfigProperties the configuration properties for JWT
     */
    public SessionJwtServiceImpl(JwtConfigProperties jwtConfigProperties) {
        this.jwtConfigProperties = jwtConfigProperties;
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
        // Required
        claims.put(AuthKey.JTI.value, UUID.randomUUID().toString());
        claims.put(AuthKey.TYPE.value, SESSION_TOKEN_KEY);
        claims.put(AuthKey.IAT.value, now.getEpochSecond());
        claims.put("exp", now.plusMillis(jwtConfigProperties.getExpirationMs()).getEpochSecond());

        return Jwts.builder().claims(claims).subject(username).signWith(key).compact();
    }

    /**
     * Retrieves the claims from the given token.
     *
     * @param token the JWT token
     * @return the claims contained in the token
     */
    @Override
    public Claims getTokenClaims(String token) throws CertificateSecurityException {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new CertificateSecurityException(e.getMessage(), e);
        }
        return claims;
    }

    /**
     * Retrieves the username from the given token.
     *
     * @param token the JWT token
     * @return the username contained in the token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
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
