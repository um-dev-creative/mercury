package com.umdc.mercury.monitoring.health;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.net.ConnectException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HealthCause, ServiceStatus and ProbeResult")
class HealthCauseTest {

    private static MailSendException failure(String smtpReply) {
        return new MailSendException("Mail server connection failed", new MessagingException(smtpReply));
    }

    @Test
    @DisplayName("the SMTP replies are told apart: login, quota, rate limit, blocked sign-in")
    void classifiesSmtpReplies() {
        assertEquals(HealthCause.AUTH_REFUSED, HealthCause.fromSmtpFailure(failure("535-5.7.8 Username and Password not accepted")));
        assertEquals(HealthCause.AUTH_REFUSED, HealthCause.fromSmtpFailure(new MailSendException("x", new AuthenticationFailedException("no"))));
        assertEquals(HealthCause.QUOTA_EXCEEDED, HealthCause.fromSmtpFailure(failure("550 5.4.5 Daily user sending limit exceeded")));
        assertEquals(HealthCause.QUOTA_EXCEEDED, HealthCause.fromSmtpFailure(failure("over quota")));
        assertEquals(HealthCause.RATE_LIMITED, HealthCause.fromSmtpFailure(failure("421 4.7.0 Try again later")));
        assertEquals(HealthCause.RATE_LIMITED, HealthCause.fromSmtpFailure(failure("454 temporary")));
        assertEquals(HealthCause.SIGNIN_BLOCKED, HealthCause.fromSmtpFailure(failure("534-5.7.9 Application-specific password required")));
    }

    @Test
    @DisplayName("a timeout and a connection problem are told apart, and anything else is a failed check")
    void classifiesNetworkFailures() {
        assertEquals(HealthCause.TIMEOUT, HealthCause.fromSmtpFailure(new MailSendException("x", new java.net.SocketTimeoutException("Read timed out"))));
        assertEquals(HealthCause.CONNECTION_FAILED, HealthCause.fromSmtpFailure(new MailSendException("x", new ConnectException("refused"))));
        assertEquals(HealthCause.CONNECTION_FAILED, HealthCause.fromSmtpFailure(new MailSendException("x", new UnknownHostException("smtp.example"))));
        assertEquals(HealthCause.CHECK_ERROR, HealthCause.fromSmtpFailure(new MailSendException("boom")));
        assertEquals(HealthCause.CHECK_ERROR, HealthCause.fromSmtpFailure(new RuntimeException((String) null)));
    }

    @Test
    @DisplayName("a cause that wraps itself does not loop")
    void selfReferencingCause() {
        var loop = new RuntimeException("535 loop");
        var other = new RuntimeException("other");
        loop.initCause(other);
        other.initCause(loop);

        assertEquals(HealthCause.AUTH_REFUSED, HealthCause.fromSmtpFailure(loop));
    }

    @Test
    @DisplayName("every cause has a fixed sentence and a default state; only DOWN and DEGRADED are outages")
    void defaults() {
        for (HealthCause cause : HealthCause.values()) {
            assertFalse(cause.text().isBlank());
            assertEquals(cause.defaultStatus(), ProbeResult.failed(cause, 5).status());
        }
        assertTrue(ServiceStatus.DOWN.isOutage());
        assertTrue(ServiceStatus.DEGRADED.isOutage());
        assertFalse(ServiceStatus.UP.isOutage());
        assertFalse(ServiceStatus.NOT_CONFIGURED.isOutage());
        assertFalse(ServiceStatus.UNKNOWN.isOutage());
    }

    @Test
    @DisplayName("ProbeResult builds the three shapes: up, failed and not configured")
    void probeResults() {
        var up = ProbeResult.up(12);
        assertEquals(ServiceStatus.UP, up.status());
        assertEquals(12, up.latencyMs());
        assertEquals(null, up.cause());
        var off = ProbeResult.notConfigured(HealthCause.LISTENERS_OFF);
        assertEquals(ServiceStatus.NOT_CONFIGURED, off.status());
        assertEquals(-1, off.latencyMs());
        assertEquals(ServiceStatus.DOWN, ProbeResult.failed(HealthCause.TIMEOUT, 5000).status());
    }
}
