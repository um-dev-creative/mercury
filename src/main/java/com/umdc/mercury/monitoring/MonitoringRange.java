package com.umdc.mercury.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** The ranges the dashboard offers and the size of the time bucket each one is drawn with. */
public enum MonitoringRange {
    LAST_HOUR("1h", Duration.ofHours(1), "minute", ChronoUnit.MINUTES),
    LAST_DAY("24h", Duration.ofHours(24), "hour", ChronoUnit.HOURS),
    LAST_WEEK("7d", Duration.ofDays(7), "day", ChronoUnit.DAYS),
    LAST_MONTH("30d", Duration.ofDays(30), "day", ChronoUnit.DAYS);

    private final String code;
    private final Duration length;
    private final String unit;
    private final ChronoUnit chronoUnit;

    MonitoringRange(String code, Duration length, String unit, ChronoUnit chronoUnit) {
        this.code = code;
        this.length = length;
        this.unit = unit;
        this.chronoUnit = chronoUnit;
    }

    /** Parses {@code 1h}, {@code 24h}, {@code 7d} or {@code 30d}; {@code null} or blank gives {@code 24h}. */
    public static MonitoringRange parse(String value) {
        if (value == null || value.isBlank()) {
            return LAST_DAY;
        }
        var wanted = value.strip().toLowerCase(Locale.ROOT);
        for (MonitoringRange range : values()) {
            if (range.code.equals(wanted)) {
                return range;
            }
        }
        throw new IllegalArgumentException("Unknown range '" + value + "': use 1h, 24h, 7d or 30d");
    }

    public String code() {
        return code;
    }

    /** The {@code date_trunc} unit of the buckets. */
    public String unit() {
        return unit;
    }

    public ChronoUnit chronoUnit() {
        return chronoUnit;
    }

    /** The start of the range counting back from {@code now}. */
    public LocalDateTime since(LocalDateTime now) {
        return now.minus(length);
    }
}
