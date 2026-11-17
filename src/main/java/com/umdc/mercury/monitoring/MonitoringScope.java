package com.umdc.mercury.monitoring;

import java.util.UUID;

/**
 * What a monitoring view is about. Mercury serves many applications (ZeroBound, Iris, ...); the monitoring is Mercury's own, so by default it covers
 * every send whoever asked for it. {@link #ALL} (the nil UUID) in the {@code applicationId} of a request means all of them, and any other id
 * narrows the view to that application.
 */
public final class MonitoringScope {

    /** All applications. */
    public static final UUID ALL = new UUID(0L, 0L);

    private MonitoringScope() {
        // utility class
    }

    public static boolean isAll(UUID applicationId) {
        return applicationId == null || ALL.equals(applicationId);
    }

    /** The application to filter by, or {@code null} when the view is about all of them. */
    public static UUID filter(UUID applicationId) {
        return isAll(applicationId) ? null : applicationId;
    }
}
