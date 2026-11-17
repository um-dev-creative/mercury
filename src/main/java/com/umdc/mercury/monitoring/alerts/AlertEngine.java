package com.umdc.mercury.monitoring.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * One evaluation pass over every enabled rule: an alert is raised when a rule is breached and none fires for it, kept up to date while it keeps
 * being breached, and resolved when it stops. A failure in one rule never stops the others. A second instance racing to raise the same alert is
 * stopped by the unique index of firing alerts, so nobody is notified twice.
 */
@Service
public class AlertEngine {

    private static final Logger logger = LoggerFactory.getLogger(AlertEngine.class);

    private final AlertRuleRepository rules;
    private final AlertEventRepository events;
    private final AlertEvaluator evaluator;
    private final AlertNotifier notifier;
    private final TransactionTemplate transaction;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AlertEngine(AlertRuleRepository rules, AlertEventRepository events, AlertEvaluator evaluator, AlertNotifier notifier,
                       PlatformTransactionManager transactionManager) {
        this(rules, events, evaluator, notifier, transactionManager, Clock.systemUTC());
    }

    AlertEngine(AlertRuleRepository rules, AlertEventRepository events, AlertEvaluator evaluator, AlertNotifier notifier,
                PlatformTransactionManager transactionManager, Clock clock) {
        this.rules = rules;
        this.events = events;
        this.evaluator = evaluator;
        this.notifier = notifier;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /** Evaluates every enabled rule once; returns how many alerts were raised. */
    public int evaluateAll() {
        int raised = 0;
        var now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        for (var rule : rules.findByEnabledTrue()) {
            try {
                if (evaluate(rule, now)) {
                    raised++;
                }
            } catch (RuntimeException e) {
                logger.warn("Could not evaluate alert rule {}: {}", rule.getId(), e.getClass().getSimpleName());
            }
        }
        return raised;
    }

    private boolean evaluate(AlertRuleEntity rule, LocalDateTime now) {
        var reading = evaluator.read(rule, now);
        var value = BigDecimal.valueOf(reading.value()).setScale(2, RoundingMode.HALF_UP);
        var open = events.findFirstByRuleIdAndStatus(rule.getId(), AlertEventEntity.FIRING);
        if (reading.breached() && open.isEmpty()) {
            return raise(rule, reading, value, now);
        }
        if (open.isPresent()) {
            var alert = open.get();
            if (reading.breached()) {
                update(alert, value, reading.detail(), null);
            } else {
                update(alert, value, reading.detail(), now);
                logger.info("Alert {} of rule {} resolved", alert.getId(), rule.getId());
            }
        }
        return false;
    }

    private boolean raise(AlertRuleEntity rule, AlertEvaluator.Reading reading, BigDecimal value, LocalDateTime now) {
        var alert = new AlertEventEntity();
        alert.setId(UUID.randomUUID());
        alert.setRuleId(rule.getId());
        alert.setApplicationId(rule.getApplicationId());
        alert.setType(rule.getType());
        alert.setStatus(AlertEventEntity.FIRING);
        alert.setValue(value);
        alert.setDetail(reading.detail());
        alert.setStartedAt(now);
        try {
            transaction.executeWithoutResult(status -> events.saveAndFlush(alert));
        } catch (DataIntegrityViolationException e) {
            logger.debug("Alert of rule {} was already raised by another instance", rule.getId());
            return false;
        }
        logger.warn("Alert raised: rule {} ({}) of application {}", rule.getId(), rule.getType(), rule.getApplicationId());
        notifier.notifyFiring(rule, alert);
        return true;
    }

    private void update(AlertEventEntity alert, BigDecimal value, String detail, LocalDateTime resolvedAt) {
        alert.setValue(value);
        alert.setDetail(detail);
        if (resolvedAt != null) {
            alert.setStatus(AlertEventEntity.RESOLVED);
            alert.setResolvedAt(resolvedAt);
        }
        transaction.executeWithoutResult(status -> events.save(alert));
    }

    /** Deletes the resolved alerts older than the retention period. */
    public int purgeResolvedBefore(LocalDateTime before) {
        return java.util.Optional.ofNullable(transaction.execute(status -> events.deleteResolvedBefore(before))).orElse(0);
    }
}
