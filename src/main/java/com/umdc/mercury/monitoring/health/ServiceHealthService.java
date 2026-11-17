package com.umdc.mercury.monitoring.health;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.monitoring.MonitoringRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** The read model of service health: the state of each service now, and the history of outages. */
@Service
public class ServiceHealthService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthService.class);

    private final ServiceHealthRegistry registry;
    private final ServiceOutageRepository outages;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate isolated;
    private final Clock clock;
    private final int retentionDays;

    @Autowired
    public ServiceHealthService(ServiceHealthRegistry registry, ServiceOutageRepository outages, PlatformTransactionManager transactionManager,
                                @Value("${umdc.monitoring.retention-days:90}") int retentionDays) {
        this(registry, outages, transactionManager, retentionDays, Clock.systemUTC());
    }

    ServiceHealthService(ServiceHealthRegistry registry, ServiceOutageRepository outages, PlatformTransactionManager transactionManager,
                         int retentionDays, Clock clock) {
        this.registry = registry;
        this.outages = outages;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.isolated = new TransactionTemplate(transactionManager);
        this.isolated.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.retentionDays = retentionDays;
        this.clock = clock;
    }

    /** The state of every service and the overall one: any DOWN is OUTAGE, else any DEGRADED is DEGRADED; nothing checked yet is UNKNOWN. */
    public MonitoringTO.HealthResponse health() {
        var states = registry.snapshot();
        var services = states.stream().map(ServiceHealthService::toView).toList();
        return new MonitoringTO.HealthResponse(now(), overall(states), services);
    }

    private static String overall(List<ServiceHealthRegistry.ServiceState> states) {
        if (states.stream().anyMatch(s -> s.status() == ServiceStatus.DOWN)) {
            return "OUTAGE";
        }
        if (states.stream().anyMatch(s -> s.status() == ServiceStatus.DEGRADED)) {
            return "DEGRADED";
        }
        var checked = states.stream().filter(s -> s.status() != ServiceStatus.NOT_CONFIGURED).toList();
        return checked.isEmpty() || checked.stream().allMatch(s -> s.status() == ServiceStatus.UNKNOWN) ? "UNKNOWN" : "OPERATIONAL";
    }

    private static MonitoringTO.ServiceHealth toView(ServiceHealthRegistry.ServiceState state) {
        return new MonitoringTO.ServiceHealth(state.service(), state.status().name(), state.latencyMs() < 0 ? null : state.latencyMs(),
                state.cause() == null ? null : state.cause().name(), state.cause() == null ? null : state.cause().text(),
                LocalDateTime.ofInstant(state.since(), ZoneOffset.UTC), state.checkedAt() == null ? null : LocalDateTime.ofInstant(state.checkedAt(), ZoneOffset.UTC));
    }

    /** The outages that overlap the range, newest first: those in the database plus any that is still only in memory (PostgreSQL was down). */
    public MonitoringTO.OutagesResponse outages(String range) {
        var selected = MonitoringRange.parse(range);
        var now = now();
        var since = selected.since(now);
        var all = new ArrayList<ServiceOutageEntity>();
        try {
            var rows = readOnly.execute(status -> outages.findOverlapping(since));
            if (rows != null) {
                all.addAll(rows);
            }
        } catch (RuntimeException e) {
            logger.warn("Could not read the outage history, answering with what is in memory: {}", e.getMessage());
        }
        var known = new HashSet<>(all.stream().map(ServiceOutageEntity::getId).toList());
        for (var inMemory : registry.inMemoryOutages()) {
            if (!known.contains(inMemory.getId()) && overlaps(inMemory, since)) {
                all.add(inMemory);
            } else if (known.contains(inMemory.getId())) {
                // the in-memory copy is the newest: it may have ended after the last write
                all.removeIf(row -> row.getId().equals(inMemory.getId()));
                all.add(inMemory);
            }
        }
        var views = all.stream().sorted(Comparator.comparing(ServiceOutageEntity::getStartedAt).reversed()).map(o -> toView(o, now)).toList();
        return new MonitoringTO.OutagesResponse(selected.code(), views);
    }

    private static boolean overlaps(ServiceOutageEntity outage, LocalDateTime since) {
        return outage.getEndedAt() == null || !outage.getEndedAt().isBefore(since);
    }

    private static MonitoringTO.Outage toView(ServiceOutageEntity outage, LocalDateTime now) {
        var end = outage.getEndedAt() == null ? now : outage.getEndedAt();
        return new MonitoringTO.Outage(outage.getId(), outage.getService(), outage.getStatus(), outage.getCauseCode(), outage.getCauseText(),
                outage.getStartedAt(), outage.getEndedAt(), Math.max(0, Duration.between(outage.getStartedAt().toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC)).toSeconds()),
                outage.getEndedAt() == null);
    }

    /** Deletes the outages that ended before the retention period; returns how many. */
    public int purgeExpired() {
        var before = now().minusDays(retentionDays);
        return java.util.Optional.ofNullable(isolated.execute(status -> outages.deleteEndedBefore(before))).orElse(0);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }
}
