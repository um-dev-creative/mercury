package com.umdc.mercury.monitoring.health;

import java.util.Locale;

/**
 * Why a service is not well. The text is a fixed sentence on purpose: the monitoring views never carry a raw exception message, which can
 * hold a host, an address or part of a credential.
 */
public enum HealthCause {
    CONNECTION_FAILED("The service cannot be reached", ServiceStatus.DOWN),
    TIMEOUT("The service did not answer in time", ServiceStatus.DOWN),
    AUTH_REFUSED("The service refused our credentials", ServiceStatus.DOWN),
    SIGNIN_BLOCKED("The provider blocked the sign-in as suspicious", ServiceStatus.DOWN),
    HTTP_ERROR("The service answered with a server error", ServiceStatus.DOWN),
    QUOTA_EXCEEDED("The sending limit of the account was exceeded", ServiceStatus.DEGRADED),
    RATE_LIMITED("The service is rate limiting us", ServiceStatus.DEGRADED),
    SLOW("The service answers slowly", ServiceStatus.DEGRADED),
    CHECK_ERROR("The check itself failed", ServiceStatus.DEGRADED),
    NOT_CONFIGURED("Not configured in this environment", ServiceStatus.NOT_CONFIGURED),
    LISTENERS_OFF("Kafka listeners are switched off in this environment", ServiceStatus.NOT_CONFIGURED);

    private final String text;
    private final ServiceStatus defaultStatus;

    HealthCause(String text, ServiceStatus defaultStatus) {
        this.text = text;
        this.defaultStatus = defaultStatus;
    }

    public String text() {
        return text;
    }

    /** The state a service gets for this cause when nothing says otherwise. */
    public ServiceStatus defaultStatus() {
        return defaultStatus;
    }

    /**
     * Classifies a failure of the SMTP server from the text of the exception chain (the same replies {@code MessageProcessor} hints at):
     * 535 / authentication, 5.4.5 / quota, 421 / 454 / 4.7.0, 534 / 5.7.9, and connection problems.
     */
    public static HealthCause fromSmtpFailure(Throwable failure) {
        var detail = new StringBuilder();
        var seen = new java.util.ArrayList<Throwable>();
        for (Throwable t = failure; t != null && !seen.contains(t) && seen.size() < 8; t = t.getCause()) {
            seen.add(t);
            detail.append(t.getClass().getSimpleName()).append(' ').append(t.getMessage() == null ? "" : t.getMessage()).append(' ');
        }
        var text = detail.toString().toLowerCase(Locale.ROOT);
        if (text.contains("535") || text.contains("authenticationfailedexception")) {
            return AUTH_REFUSED;
        }
        if (text.contains("5.4.5") || text.contains("quota")) {
            return QUOTA_EXCEEDED;
        }
        if (text.contains("421") || text.contains("454") || text.contains("4.7.0")) {
            return RATE_LIMITED;
        }
        if (text.contains("534") || text.contains("5.7.9")) {
            return SIGNIN_BLOCKED;
        }
        if (text.contains("timed out") || text.contains("timeout")) {
            return TIMEOUT;
        }
        if (text.contains("connectexception") || text.contains("unknownhostexception") || text.contains("connection")) {
            return CONNECTION_FAILED;
        }
        return CHECK_ERROR;
    }
}
