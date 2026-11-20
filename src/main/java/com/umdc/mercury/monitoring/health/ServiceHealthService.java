package com.umdc.mercury.monitoring.health;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.monitoring.MonitoringRange;
import com.umdc.mercury.monitoring.MonitoringStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** The read model of service health: the state of each service now, and the history of outages. */
@Service
public class ServiceHealthService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthService.class);
    private static final String CUSTOM = "custom";
    private static final int MAX_QUERY = 100;
    private static final int MAX_SERVICE = 30;
    private static final int MAX_STATUS = 10;
    private static final Set<String> STATUSES = Set.of("DOWN", "DEGRADED");
    private static final Comparator<ServiceOutageEntity> NEWEST_FIRST = Comparator.comparing(ServiceOutageEntity::getStartedAt).reversed();

    private final ServiceHealthRegistry registry;
    private final ServiceOutageRepository outages;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate isolated;
    private final Clock clock;
    private final int retentionDays;
    private final MonitoringStorage storage;


    @Autowired
    public ServiceHealthService(ServiceHealthRegistry registry, ServiceOutageRepository outages, PlatformTransactionManager transactionManager,
                                @Value("${umdc.monitoring.retention-days:90}") int retentionDays, MonitoringStorage storage) {
        this(registry, outages, transactionManager, retentionDays, Clock.systemUTC(), storage);
    }

    ServiceHealthService(ServiceHealthRegistry registry, ServiceOutageRepository outages, PlatformTransactionManager transactionManager,
                         int retentionDays, Clock clock) {
        this(registry, outages, transactionManager, retentionDays, clock, null);
    }

    ServiceHealthService(ServiceHealthRegistry registry, ServiceOutageRepository outages, PlatformTransactionManager transactionManager,
                         int retentionDays, Clock clock, MonitoringStorage storage) {
        this.storage = storage;
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
        return new MonitoringTO.HealthResponse(now(), overall(states), services, storage == null ? MonitoringTO.Storage.available() : storage.status());
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

    /** The first page of the outages of a preset range (see {@link #outages(MonitoringTO.OutageQuery)}). */
    public MonitoringTO.OutagesResponse outages(String range) {
        return outages(MonitoringTO.OutageQuery.of(range));
    }

    /**
     * One page of the outages that overlap the window of {@code query} and match its filters, newest first, with how many match in all.
     *
     * <p>Outages still only in memory (they began or ended while PostgreSQL was down) are not lost: an in-memory copy of a row in the
     * database replaces it on the page (it is the newer one), and one the database does not have yet goes at the top of the first page and
     * counts in the total. If the history cannot be read at all, the answer is the matching outages in memory, paged.</p>
     *
     * @throws IllegalArgumentException an unknown range or status, {@code from} not before {@code to}, or a search or service too long (400)
     */
    public MonitoringTO.OutagesResponse outages(MonitoringTO.OutageQuery query) {
        var window = Window.of(query, now());
        var service = normalized(query.service(), MAX_SERVICE, "service");
        var status = normalized(query.status(), MAX_STATUS, "status");
        if (!status.isEmpty() && !STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unknown status '" + query.status() + "': use DOWN or DEGRADED");
        }
        var text = query.q() == null ? "" : query.q().strip().toLowerCase(Locale.ROOT);
        if (text.length() > MAX_QUERY) {
            throw new IllegalArgumentException("The search is longer than " + MAX_QUERY + " characters");
        }
        var filter = new Filter(window.from(), window.to(), service, status, text);
        var inMemory = registry.inMemoryOutages().stream().filter(filter::matches).toList();
        var now = now();
        try {
            var page = Optional.ofNullable(readOnly.execute(tx -> {
                var rows = outages.search(window.from(), window.to(), service, status, likePattern(text), PageRequest.of(query.page(), query.size()));
                var stored = new HashSet<UUID>();
                for (var outage : inMemory) {
                    if (outages.existsById(outage.getId())) {
                        stored.add(outage.getId());
                    }
                }
                return new DatabasePage(rows.getContent(), rows.getTotalElements(), stored);
            })).orElse(DatabasePage.EMPTY);
            var byId = new HashMap<UUID, ServiceOutageEntity>();
            inMemory.forEach(o -> byId.put(o.getId(), o));
            var rows = new ArrayList<ServiceOutageEntity>(page.rows().stream().map(row -> byId.getOrDefault(row.getId(), row)).toList());
            var onlyInMemory = inMemory.stream().filter(o -> !page.stored().contains(o.getId())).toList();
            if (query.page() == 0) {
                rows.addAll(onlyInMemory);
            }
            return response(window, query, page.total() + onlyInMemory.size(), rows, now);
        } catch (RuntimeException e) {
            logger.warn("Could not read the outage history, answering with what is in memory: {}", e.getMessage());
            var sorted = inMemory.stream().sorted(NEWEST_FIRST).toList();
            var from = Math.min(sorted.size(), query.page() * query.size());
            return response(window, query, sorted.size(), sorted.subList(from, Math.min(sorted.size(), from + query.size())), now);
        }
    }

    private static MonitoringTO.OutagesResponse response(Window window, MonitoringTO.OutageQuery query, long total, List<ServiceOutageEntity> rows,
                                                         LocalDateTime now) {
        var views = rows.stream().sorted(NEWEST_FIRST).map(o -> toView(o, now)).toList();
        return new MonitoringTO.OutagesResponse(window.code(), window.from(), window.to(), query.page(), query.size(), total, views);
    }

    /** {@code text} as a lower-case {@code LIKE} pattern, with its own {@code %}, {@code _} and {@code !} escaped by {@code !}; empty matches all. */
    static String likePattern(String text) {
        if (text.isEmpty()) {
            return "%";
        }
        return "%" + text.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static String normalized(String value, int max, String name) {
        var clean = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (clean.length() > max) {
            throw new IllegalArgumentException("The " + name + " is longer than " + max + " characters");
        }
        return clean;
    }

    /** The window asked for: a preset back from now, or {@code from}–{@code to} ({@code custom}). */
    record Window(String code, LocalDateTime from, LocalDateTime to) {
        static Window of(MonitoringTO.OutageQuery query, LocalDateTime now) {
            var preset = MonitoringRange.parse(query.range());
            if (query.from() == null && query.to() == null) {
                return new Window(preset.code(), preset.since(now), now);
            }
            var to = query.to() == null ? now : query.to();
            var from = query.from() == null ? preset.since(to) : query.from();
            if (!from.isBefore(to)) {
                throw new IllegalArgumentException("'from' must be before 'to'");
            }
            return new Window(CUSTOM, from, to);
        }
    }

    /** The same filter as {@link ServiceOutageRepository#search}, for the outages that are only in memory. */
    private record Filter(LocalDateTime from, LocalDateTime to, String service, String status, String text) {
        boolean matches(ServiceOutageEntity outage) {
            return outage.getStartedAt().isBefore(to)
                    && (outage.getEndedAt() == null || !outage.getEndedAt().isBefore(from))
                    && (service.isEmpty() || service.equals(outage.getService()))
                    && (status.isEmpty() || status.equals(outage.getStatus()))
                    && (text.isEmpty() || contains(outage.getService()) || contains(outage.getCauseCode()) || contains(outage.getCauseText()));
        }

        private boolean contains(String value) {
            return value != null && value.toLowerCase(Locale.ROOT).contains(text);
        }
    }

    private record DatabasePage(List<ServiceOutageEntity> rows, long total, Set<UUID> stored) {
        static final DatabasePage EMPTY = new DatabasePage(List.of(), 0, Set.of());
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
        return Optional.ofNullable(isolated.execute(status -> outages.deleteEndedBefore(before))).orElse(0);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }
}
