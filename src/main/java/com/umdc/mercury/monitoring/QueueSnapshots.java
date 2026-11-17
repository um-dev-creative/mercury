package com.umdc.mercury.monitoring;

import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What is waiting to be delivered, per application and channel: the {@code OPENED} documents in MongoDB whose template belongs to the
 * application. The snapshot is reused for a few seconds so many viewers of the dashboard do not each read MongoDB.
 */
@Component
public class QueueSnapshots {

    private final TemplateDefinedRepository definitions;
    private final EmailMessageNSRepository emailQueue;
    private final MessageNSRepository channelQueue;
    private final Clock clock;
    private final Duration cacheFor;
    private final Map<UUID, QueueSnapshot> cache = new ConcurrentHashMap<>();

    @Autowired
    public QueueSnapshots(TemplateDefinedRepository definitions, EmailMessageNSRepository emailQueue, MessageNSRepository channelQueue,
                          @Value("${umdc.monitoring.cache-seconds:10}") long cacheSeconds) {
        this(definitions, emailQueue, channelQueue, cacheSeconds, Clock.systemUTC());
    }

    public QueueSnapshots(TemplateDefinedRepository definitions, EmailMessageNSRepository emailQueue, MessageNSRepository channelQueue,
                          long cacheSeconds, Clock clock) {
        this.definitions = definitions;
        this.emailQueue = emailQueue;
        this.channelQueue = channelQueue;
        this.cacheFor = Duration.ofSeconds(Math.max(0, cacheSeconds));
        this.clock = clock;
    }

    /** The queues of the application (or of all of them, {@link MonitoringScope#ALL}), from the cache while it is fresh. */
    public QueueSnapshot of(UUID applicationId) {
        var now = clock.instant();
        var cached = cache.get(applicationId);
        if (cached != null && now.isBefore(cached.takenAt().plus(cacheFor))) {
            return cached;
        }
        var fresh = load(applicationId, now);
        cache.put(applicationId, fresh);
        return fresh;
    }

    private QueueSnapshot load(UUID applicationId, Instant at) {
        // every application: no template filter (an undefined template still counts as a queued message of Mercury)
        boolean everything = MonitoringScope.isAll(applicationId);
        Set<UUID> templates = everything ? Set.of() : new HashSet<>(definitions.findIdsByApplicationId(applicationId));
        var waiting = new EnumMap<ChannelType, List<LocalDateTime>>(ChannelType.class);
        for (ChannelType channel : ChannelType.values()) {
            waiting.put(channel, new ArrayList<>());
        }
        if (everything || !templates.isEmpty()) {
            emailQueue.findByDeliveryStatus(DeliveryStatusType.OPENED).stream()
                    .filter(email -> everything || templates.contains(email.templateDefinedId()))
                    .forEach(email -> waiting.get(ChannelType.EMAIL).add(email.sendDate()));
            channelQueue.findByDeliveryStatus(DeliveryStatusType.OPENED).stream()
                    .filter(other -> other.getChannelType() != null && (everything || templates.contains(other.getTemplateDefinedId())))
                    .forEach(other -> waiting.get(other.getChannelType()).add(other.getSendDate()));
        }
        return new QueueSnapshot(at, waiting);
    }

    /** What waits in each channel: the send date of every queued message ({@code null} when the document has none). */
    public record QueueSnapshot(Instant takenAt, Map<ChannelType, List<LocalDateTime>> sendDates) {

        public long waiting(ChannelType channel) {
            return sendDates.get(channel).size();
        }

        /** Messages sent-dated before {@code limit}; an undated one is waiting but never counted as stuck. */
        public long countOlderThan(ChannelType channel, LocalDateTime limit) {
            return sendDates.get(channel).stream().filter(d -> d != null && d.isBefore(limit)).count();
        }

        /** The oldest send date before {@code limit}, or {@code null} when none. */
        public LocalDateTime oldestOlderThan(ChannelType channel, LocalDateTime limit) {
            return sendDates.get(channel).stream().filter(d -> d != null && d.isBefore(limit)).min(LocalDateTime::compareTo).orElse(null);
        }
    }
}
