package com.umdc.mercury.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Brute-force protection for {@code POST /api/v1/auth/token}, bound at
 * {@code umdc.security.login-throttle.*}: after {@code maxAttempts} failed logins for the same
 * alias from the same address within {@code window}, further attempts get {@code 429} until the
 * window since the first failure elapses. A successful login clears the counter.
 */
@Component
@ConfigurationProperties(prefix = "umdc.security.login-throttle")
public class LoginThrottleProperties {

    private int maxAttempts = 5;
    private Duration window = Duration.ofMinutes(15);

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
