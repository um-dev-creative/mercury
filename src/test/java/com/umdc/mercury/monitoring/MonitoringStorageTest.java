package com.umdc.mercury.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringStorage")
class MonitoringStorageTest {

    private static final String QUERY = "select to_regclass(?) is not null";

    @Mock
    private JdbcTemplate jdbc;

    private void tables(boolean sendEvents, boolean outages, boolean rules, boolean events) {
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.send_events")).thenReturn(sendEvents);
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.service_outages")).thenReturn(outages);
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.alert_rules")).thenReturn(rules);
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.alert_events")).thenReturn(events);
    }

    @Test
    @DisplayName("with every table present the storage is ready")
    void ready() {
        tables(true, true, true, true);

        var status = new MonitoringStorage(jdbc).status();

        assertTrue(status.ready());
        assertEquals(List.of(), status.missingTables());
        assertEquals(List.of(), status.migrations());
    }

    @Test
    @DisplayName("missing tables are named with the migrations that create them (each migration once)")
    void missing() {
        tables(false, false, false, false);

        var status = new MonitoringStorage(jdbc).status();

        assertFalse(status.ready());
        assertEquals(List.of("send_events", "service_outages", "alert_rules", "alert_events"), status.missingTables());
        assertEquals(List.of("V2039__send_events.sql", "V2040__service_outages.sql", "V2041__monitoring_alerts.sql"), status.migrations());
    }

    @Test
    @DisplayName("the check at start only complains when something is missing")
    void checkAtStart() {
        tables(true, true, true, true);
        var healthy = new MonitoringStorage(jdbc);
        healthy.checkAtStart();
        assertTrue(healthy.status().ready());
    }

    @Test
    @DisplayName("the check at start logs the missing tables and does not throw")
    void checkAtStartMissing() {
        tables(false, true, true, true);

        new MonitoringStorage(jdbc).checkAtStart();
    }

    @Test
    @DisplayName("while all is fine it does not ask again; while something is missing it asks again after 30 s, so applying the script needs no restart")
    void rechecks() {
        var clock = new com.umdc.mercury.monitoring.health.MovableClock();
        var storage = new MonitoringStorage(jdbc, clock);
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.send_events")).thenReturn(false, true);
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.service_outages")).thenReturn(true);
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.alert_rules")).thenReturn(true);
        when(jdbc.queryForObject(QUERY, Boolean.class, "mercury.alert_events")).thenReturn(true);

        assertFalse(storage.status().ready());
        clock.advanceSeconds(10);
        assertFalse(storage.status().ready(), "too soon to look again");
        clock.advanceSeconds(25);
        assertTrue(storage.status().ready(), "the table was created by hand");
        clock.advanceSeconds(600);
        assertTrue(storage.status().ready());
        verify(jdbc, times(2)).queryForObject(QUERY, Boolean.class, "mercury.send_events");
    }

    @Test
    @DisplayName("when the database cannot be asked it does not claim a table is missing")
    void databaseUnavailable() {
        when(jdbc.queryForObject(eq(QUERY), eq(Boolean.class), org.mockito.ArgumentMatchers.<Object>any())).thenThrow(new IllegalStateException("db down"));

        assertTrue(new MonitoringStorage(jdbc).status().ready());
    }

    @Test
    @DisplayName("counts the recordings that failed")
    void countsFailures() {
        tables(true, true, true, true);
        var storage = new MonitoringStorage(jdbc);

        storage.recordFailed();
        storage.recordFailed();

        assertEquals(2, storage.status().recordFailures());
        verify(jdbc, never()).update(org.mockito.ArgumentMatchers.anyString());
    }
}
