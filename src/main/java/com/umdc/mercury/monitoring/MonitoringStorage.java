package com.umdc.mercury.monitoring;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Knows whether the tables of the monitoring views exist. Recording a delivery is best effort and never throws, so when the migrations
 * {@code V2039} to {@code V2041} were not applied (for example with {@code MERCURY_FLYWAY_ENABLED=false}, where they are applied by hand) nothing
 * is recorded and nothing complains. This says so: once at start, in the health answer that the dashboard shows, and by counting the recordings
 * that failed. It looks again every half minute while something is missing, so applying the script by hand needs no restart.
 */
@Component
public class MonitoringStorage {

    /** The tables of the monitoring views, in the order they were added. */
    static final List<String> TABLES = List.of("send_events", "service_outages", "alert_rules", "alert_events");

    private static final Logger logger = LoggerFactory.getLogger(MonitoringStorage.class);
    private static final Duration RECHECK = Duration.ofSeconds(30);

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final AtomicLong recordFailures = new AtomicLong();
    private final AtomicReference<List<String>> missing = new AtomicReference<>(List.of());
    private final AtomicReference<Instant> checkedAt = new AtomicReference<>();

    @Autowired
    public MonitoringStorage(JdbcTemplate jdbc) {
        this(jdbc, Clock.systemUTC());
    }

    MonitoringStorage(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkAtStart() {
        var status = status();
        if (!status.ready()) {
            logger.error("The monitoring tables {} do not exist in schema mercury: delivery outcomes, outages and alerts are NOT being recorded. "
                    + "Apply {} (Flyway does it unless MERCURY_FLYWAY_ENABLED=false).", status.missingTables(), status.migrations());
        }
    }

    /** A recording that failed (the delivery itself is never affected). */
    public void recordFailed() {
        recordFailures.incrementAndGet();
    }

    /** What the dashboard shows. While tables are missing it looks again at most every 30 s. */
    public MonitoringTO.Storage status() {
        var now = clock.instant();
        var last = checkedAt.get();
        if (last == null || !missing.get().isEmpty() && Duration.between(last, now).compareTo(RECHECK) >= 0) {
            missing.set(lookForMissing());
            checkedAt.set(now);
        }
        var absent = missing.get();
        return new MonitoringTO.Storage(absent.isEmpty(), absent, absent.stream().map(MonitoringStorage::migrationOf).distinct().toList(), recordFailures.get());
    }

    /** The migration that creates a table. */
    static String migrationOf(String table) {
        return switch (table) {
            case "send_events" -> "V2039__send_events.sql";
            case "service_outages" -> "V2040__service_outages.sql";
            default -> "V2041__monitoring_alerts.sql";
        };
    }

    private List<String> lookForMissing() {
        var absent = new java.util.ArrayList<String>();
        for (var table : TABLES) {
            try {
                var exists = jdbc.queryForObject("select to_regclass(?) is not null", Boolean.class, "mercury." + table);
                if (!Boolean.TRUE.equals(exists)) {
                    absent.add(table);
                }
            } catch (RuntimeException e) {
                // the database cannot be asked right now: do not claim a table is missing because of that
                logger.warn("Could not check the monitoring table {}: {}", table, e.getClass().getSimpleName());
                return missing.get();
            }
        }
        return List.copyOf(absent);
    }
}
