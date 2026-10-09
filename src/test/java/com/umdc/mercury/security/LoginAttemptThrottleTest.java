package com.umdc.mercury.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginAttemptThrottle unit tests")
class LoginAttemptThrottleTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-07T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private LoginAttemptThrottle throttle;
    private final String key = LoginAttemptThrottle.key("Directory-Backend", "10.0.0.7");

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        var properties = new LoginThrottleProperties();
        properties.setMaxAttempts(3);
        properties.setWindow(Duration.ofMinutes(10));
        throttle = new LoginAttemptThrottle(properties, clock);
    }

    @Test
    @DisplayName("is not blocked until maxAttempts failures are recorded")
    void blocksAtMaxAttempts() {
        throttle.recordFailure(key);
        throttle.recordFailure(key);
        assertThat(throttle.isBlocked(key)).isFalse();

        throttle.recordFailure(key);

        assertThat(throttle.isBlocked(key)).isTrue();
        assertThat(throttle.retryAfter(key)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("retryAfter shrinks as the window runs out and the block lifts when it ends")
    void blockLiftsAfterWindow() {
        for (int i = 0; i < 3; i++) {
            throttle.recordFailure(key);
        }
        clock.advance(Duration.ofMinutes(4));
        assertThat(throttle.retryAfter(key)).isEqualTo(Duration.ofMinutes(6));

        clock.advance(Duration.ofMinutes(6));

        assertThat(throttle.isBlocked(key)).isFalse();
        assertThat(throttle.retryAfter(key)).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("an expired window starts counting from scratch")
    void expiredWindowRestarts() {
        throttle.recordFailure(key);
        throttle.recordFailure(key);
        clock.advance(Duration.ofMinutes(11));

        throttle.recordFailure(key);

        assertThat(throttle.isBlocked(key)).isFalse();
    }

    @Test
    @DisplayName("success clears the counter")
    void successClears() {
        for (int i = 0; i < 3; i++) {
            throttle.recordFailure(key);
        }
        throttle.recordSuccess(key);

        assertThat(throttle.isBlocked(key)).isFalse();
    }

    @Test
    @DisplayName("keys are case-insensitive on alias and distinct per address")
    void keyShape() {
        assertThat(LoginAttemptThrottle.key("ALIAS", "1.1.1.1")).isEqualTo(LoginAttemptThrottle.key("alias", "1.1.1.1"));
        assertThat(LoginAttemptThrottle.key("alias", "1.1.1.1")).isNotEqualTo(LoginAttemptThrottle.key("alias", "2.2.2.2"));
        assertThat(throttle.isBlocked("unknown")).isFalse();
        assertThat(throttle.retryAfter("unknown")).isEqualTo(Duration.ZERO);
    }
}
