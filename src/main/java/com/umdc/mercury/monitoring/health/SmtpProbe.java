package com.umdc.mercury.monitoring.health;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * SMTP: opens a connection and signs in, without sending anything. It runs rarely (every 5 minutes by default) because a mail provider can
 * block an account that signs in too often; the real send outcomes report the rest ({@link ServiceHealthRegistry#reportFailure}).
 */
@Component
public class SmtpProbe implements ServiceProbe {

    private final MailSender mailSender;
    private final Duration interval;

    public SmtpProbe(MailSender mailSender, @Value("${umdc.monitoring.health.smtp-interval-seconds:300}") long intervalSeconds) {
        this.mailSender = mailSender;
        this.interval = Duration.ofSeconds(Math.max(1, intervalSeconds));
    }

    @Override
    public String service() {
        return "SMTP";
    }

    @Override
    public Duration interval() {
        return interval;
    }

    @Override
    public ProbeResult check() {
        if (!(mailSender instanceof JavaMailSenderImpl sender)) {
            return ProbeResult.notConfigured(HealthCause.NOT_CONFIGURED);
        }
        var start = System.nanoTime();
        try {
            sender.testConnection();
            return ProbeResult.up((System.nanoTime() - start) / 1_000_000);
        } catch (MessagingException e) {
            return ProbeResult.failed(HealthCause.fromSmtpFailure(e), (System.nanoTime() - start) / 1_000_000);
        }
    }
}
