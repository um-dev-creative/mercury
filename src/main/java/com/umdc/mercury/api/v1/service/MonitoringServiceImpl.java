package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.jpa.sql.repository.VerificationCodeRepository;
import com.umdc.mercury.monitoring.MonitoringRange;
import com.umdc.mercury.monitoring.QueueSnapshots;
import com.umdc.mercury.monitoring.SendEventRepository;
import com.umdc.mercury.monitoring.SendEventService;
import com.umdc.mercury.monitoring.SendEventSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the monitoring views from the durable send events (history), the verification-code table and the live queues ({@link QueueSnapshots}). Nothing
 * is invented: a rate is {@code null} when there is nothing to divide, and a channel without a provider reports its messages as queued.
 */
@Service
public class MonitoringServiceImpl implements MonitoringService {

    /** Channels whose messages a provider really delivers today; the others stay queued (see CLAUDE.md, "Known gaps"). */
    static final Set<ChannelType> WITH_PROVIDER = Set.of(ChannelType.EMAIL, ChannelType.TELEGRAM);
    static final List<String> KINDS = List.of(SendEventService.KIND_TRANSACTIONAL, SendEventService.KIND_CAMPAIGN, SendEventService.KIND_VERIFICATION_CODE);
    static final int MAX_PAGE_SIZE = 100;
    private static final int TOP_CAUSES = 10;

    private final SendEventRepository events;
    private final VerificationCodeRepository codes;
    private final QueueSnapshots queueSnapshots;
    private final Clock clock;
    private final long stuckAfterMinutes;

    @Autowired
    public MonitoringServiceImpl(SendEventRepository events, VerificationCodeRepository codes, QueueSnapshots queueSnapshots,
                                 @Value("${umdc.monitoring.stuck-after-minutes:15}") long stuckAfterMinutes) {
        this(events, codes, queueSnapshots, stuckAfterMinutes, Clock.systemUTC());
    }

