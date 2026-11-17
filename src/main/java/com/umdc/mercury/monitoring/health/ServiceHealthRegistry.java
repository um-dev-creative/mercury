package com.umdc.mercury.monitoring.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What Mercury knows about the state of each service right now, and the outages it has seen. Two things feed it: the periodic probes
 * ({@link ServiceHealthScheduler}) and the real traffic (an e-mail that the SMTP server refuses is the best proof that SMTP is not well).
 *
 * <p>Outages are written to {@code service_outages} in their own transaction and best effort; one that cannot be written yet (PostgreSQL is
 * the service that is down) stays in memory and is written when the database is back.</p>
 */
@Component
public class ServiceHealthRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthRegistry.class);

    private final ServiceOutageRepository outages;
    private final TransactionTemplate isolated;
    private final Clock clock;
    private final Duration trafficHold;
    private final Map<String, ServiceState> states = new ConcurrentHashMap<>();
    private final Map<String, Outage> open = new ConcurrentHashMap<>();
    private final Map<String, Instant> trafficFailureAt = new ConcurrentHashMap<>();
    private final List<Outage> pendingWrites = new ArrayList<>();

    @Autowired
    public ServiceHealthRegistry(List<ServiceProbe> probes, ServiceOutageRepository outages, PlatformTransactionManager transactionManager,
                                 @Value("${umdc.monitoring.health.traffic-hold-minutes:10}") long trafficHoldMinutes) {
        this(probes, outages, transactionManager, trafficHoldMinutes, Clock.systemUTC());
    }

    ServiceHealthRegistry(List<ServiceProbe> probes, ServiceOutageRepository outages, PlatformTransactionManager transactionManager,
                          long trafficHoldMinutes, Clock clock) {
        this.outages = outages;
        this.isolated = new TransactionTemplate(transactionManager);
        this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
        this.trafficHold = Duration.ofMinutes(Math.max(0, trafficHoldMinutes));
        var now = clock.instant();
        probes.stream().map(ServiceProbe::service).sorted().forEach(name -> states.put(name, new ServiceState(name, ServiceStatus.UNKNOWN, -1, null, now, null)));
    }

    /** Picks up the outages that were still going on when Mercury stopped, so a recovery can close them. Best effort. */
    @EventListener(ApplicationReadyEvent.class)
    public void loadOpenOutages() {
        try {
            var rows = isolated.execute(status -> outages.findByEndedAtIsNull());
            synchronized (this) {
                for (var row : rows == null ? List.<ServiceOutageEntity>of() : rows) {
                    // keep the persisted id so closing it updates the same row
                    var restored = new Outage(row.getId(), row.getService(), ServiceStatus.valueOf(row.getStatus()), HealthCause.valueOf(row.getCauseCode()),
                            row.getStartedAt().toInstant(ZoneOffset.UTC));
                    restored.dirty = false;
                    open.putIfAbsent(row.getService(), restored);
                }
            }
        } catch (RuntimeException e) {
            logger.warn("Could not load the open service outages: {}", e.getMessage());
        }
    }

    /** Applies what a probe found. A clean result does not clear a failure that real traffic reported a moment ago. */
    public void apply(String service, ProbeResult result) {
        var writes = new ArrayList<Outage>();
        synchronized (this) {
            var now = clock.instant();
            var failedAt = trafficFailureAt.get(service);
            if (!result.status().isOutage() && failedAt != null && now.isBefore(failedAt.plus(trafficHold))) {
                return;
            }
            update(service, result.status(), result.latencyMs(), result.cause(), now, writes);
        }
        persist(writes);
    }

    /** The real traffic of a service failed (for example the SMTP server refused an e-mail). */
    public void reportFailure(String service, HealthCause cause) {
        var writes = new ArrayList<Outage>();
        synchronized (this) {
            var now = clock.instant();
            trafficFailureAt.put(service, now);
            var status = cause.defaultStatus().isOutage() ? cause.defaultStatus() : ServiceStatus.DEGRADED;
            update(service, status, states.containsKey(service) ? states.get(service).latencyMs() : -1, cause, now, writes);
        }
        persist(writes);
    }

    /** The real traffic of a service worked (for example an e-mail was accepted): it is up. */
    public void reportSuccess(String service) {
        var writes = new ArrayList<Outage>();
        synchronized (this) {
            var wasFailing = trafficFailureAt.remove(service) != null;
            var current = states.get(service);
            if (!wasFailing && current != null && !current.status().isOutage()) {
                return;
            }
            update(service, ServiceStatus.UP, current == null ? -1 : current.latencyMs(), null, clock.instant(), writes);
        }
        persist(writes);
    }

    private void update(String service, ServiceStatus status, long latencyMs, HealthCause cause, Instant now, List<Outage> writes) {
        var old = states.get(service);
        var unchanged = old != null && old.status() == status && old.cause() == cause;
        states.put(service, new ServiceState(service, status, latencyMs, cause, unchanged ? old.since() : now, now));

        var current = open.get(service);
        var sameOutage = status.isOutage() && current != null && current.status == status && current.cause == cause;
        if (current != null && !sameOutage) {
            current.endedAt = now;
            current.dirty = true;
            writes.add(current);
            open.remove(service);
            if (!status.isOutage()) {
                logger.info("Service {} recovered ({})", service, status);
            }
        }
        if (status.isOutage() && !sameOutage) {
            var started = new Outage(service, status, cause, now);
            open.put(service, started);
            writes.add(started);
            logger.warn("Service {} is {} ({})", service, status, cause);
        }
    }

    /** Writes the outages that are still waiting for the database (best effort); called by the scheduler on every tick. */
    public void flushPending() {
        List<Outage> writes;
        synchronized (this) {
            writes = new ArrayList<>(pendingWrites);
            pendingWrites.clear();
        }
        persist(writes);
    }

    private void persist(List<Outage> writes) {
        for (Outage outage : writes) {
            try {
                var entity = toEntity(outage);
                isolated.executeWithoutResult(status -> outages.save(entity));
                outage.dirty = false;
            } catch (RuntimeException e) {
                logger.warn("Could not write the outage of {} yet (kept in memory): {}", outage.service, e.getMessage());
                synchronized (this) {
                    if (!pendingWrites.contains(outage)) {
                        pendingWrites.add(outage);
                    }
                }
            }
        }
    }

    private static ServiceOutageEntity toEntity(Outage outage) {
        var entity = new ServiceOutageEntity();
        entity.setId(outage.id);
        entity.setService(outage.service);
        entity.setStatus(outage.status.name());
        entity.setCauseCode(outage.cause.name());
        entity.setCauseText(outage.cause.text());
        entity.setStartedAt(LocalDateTime.ofInstant(outage.startedAt, ZoneOffset.UTC));
        entity.setEndedAt(outage.endedAt == null ? null : LocalDateTime.ofInstant(outage.endedAt, ZoneOffset.UTC));
        return entity;
    }

    /** The state of every service, in alphabetical order. */
    public List<ServiceState> snapshot() {
        synchronized (this) {
            return states.values().stream().sorted(java.util.Comparator.comparing(ServiceState::service)).toList();
        }
    }

    /** The outages in memory that the database may not have yet (open ones and those waiting to be written), as entities. */
    List<ServiceOutageEntity> inMemoryOutages() {
        synchronized (this) {
            var all = new ArrayList<Outage>(open.values());
            pendingWrites.stream().filter(o -> !all.contains(o)).forEach(all::add);
            return all.stream().map(ServiceHealthRegistry::toEntity).toList();
        }
    }

    /** The state of one service as the views show it. */
    public record ServiceState(String service, ServiceStatus status, long latencyMs, HealthCause cause, Instant since, Instant checkedAt) {
    }

    /** An outage in memory; {@code dirty} means it still has to be written to the database. */
    static final class Outage {
        final UUID id;
        final String service;
        final ServiceStatus status;
        final HealthCause cause;
        final Instant startedAt;
        Instant endedAt;
        boolean dirty = true;

        Outage(String service, ServiceStatus status, HealthCause cause, Instant startedAt) {
            this(UUID.randomUUID(), service, status, cause, startedAt);
        }

        Outage(UUID id, String service, ServiceStatus status, HealthCause cause, Instant startedAt) {
            this.id = id;
            this.service = service;
            this.status = status;
            this.cause = cause;
            this.startedAt = startedAt;
        }
    }
}
