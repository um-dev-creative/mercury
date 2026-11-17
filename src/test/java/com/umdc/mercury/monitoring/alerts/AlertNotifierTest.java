package com.umdc.mercury.monitoring.alerts;

import com.umdc.mercury.mail.MailRecipientPolicy;
import com.umdc.mercury.mail.MailRecipientProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertNotifier")
class AlertNotifierTest {

    @Mock
    private ObjectProvider<JavaMailSender> provider;
    @Mock
    private JavaMailSender sender;

    private AlertRuleEntity rule(String emails) {
        var rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setApplicationId(UUID.randomUUID());
        rule.setType("SERVICE_DOWN");
        rule.setNotifyEmails(emails);
        return rule;
    }

    private static AlertEventEntity alert() {
        var alert = new AlertEventEntity();
        alert.setId(UUID.randomUUID());
        alert.setDetail("SMTP DOWN for at least 10 min");
        alert.setStartedAt(LocalDateTime.of(2026, 11, 17, 8, 0));
        return alert;
    }

    @Test
    @DisplayName("sends one e-mail with the fixed alert sentence to the rule's addresses")
    void sends() {
        when(provider.getIfAvailable()).thenReturn(sender);
        var notifier = new AlertNotifier(provider, MailRecipientPolicy.passThrough(), "alerts@mercury.test");
        var rule = rule("a@x.com,b@x.com");

        notifier.notifyFiring(rule, alert());

        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals("alerts@mercury.test", message.getValue().getFrom());
        assertArrayEquals(new String[]{"a@x.com", "b@x.com"}, message.getValue().getTo());
        assertEquals("[Mercury] Alert: SERVICE_DOWN in application " + rule.getApplicationId(), message.getValue().getSubject());
        assertTrue(message.getValue().getText().contains("SMTP DOWN for at least 10 min"));
    }

    @Test
    @DisplayName("in a non-production environment the QA policy replaces the addresses with the test list")
    void honoursTheQaPolicy() {
        when(provider.getIfAvailable()).thenReturn(sender);
        var policy = new MailRecipientPolicy(new MailRecipientProperties(List.of("qa@mercury.test"), false), "qa");
        var notifier = new AlertNotifier(provider, policy, "alerts@mercury.test");

        notifier.notifyFiring(rule("real@customer.com"), alert());

        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertArrayEquals(new String[]{"qa@mercury.test"}, message.getValue().getTo());
    }

    @Test
    @DisplayName("a rule without addresses sends nothing and does not even look for a mail sender")
    void noAddresses() {
        new AlertNotifier(provider, MailRecipientPolicy.passThrough(), "alerts@mercury.test").notifyFiring(rule(null), alert());

        verifyNoInteractions(provider, sender);
    }

    @Test
    @DisplayName("without a mail sender or a sender address nobody is notified, and nothing throws")
    void notConfigured() {
        when(provider.getIfAvailable()).thenReturn(null, sender);

        assertDoesNotThrow(() -> new AlertNotifier(provider, MailRecipientPolicy.passThrough(), "alerts@mercury.test").notifyFiring(rule("a@x.com"), alert()));
        assertDoesNotThrow(() -> new AlertNotifier(provider, MailRecipientPolicy.passThrough(), " ").notifyFiring(rule("a@x.com"), alert()));
        assertDoesNotThrow(() -> new AlertNotifier(provider, MailRecipientPolicy.passThrough(), null).notifyFiring(rule("a@x.com"), alert()));
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("a mail failure (the mail server may be what is down) is swallowed")
    void mailFailureIsSwallowed() {
        when(provider.getIfAvailable()).thenReturn(sender);
        doThrow(new MailSendException("smtp down")).when(sender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> new AlertNotifier(provider, MailRecipientPolicy.passThrough(), "alerts@mercury.test").notifyFiring(rule("a@x.com"), alert()));
    }

    @Test
    @DisplayName("a stored address that is not valid is swallowed too")
    void invalidStoredAddress() {
        assertDoesNotThrow(() -> new AlertNotifier(provider, MailRecipientPolicy.passThrough(), "alerts@mercury.test").notifyFiring(rule(" , "), alert()));
    }
}
