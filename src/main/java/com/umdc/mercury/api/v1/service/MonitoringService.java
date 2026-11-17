package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.MonitoringTO;

import java.time.LocalDateTime;
import java.util.UUID;

/** Read model of the monitoring dashboard. Every operation is scoped to one application. */
public interface MonitoringService {

    /** Totals, per channel and per kind, for a range ({@code 1h}, {@code 24h}, {@code 7d}, {@code 30d}; default {@code 24h}). */
    MonitoringTO.SummaryResponse summary(UUID applicationId, String range);

    /** Sent and failed per time bucket; the optional channel code and kind narrow it. Empty buckets are returned as zeros. */
    MonitoringTO.SeriesResponse series(UUID applicationId, String range, String channel, String kind);

    /** The most frequent failure causes and the queues that hold messages for too long. */
    MonitoringTO.FailuresResponse failures(UUID applicationId, String range);

    /** Generated, confirmed, expired and pending verification codes. */
    MonitoringTO.VerificationCodeStats verificationCodes(UUID applicationId, String range);

    /** The message table: newest first, every filter optional, recipients masked, at most 100 per page. */
    MonitoringTO.MessagePage messages(UUID applicationId, String channel, String status, String kind, UUID templateDefinedId,
                                      LocalDateTime from, LocalDateTime to, int page, int size);

    /** The rows of the message table for an export, newest first and at most {@code umdc.monitoring.export.max-rows}; {@code total} says whether it was cut. */
    MonitoringTO.MessageExport export(UUID applicationId, String channel, String status, String kind, UUID templateDefinedId,
                                      LocalDateTime from, LocalDateTime to);
}
