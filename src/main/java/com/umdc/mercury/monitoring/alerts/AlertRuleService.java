package com.umdc.mercury.monitoring.alerts;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.monitoring.MonitoringRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * The alert rules of an application and the alerts they raised. Every operation is scoped to one application: a rule of another is "not found".
 * A rule is validated before it is stored, so the evaluator only ever sees sane values.
 */
@Service
public class AlertRuleService {

    static final int MAX_RULES = 20;
    private static final int MAX_ALERTS = 200;

    private final AlertRuleRepository rules;
    private final AlertEventRepository events;
    private final Clock clock;
    private final AlertRuleValidator validator;

    @org.springframework.beans.factory.annotation.Autowired
    public AlertRuleService(AlertRuleRepository rules, AlertEventRepository events, @Value("${umdc.monitoring.alerts.max-emails:5}") int maxEmails) {
        this(rules, events, Clock.systemUTC(), maxEmails);
    }

    AlertRuleService(AlertRuleRepository rules, AlertEventRepository events, Clock clock, int maxEmails) {
        this.rules = rules;
        this.events = events;
        this.clock = clock;
        this.validator = new AlertRuleValidator(maxEmails);
    }

    @Transactional(readOnly = true)
    public MonitoringTO.AlertRulesResponse list(UUID applicationId) {
        return new MonitoringTO.AlertRulesResponse(rules.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream().map(AlertRuleService::toTo).toList());
    }

    @Transactional
    public MonitoringTO.AlertRule create(UUID applicationId, MonitoringTO.AlertRuleRequest request) {
        if (rules.countByApplicationId(applicationId) >= MAX_RULES) {
            throw new IllegalArgumentException("An application can have at most " + MAX_RULES + " alert rules");
        }
        var rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setApplicationId(applicationId);
        rule.setCreatedAt(now());
        validator.apply(rule, request, now());
        return toTo(rules.save(rule));
    }

    /** @return the updated rule, or {@code null} when the application has no such rule */
    @Transactional
    public MonitoringTO.AlertRule update(UUID applicationId, UUID ruleId, MonitoringTO.AlertRuleRequest request) {
        return rules.findById(ruleId).filter(r -> applicationId.equals(r.getApplicationId())).map(rule -> {
            validator.apply(rule, request, now());
            return toTo(rules.save(rule));
        }).orElse(null);
    }

    /** @return whether the rule existed (and was deleted with its alerts) */
    @Transactional
    public boolean delete(UUID applicationId, UUID ruleId) {
        var rule = rules.findById(ruleId).filter(r -> applicationId.equals(r.getApplicationId()));
        rule.ifPresent(rules::delete);
        return rule.isPresent();
    }

    @Transactional(readOnly = true)
    public MonitoringTO.AlertsResponse alerts(UUID applicationId, String range) {
        var selected = MonitoringRange.parse(range);
        var list = events.recent(applicationId, selected.since(now()), PageRequest.of(0, MAX_ALERTS)).stream().map(AlertRuleService::toTo).toList();
        return new MonitoringTO.AlertsResponse(selected.code(), events.countByApplicationIdAndStatus(applicationId, AlertEventEntity.FIRING), list);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    static List<String> splitEmails(String stored) {
        return stored == null || stored.isBlank() ? List.of() : Arrays.stream(stored.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    static MonitoringTO.AlertRule toTo(AlertRuleEntity r) {
        return new MonitoringTO.AlertRule(r.getId(), r.getType(), r.getThreshold().doubleValue(), r.getWindowMinutes(), r.getMinVolume(), r.getService(),
                splitEmails(r.getNotifyEmails()), r.isEnabled(), r.getCreatedAt(), r.getUpdatedAt());
    }

    static MonitoringTO.AlertEvent toTo(AlertEventEntity e) {
        return new MonitoringTO.AlertEvent(e.getId(), e.getRuleId(), e.getType(), e.getStatus(), e.getValue().doubleValue(), e.getDetail(), e.getStartedAt(),
                e.getResolvedAt());
    }
}
