package com.umdc.mercury.security;

import com.umdc.mercury.client.BackboneSessionClient;
import com.umdc.security.exception.CertificateSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BackboneSessionValidator unit tests")
class BackboneSessionValidatorTest {

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

    private BackboneSessionClient client;
    private MutableClock clock;
    private BackboneSessionValidator validator;

    @BeforeEach
    void setUp() {
        client = mock(BackboneSessionClient.class);
        clock = new MutableClock();
        var properties = new SessionJwtClaimsProperties();
        properties.setBackboneValidationCacheTtl(Duration.ofSeconds(30));
        validator = new BackboneSessionValidator(client, properties, clock);
    }

    @Test
    @DisplayName("accepts a token backbone-rest reports active")
    void activeToken_accepted() {
        when(client.validate("t")).thenReturn(true);

        assertThatCode(() -> validator.assertActive("t")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a token backbone-rest reports inactive (revoked / logged out)")
    void inactiveToken_rejected() {
        when(client.validate("t")).thenReturn(false);

        assertThatThrownBy(() -> validator.assertActive("t"))
                .isInstanceOf(CertificateSecurityException.class)
                .hasMessageContaining("no longer active");
    }

    @Test
    @DisplayName("a null body from backbone-rest is treated as inactive")
    void nullBody_rejected() {
        when(client.validate("t")).thenReturn(null);

        assertThatThrownBy(() -> validator.assertActive("t")).isInstanceOf(CertificateSecurityException.class);
    }

    @Test
    @DisplayName("fails closed when backbone-rest cannot be reached")
    void backboneDown_failsClosed() {
        when(client.validate("t")).thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> validator.assertActive("t"))
                .isInstanceOf(CertificateSecurityException.class)
                .hasMessageContaining("Unable to verify");
    }

    @Test
    @DisplayName("a positive answer is cached for the TTL, then backbone-rest is asked again")
    void positiveAnswerCachedForTtl() throws CertificateSecurityException {
        when(client.validate("t")).thenReturn(true);

        validator.assertActive("t");
        validator.assertActive("t");
        verify(client, times(1)).validate("t");

        clock.advance(Duration.ofSeconds(31));
        validator.assertActive("t");
        verify(client, times(2)).validate("t");
    }

    @Test
    @DisplayName("a logout inside the cache window is reflected once the TTL lapses")
    void logoutReflectedAfterTtl() throws CertificateSecurityException {
        when(client.validate("t")).thenReturn(true, false);
        validator.assertActive("t");

        clock.advance(Duration.ofSeconds(31));

        assertThatThrownBy(() -> validator.assertActive("t")).isInstanceOf(CertificateSecurityException.class);
    }

    @Test
    @DisplayName("a negative answer is never cached: a re-activated token is re-checked every time")
    void negativeAnswerNotCached() {
        when(client.validate("t")).thenReturn(false, true);

        assertThatThrownBy(() -> validator.assertActive("t")).isInstanceOf(CertificateSecurityException.class);
        assertThatCode(() -> validator.assertActive("t")).doesNotThrowAnyException();
    }
}
