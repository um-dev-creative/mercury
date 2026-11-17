package com.umdc.mercury.monitoring.alerts;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.mail.MailRecipientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Tells the people a rule names that an alert fired. It goes through {@link MailRecipientPolicy}, so a non-production environment sends it to the
 * test list and not to the real addresses. Best effort: a failure (the mail server may be the very thing that is down) is logged and never stops
 * the evaluation. Nothing but the fixed alert sentence is sent.
 */
@Component
public class AlertNotifier {

    private static final Logger logger = LoggerFactory.getLogger(AlertNotifier.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MailRecipientPolicy policy;
    private final String from;

    public AlertNotifier(ObjectProvider<JavaMailSender> mailSender, MailRecipientPolicy policy,
                         @Value("${umdc.monitoring.alerts.from:${spring.mail.username:}}") String from) {
        this.mailSender = mailSender;
        this.policy = policy;
        this.from = from;
    }

    public void notifyFiring(AlertRuleEntity rule, AlertEventEntity alert) {
        var requested = AlertRuleService.splitEmails(rule.getNotifyEmails());
        if (requested.isEmpty()) {
            return;
        }
        var sender = mailSender.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) {
            logger.warn("Alert {} fired but no mail sender or sender address is configured: nobody was notified", alert.getId());
            return;
        }
        try {
            var to = policy.resolveTo(requested.stream().map(a -> new EmailContact(a, a, null)).toList());
            if (to.isEmpty()) {
                return;
            }
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to.toArray(String[]::new));
            message.setSubject("[Mercury] Alert: " + rule.getType() + " in application " + rule.getApplicationId());
            message.setText(alert.getDetail() + "\n\nStarted at " + alert.getStartedAt() + " UTC. Rule " + rule.getId() + ".");
            sender.send(message);
            logger.info("Alert {} notified to {} address(es)", alert.getId(), to.size());
        } catch (MailException | IllegalArgumentException e) {
            logger.warn("Alert {} could not be notified by e-mail: {}", alert.getId(), e.getClass().getSimpleName());
        }
    }
}
