package com.umdc.mercury.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("MonitoringRange")
class MonitoringRangeTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 0);

    @Test
    @DisplayName("parses the four ranges ignoring case and spaces, and defaults to 24h")
    void parses() {
        assertEquals(MonitoringRange.LAST_HOUR, MonitoringRange.parse("1h"));
        assertEquals(MonitoringRange.LAST_DAY, MonitoringRange.parse(" 24H "));
        assertEquals(MonitoringRange.LAST_WEEK, MonitoringRange.parse("7d"));
        assertEquals(MonitoringRange.LAST_MONTH, MonitoringRange.parse("30d"));
        assertEquals(MonitoringRange.LAST_DAY, MonitoringRange.parse(null));
        assertEquals(MonitoringRange.LAST_DAY, MonitoringRange.parse(" "));
    }

    @Test
    @DisplayName("an unknown range is refused with the accepted values")
    void refusesUnknown() {
        var error = assertThrows(IllegalArgumentException.class, () -> MonitoringRange.parse("3d"));
        assertEquals("Unknown range '3d': use 1h, 24h, 7d or 30d", error.getMessage());
    }

    @Test
    @DisplayName("each range starts the right time back and draws with the right bucket")
    void sinceAndBuckets() {
        assertEquals(NOW.minusHours(1), MonitoringRange.LAST_HOUR.since(NOW));
        assertEquals(NOW.minusDays(30), MonitoringRange.LAST_MONTH.since(NOW));
        assertEquals("minute", MonitoringRange.LAST_HOUR.unit());
        assertEquals("hour", MonitoringRange.LAST_DAY.unit());
        assertEquals("day", MonitoringRange.LAST_WEEK.unit());
        assertEquals(ChronoUnit.DAYS, MonitoringRange.LAST_MONTH.chronoUnit());
        assertEquals("24h", MonitoringRange.LAST_DAY.code());
    }
}
