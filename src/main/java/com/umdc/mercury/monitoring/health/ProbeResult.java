package com.umdc.mercury.monitoring.health;

/**
 * What one check of a service found. {@code cause} is {@code null} when the service is up.
 *
 * @param latencyMs how long the check took, or {@code -1} when it was not measured
 */
public record ProbeResult(ServiceStatus status, long latencyMs, HealthCause cause) {

    public static ProbeResult up(long latencyMs) {
        return new ProbeResult(ServiceStatus.UP, latencyMs, null);
    }

    /** A failure; the state is the default one of the cause. */
    public static ProbeResult failed(HealthCause cause, long latencyMs) {
        return new ProbeResult(cause.defaultStatus(), latencyMs, cause);
    }

    /** The service is switched off or not set up: not a failure. */
    public static ProbeResult notConfigured(HealthCause cause) {
        return new ProbeResult(ServiceStatus.NOT_CONFIGURED, -1, cause);
    }
}
