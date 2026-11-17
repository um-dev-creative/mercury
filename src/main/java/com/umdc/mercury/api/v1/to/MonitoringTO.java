package com.umdc.mercury.api.v1.to;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Transfer objects of the monitoring endpoints ({@code /api/v1/monitoring/**}). Recipients are always masked. */
public interface MonitoringTO {

    /**
     * Totals of a channel or of everything. {@code successRate} is {@code sent / (sent + failed)} and {@code null} when nothing finished, so
     * no rate is invented.
     */
    public record Totals(long sent, long failed, long queued, Double successRate) {
    }

    /** One channel. {@code providerConnected=false} means its messages are only queued: no provider delivers them yet. */
    public record ChannelSummary(String channel, boolean providerConnected, long sent, long failed, long queued, Double successRate) {
    }

    /** One kind of communication (transactional, campaign, verification code). */
    public record KindSummary(String kind, long sent, long failed, Double successRate) {
    }

    /** {@code GET /monitoring/applications/{id}/summary}. */
    public record SummaryResponse(String range, LocalDateTime generatedAt, Totals totals, List<ChannelSummary> channels, List<KindSummary> kinds) {
    }

    /** Outcomes inside one time bucket. */
    public record SeriesPoint(LocalDateTime bucket, long sent, long failed) {
    }

    /** {@code GET /monitoring/applications/{id}/series}: {@code unit} is {@code minute}, {@code hour} or {@code day}. */
    public record SeriesResponse(String range, String unit, List<SeriesPoint> points) {
    }

    /** How often one cause failed messages of one channel. */
    public record FailureCause(String cause, String channel, long total) {
    }

    /** Messages waiting for longer than the threshold in one channel. */
    public record StuckQueue(String channel, boolean providerConnected, long waiting, long oldestMinutes) {
    }

    /** {@code GET /monitoring/applications/{id}/failures}. */
    public record FailuresResponse(String range, long stuckAfterMinutes, List<FailureCause> causes, List<StuckQueue> stuck) {
    }

    /** {@code GET /monitoring/applications/{id}/verification-codes}: the confirmation rate is {@code null} when no code was generated. */
    public record VerificationCodeStats(String range, long generated, long confirmed, long expired, long pending, long failedSends,
                                        Double confirmationRate) {
    }

    /** One delivery outcome in the message table. */
    public record MessageItem(UUID id, LocalDateTime occurredAt, String channel, String kind, String status, UUID messageId, UUID campaignId,
                              UUID templateDefinedId, String recipient, String errorCause) {
    }

    /** {@code GET /monitoring/applications/{id}/messages}. */
    public record MessagePage(int page, int size, long total, List<MessageItem> items) {
    }

    /** The rows of an export and how many matched in all. */
    public record MessageExport(List<MessageItem> items, long total) {
        public boolean truncated() {
            return total > items.size();
        }
    }

    /** Query of {@code /messages/export}: the filters of the message table plus the format ({@code csv}, the default, or {@code json}). */
    record ExportQuery(String format, String channel, String status, String kind, UUID templateDefinedId,
                       @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                       @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
    }

    /** Query of {@code /series}: all optional, {@code range} defaults to 24h. */
    record SeriesQuery(String range, String channel, String kind) {
    }

    /** Query of {@code /messages}: every filter optional; a missing page is 0 and a missing or non-positive size is 20, and the service never returns more than 100. */
    record MessageQuery(String channel, String status, String kind, UUID templateDefinedId,
                        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                        Integer page, Integer size) {
        public MessageQuery {
            page = page == null || page < 0 ? 0 : page;
            size = size == null || size <= 0 ? 20 : size;
        }
    }

    /**
     * State of one service. {@code latencyMs} is {@code null} when it was not measured; {@code cause} and {@code causeText} are a fixed code and
     * sentence (never a raw error) and are {@code null} while the service is up.
     */
    record ServiceHealth(String service, String status, Long latencyMs, String cause, String causeText, LocalDateTime since,
                         LocalDateTime lastCheckedAt) {
    }

    /** {@code GET /monitoring/health}: {@code overall} is OPERATIONAL, DEGRADED, OUTAGE or UNKNOWN. */
    record HealthResponse(LocalDateTime generatedAt, String overall, List<ServiceHealth> services) {
    }

    /** One outage; {@code endedAt} is {@code null} and {@code ongoing} true while it lasts. */
    record Outage(UUID id, String service, String status, String cause, String causeText, LocalDateTime startedAt, LocalDateTime endedAt,
                  long durationSeconds, boolean ongoing) {
    }

    /** {@code GET /monitoring/health/outages}: the outages that overlap the range, newest first. */
    record OutagesResponse(String range, List<Outage> outages) {
    }

    /** An alert rule. {@code threshold} is a percentage (FAILURE_RATE), a number of messages (QUEUE_STUCK) or unused (SERVICE_DOWN). */
    record AlertRule(UUID id, String type, double threshold, int windowMinutes, int minVolume, String service, List<String> notifyEmails, boolean enabled,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    /** Body of a rule creation or update; absent optional fields take their default (window 15, volume 0, enabled). */
    record AlertRuleRequest(String type, Double threshold, Integer windowMinutes, Integer minVolume, String service, List<String> notifyEmails,
                            Boolean enabled) {
    }

    /** An alert a rule raised; {@code resolvedAt} is {@code null} while it fires. */
    record AlertEvent(UUID id, UUID ruleId, String type, String status, double value, String detail, LocalDateTime startedAt, LocalDateTime resolvedAt) {
    }

    /** {@code GET /alerts}: the alerts that fire now and the recent ones. */
    record AlertsResponse(String range, long firing, List<AlertEvent> alerts) {
    }

    /** {@code GET /alerts/rules}. */
    record AlertRulesResponse(List<AlertRule> rules) {
    }
}