    MonitoringServiceImpl(SendEventRepository events, VerificationCodeRepository codes, QueueSnapshots queueSnapshots, long stuckAfterMinutes,
                          Clock clock) {
        this.events = events;
        this.codes = codes;
        this.queueSnapshots = queueSnapshots;
        this.stuckAfterMinutes = stuckAfterMinutes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringTO.SummaryResponse summary(UUID applicationId, String range) {
        var selected = MonitoringRange.parse(range);
        var now = now();
        var outcomes = events.countOutcomes(applicationId, selected.since(now));
        var queues = queueSnapshots.of(applicationId);

        var sent = new EnumMap<ChannelType, Long>(ChannelType.class);
        var failed = new EnumMap<ChannelType, Long>(ChannelType.class);
        var kindSent = new java.util.HashMap<String, Long>();
        var kindFailed = new java.util.HashMap<String, Long>();
        for (var outcome : outcomes) {
            var isSent = "SENT".equals(outcome.getStatus());
            channelOf(outcome.getChannel()).ifPresent(channel -> (isSent ? sent : failed).merge(channel, outcome.getTotal(), Long::sum));
            (isSent ? kindSent : kindFailed).merge(outcome.getKind(), outcome.getTotal(), Long::sum);
        }

        var channels = new ArrayList<MonitoringTO.ChannelSummary>();
        long totalSent = 0;
        long totalFailed = 0;
        long totalQueued = 0;
        for (ChannelType channel : ChannelType.values()) {
            long s = sent.getOrDefault(channel, 0L);
            long f = failed.getOrDefault(channel, 0L);
            long q = queues.waiting(channel);
            channels.add(new MonitoringTO.ChannelSummary(channel.name(), WITH_PROVIDER.contains(channel), s, f, q, rate(s, f)));
            totalSent += s;
            totalFailed += f;
            totalQueued += q;
        }
        var kinds = KINDS.stream()
                .map(kind -> new MonitoringTO.KindSummary(kind, kindSent.getOrDefault(kind, 0L), kindFailed.getOrDefault(kind, 0L),
                        rate(kindSent.getOrDefault(kind, 0L), kindFailed.getOrDefault(kind, 0L))))
                .toList();
        return new MonitoringTO.SummaryResponse(selected.code(), now, new MonitoringTO.Totals(totalSent, totalFailed, totalQueued, rate(totalSent, totalFailed)),
                channels, kinds);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringTO.SeriesResponse series(UUID applicationId, String range, String channel, String kind) {
        var selected = MonitoringRange.parse(range);
        var now = now();
        var since = selected.since(now);
        var channelName = channel == null || channel.isBlank() ? null : ChannelType.fromCode(channel.strip()).name();
        var kindName = kind == null || kind.isBlank() ? null : requireKind(kind);

        var sent = new java.util.HashMap<LocalDateTime, Long>();
        var failed = new java.util.HashMap<LocalDateTime, Long>();
        for (var row : events.countByBucket(applicationId, since, selected.unit(), channelName, kindName)) {
            ("SENT".equals(row.getStatus()) ? sent : failed).merge(row.getBucket(), row.getTotal(), Long::sum);
        }
        // every bucket of the range is returned, also the empty ones, so the chart has no holes
        var points = new ArrayList<MonitoringTO.SeriesPoint>();
        for (var bucket = since.truncatedTo(selected.chronoUnit()); !bucket.isAfter(now); bucket = bucket.plus(1, selected.chronoUnit())) {
            points.add(new MonitoringTO.SeriesPoint(bucket, sent.getOrDefault(bucket, 0L), failed.getOrDefault(bucket, 0L)));
        }
        return new MonitoringTO.SeriesResponse(selected.code(), selected.unit(), points);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringTO.FailuresResponse failures(UUID applicationId, String range) {
        var selected = MonitoringRange.parse(range);
        var now = now();
        var causes = events.topCauses(applicationId, selected.since(now), PageRequest.of(0, TOP_CAUSES)).stream()
                .map(row -> new MonitoringTO.FailureCause(row.getCause(), row.getChannel(), row.getTotal()))
                .toList();
        var queues = queueSnapshots.of(applicationId);
        var stuck = new ArrayList<MonitoringTO.StuckQueue>();
        for (ChannelType channel : ChannelType.values()) {
            var oldest = queues.oldestOlderThan(channel, now.minusMinutes(stuckAfterMinutes));
            long waiting = queues.countOlderThan(channel, now.minusMinutes(stuckAfterMinutes));
            if (waiting > 0 && oldest != null) {
                stuck.add(new MonitoringTO.StuckQueue(channel.name(), WITH_PROVIDER.contains(channel), waiting,
                        Math.max(0, Duration.between(oldest, now).toMinutes())));
            }
        }
        return new MonitoringTO.FailuresResponse(selected.code(), stuckAfterMinutes, causes, stuck);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringTO.VerificationCodeStats verificationCodes(UUID applicationId, String range) {
        var selected = MonitoringRange.parse(range);
        var now = now();
        var since = selected.since(now);
        long generated = codes.countByApplicationIdAndCreatedAtGreaterThanEqual(applicationId, since);
        long confirmed = codes.countByApplicationIdAndCreatedAtGreaterThanEqualAndIsVerifiedTrue(applicationId, since);
        long expired = codes.countByApplicationIdAndCreatedAtGreaterThanEqualAndIsVerifiedFalseAndExpiresAtBefore(applicationId, since, now);
        long pending = Math.max(0, generated - confirmed - expired);
        long failedSends = events.countOutcomes(applicationId, since).stream()
                .filter(o -> SendEventService.KIND_VERIFICATION_CODE.equals(o.getKind()) && "FAILED".equals(o.getStatus()))
                .mapToLong(SendEventRepository.OutcomeCount::getTotal).sum();
        return new MonitoringTO.VerificationCodeStats(selected.code(), generated, confirmed, expired, pending, failedSends,
                generated == 0 ? null : (double) confirmed / generated);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringTO.MessagePage messages(UUID applicationId, String channel, String status, String kind, UUID templateDefinedId,
                                             LocalDateTime from, LocalDateTime to, int page, int size) {
        var channelName = channel == null || channel.isBlank() ? null : ChannelType.fromCode(channel.strip()).name();
        var statusName = status == null || status.isBlank() ? null : requireStatus(status);
        var kindName = kind == null || kind.isBlank() ? null : requireKind(kind);
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "occurredAt"));
        var result = events.findAll(SendEventSpecifications.search(applicationId, channelName, statusName, kindName, templateDefinedId, from, to), pageable);
        var items = result.getContent().stream()
                .map(e -> new MonitoringTO.MessageItem(e.getId(), e.getOccurredAt(), e.getChannel(), e.getKind(), e.getStatus(), e.getMessageId(),
                        e.getCampaignId(), e.getTemplateDefinedId(), e.getRecipientMasked(), e.getErrorCause()))
                .toList();
        return new MonitoringTO.MessagePage(result.getNumber(), result.getSize(), result.getTotalElements(), items);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    private static Double rate(long sent, long failed) {
        return sent + failed == 0 ? null : (double) sent / (sent + failed);
    }

    private static java.util.Optional<ChannelType> channelOf(String name) {
        try {
            return java.util.Optional.of(ChannelType.valueOf(name));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    private static String requireKind(String kind) {
        var value = kind.strip().toUpperCase(Locale.ROOT);
        if (!KINDS.contains(value)) {
            throw new IllegalArgumentException("Unknown kind '" + kind + "': use " + String.join(", ", KINDS));
        }
        return value;
    }

    private static String requireStatus(String status) {
        var value = status.strip().toUpperCase(Locale.ROOT);
        if (!"SENT".equals(value) && !"FAILED".equals(value)) {
            throw new IllegalArgumentException("Unknown status '" + status + "': use SENT or FAILED");
        }
        return value;
    }
}
