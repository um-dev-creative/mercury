package com.umdc.mercury.security;

import com.umdc.mercury.client.BackboneSessionClient;
import com.umdc.security.exception.CertificateSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asks backbone-rest ({@code GET /api/v1/session/validate}) whether a session-token it issued is
 * still active. Backbone keeps its own {@code jti} deny-list, so a user who logged out there would
 * otherwise stay authenticated in Mercury until the token's {@code exp}.
 * <p>
 * Only <em>positive</em> answers are cached (by SHA-256 of the token, never the token itself), for
 * {@link SessionJwtClaimsProperties#getBackboneValidationCacheTtl()}: that TTL is the longest a
 * logout can take to be reflected, and it bounds the extra remote call to one per token per TTL.
 * A negative answer or any failure (backbone unreachable, 5xx) rejects the token — fails closed.
 * </p>
 */
@Component
public class BackboneSessionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackboneSessionValidator.class);
    private static final int SWEEP_THRESHOLD = 10_000;

    private final BackboneSessionClient backboneSessionClient;
    private final SessionJwtClaimsProperties properties;
    private final Clock clock;
    private final Map<String, Instant> activeUntil = new ConcurrentHashMap<>();

    @Autowired
    public BackboneSessionValidator(BackboneSessionClient backboneSessionClient, SessionJwtClaimsProperties properties) {
        this(backboneSessionClient, properties, Clock.systemUTC());
    }

    BackboneSessionValidator(BackboneSessionClient backboneSessionClient, SessionJwtClaimsProperties properties,
                             Clock clock) {
        this.backboneSessionClient = backboneSessionClient;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @throws CertificateSecurityException if backbone-rest reports the token inactive (revoked,
     *                                      wrong type, expired) or cannot be asked
     */
    public void assertActive(String token) throws CertificateSecurityException {
        String key = fingerprint(token);
        Instant now = clock.instant();
        Instant cachedUntil = activeUntil.get(key);
        if (cachedUntil != null && now.isBefore(cachedUntil)) {
            return;
        }
        boolean active;
        try {
            active = Boolean.TRUE.equals(backboneSessionClient.validate(token));
        } catch (RuntimeException e) {
            activeUntil.remove(key);
            LOGGER.warn("backbone-rest could not validate a session token: {}", e.getClass().getSimpleName());
            throw new CertificateSecurityException("Unable to verify session token with backbone-rest", e);
        }
        if (!active) {
            activeUntil.remove(key);
            throw new CertificateSecurityException("Session token is no longer active in backbone-rest");
        }
        if (activeUntil.size() >= SWEEP_THRESHOLD) {
            activeUntil.values().removeIf(until -> !now.isBefore(until));
        }
        activeUntil.put(key, now.plus(properties.getBackboneValidationCacheTtl()));
    }

    private static String fingerprint(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandatory on every JVM", e);
        }
    }
}
