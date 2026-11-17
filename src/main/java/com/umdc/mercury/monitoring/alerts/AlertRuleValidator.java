package com.umdc.mercury.monitoring.alerts;

import com.umdc.mercury.api.v1.to.MonitoringTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Checks an alert rule request and writes it onto the entity, so the evaluator only ever sees sane values. */
final class AlertRuleValidator {

    static final int MAX_EMAILS = 5;
    static final int DEFAULT_WINDOW = 15;
    /** The services the health registry knows. */
    static final Set<String> SERVICES = Set.of("POSTGRES", "MONGO", "KAFKA", "SMTP", "BACKBONE", "TELEGRAM");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@,;]{1,64}@[^\\s@,;]{1,255}$");

    private final int maxEmails;

    AlertRuleValidator(int maxEmails) {
        this.maxEmails = Math.max(0, Math.min(maxEmails, MAX_EMAILS));
    }

    void apply(AlertRuleEntity rule, MonitoringTO.AlertRuleRequest request, LocalDateTime now) {
        if (request == null || request.type() == null || request.type().isBlank()) {
            throw new IllegalArgumentException("The rule type is required: " + Arrays.toString(AlertType.values()));
        }
        AlertType type;
        try {
            type = AlertType.valueOf(request.type().strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown rule type '" + request.type() + "': use " + Arrays.toString(AlertType.values()), e);
        }
        int window = request.windowMinutes() == null ? DEFAULT_WINDOW : request.windowMinutes();
        if (window < 1 || window > 1440) {
            throw new IllegalArgumentException("windowMinutes must be between 1 and 1440");
        }
        int volume = request.minVolume() == null ? 0 : request.minVolume();
        if (volume < 0 || volume > 1_000_000) {
            throw new IllegalArgumentException("minVolume must be between 0 and 1000000");
        }
        rule.setType(type.name());
        rule.setWindowMinutes(window);
        rule.setMinVolume(type == AlertType.FAILURE_RATE ? volume : 0);
        rule.setThreshold(threshold(type, request.threshold()));
        rule.setService(service(type, request.service()));
        rule.setNotifyEmails(emails(request.notifyEmails()));
        rule.setEnabled(request.enabled() == null || request.enabled());
        rule.setUpdatedAt(now);
    }

    private static BigDecimal threshold(AlertType type, Double value) {
        if (type == AlertType.SERVICE_DOWN) {
            return BigDecimal.ONE;
        }
        if (value == null || value.isNaN() || value.isInfinite()) {
            throw new IllegalArgumentException("threshold is required for " + type);
        }
        var max = type == AlertType.FAILURE_RATE ? 100.0 : 1_000_000.0;
        if (value <= 0 || value > max) {
            throw new IllegalArgumentException("threshold must be above 0 and at most " + (type == AlertType.FAILURE_RATE ? "100 (percent)" : "1000000 (messages)"));
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String service(AlertType type, String service) {
        if (service == null || service.isBlank()) {
            return null;
        }
        if (type != AlertType.SERVICE_DOWN) {
            throw new IllegalArgumentException("service only applies to SERVICE_DOWN rules");
        }
        var name = service.strip().toUpperCase(Locale.ROOT);
        if (!SERVICES.contains(name)) {
            throw new IllegalArgumentException("Unknown service '" + service + "': use " + SERVICES.stream().sorted().toList());
        }
        return name;
    }

    private String emails(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        var clean = addresses.stream().filter(a -> a != null && !a.isBlank()).map(String::strip).distinct().toList();
        if (clean.size() > maxEmails) {
            throw new IllegalArgumentException("At most " + maxEmails + " e-mail addresses can be notified");
        }
        for (var address : clean) {
            if (!EMAIL.matcher(address).matches()) {
                throw new IllegalArgumentException("Invalid e-mail address in notifyEmails");
            }
        }
        return clean.isEmpty() ? null : String.join(",", clean);
    }
}
