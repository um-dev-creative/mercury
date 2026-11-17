package com.umdc.mercury.monitoring.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;

/** PostgreSQL: a connection from the pool must be valid, and fast enough. */
@Component
public class PostgresProbe implements ServiceProbe {

    private final DataSource dataSource;
    private final int timeoutSeconds;
    private final long slowMs;
    private final Duration interval;

    public PostgresProbe(DataSource dataSource, @Value("${umdc.monitoring.health.timeout-seconds:5}") int timeoutSeconds,
                         @Value("${umdc.monitoring.health.slow-ms:2000}") long slowMs,
                         @Value("${umdc.monitoring.health.interval-seconds:30}") long intervalSeconds) {
        this.dataSource = dataSource;
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.slowMs = slowMs;
        this.interval = Duration.ofSeconds(Math.max(1, intervalSeconds));
    }

    @Override
    public String service() {
        return "POSTGRESQL";
    }

    @Override
    public Duration interval() {
        return interval;
    }

    @Override
    public ProbeResult check() {
        var start = System.nanoTime();
        try (var connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(timeoutSeconds);
            long took = (System.nanoTime() - start) / 1_000_000;
            if (!valid) {
                return ProbeResult.failed(HealthCause.CONNECTION_FAILED, took);
            }
            return took > slowMs ? ProbeResult.failed(HealthCause.SLOW, took) : ProbeResult.up(took);
        } catch (SQLTimeoutException e) {
            return ProbeResult.failed(HealthCause.TIMEOUT, (System.nanoTime() - start) / 1_000_000);
        } catch (SQLException e) {
            return ProbeResult.failed(HealthCause.CONNECTION_FAILED, (System.nanoTime() - start) / 1_000_000);
        }
    }
}
