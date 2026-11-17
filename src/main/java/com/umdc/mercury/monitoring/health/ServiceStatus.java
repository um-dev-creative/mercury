package com.umdc.mercury.monitoring.health;

/** State of a service as the monitoring views show it. */
public enum ServiceStatus {
    /** Answers and works. */
    UP,
    /** Answers but something is wrong (a quota, a rate limit, a slow reply). */
    DEGRADED,
    /** Does not answer or refuses us. */
    DOWN,
    /** Switched off or not set up in this environment: not a failure. */
    NOT_CONFIGURED,
    /** Not checked yet. */
    UNKNOWN;

    /** Whether this state is an outage (degraded or down). */
    public boolean isOutage() {
        return this == DEGRADED || this == DOWN;
    }
}
