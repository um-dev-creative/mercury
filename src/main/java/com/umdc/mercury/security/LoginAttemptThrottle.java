package com.umdc.mercury.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, per-instance failed-login counter keyed by alias + caller address (see
 * {@link LoginThrottleProperties}). Keyed by both so an attacker elsewhere cannot lock a legitimate
 * client out of its own alias. Behind a proxy that hides the caller's address every client shares one
 * address; size {@code maxAttempts} accordingly. Per-instance: with N replicas the effective limit is
 * up to N times higher.
 */
@Component
public class LoginAttemptThrottle {

    private static final int SWEEP_THRESHOLD = 10_000;

    private record Window(Instant firstFailure, int failures) {
    }

    private final LoginThrottleProperties properties;
    private final Clock clock;
    private final Map<String, Window> failures = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptThrottle(LoginThrottleProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginAttemptThrottle(LoginThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public static String key(String alias, String remoteAddress) {
        return String.valueOf(alias).toLowerCase(java.util.Locale.ROOT) + '|' + remoteAddress;
    }

    public boolean isBlocked(String key) {
        Window window = failures.get(key);
        if (window == null) {
            return false;
        }
        if (expired(window)) {
            failures.remove(key, window);
            return false;
        }
        return window.failures() >= properties.getMaxAttempts();
    }

    /** Time until a blocked key may try again; zero if it is not blocked. */
    public Duration retryAfter(String key) {
        Window window = failures.get(key);
        if (window == null || !isBlocked(key)) {
            return Duration.ZERO;
        }
        return Duration.between(clock.instant(), window.firstFailure().plus(properties.getWindow()));
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        if (failures.size() >= SWEEP_THRESHOLD) {
            failures.values().removeIf(this::expired);
        }
        failures.merge(key, new Window(now, 1),
                (old, fresh) -> expired(old) ? fresh : new Window(old.firstFailure(), old.failures() + 1));
    }

    public void recordSuccess(String key) {
        failures.remove(key);
    }

    private boolean expired(Window window) {
        return !clock.instant().isBefore(window.firstFailure().plus(properties.getWindow()));
    }
}
