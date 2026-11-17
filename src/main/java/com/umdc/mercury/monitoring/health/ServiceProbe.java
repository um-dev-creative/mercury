package com.umdc.mercury.monitoring.health;

import java.time.Duration;

/** One check of one service. Implementations must be safe to call from the scheduler and must not put secrets in the result. */
public interface ServiceProbe {

    /** The name the views show: SMTP, KAFKA, MONGODB, POSTGRESQL, BACKBONE or TELEGRAM. */
    String service();

    /** How often the scheduler may run this check. */
    Duration interval();

    /** Runs the check. A thrown exception is reported as {@link HealthCause#CHECK_ERROR}. */
    ProbeResult check() throws Exception;
}
