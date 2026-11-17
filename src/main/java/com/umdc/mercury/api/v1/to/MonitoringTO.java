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
}
