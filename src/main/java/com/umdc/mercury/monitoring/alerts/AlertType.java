package com.umdc.mercury.monitoring.alerts;

/** What an alert rule watches. */
public enum AlertType {
    /** The share of failed outcomes (percent) over the window, once there are at least {@code minVolume} outcomes. */
    FAILURE_RATE,
    /** Messages waiting longer than the window (count, only the channels that have a provider). */
    QUEUE_STUCK,
    /** A service degraded or down for at least the window (minutes). */
    SERVICE_DOWN
}
