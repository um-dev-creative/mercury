package com.umdc.mercury.monitoring.health;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs the probes that are due and hands what they found to the {@link ServiceHealthRegistry}. Each probe has its own interval (SMTP is
 * checked rarely on purpose) and a hard time limit, so one service that hangs cannot stop the others being checked.
 */
@Component
public class ServiceHealthScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthScheduler.class);

    private final List<ServiceProbe> probes;
    private final ServiceHealthRegistry registry;
    private final Clock clock;
    private final Duration hardLimit;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Instant> lastRun = new ConcurrentHashMap<>();

    @Autowired
    public ServiceHealthScheduler(List<ServiceProbe> probes, ServiceHealthRegistry registry,
                                  @Value("${umdc.monitoring.health.timeout-seconds:5}") int timeoutSeconds) {
        this(probes, registry, Duration.ofSeconds(Math.max(1, timeoutSeconds) + 5L), Clock.systemUTC());
    }

    ServiceHealthScheduler(List<ServiceProbe> probes, ServiceHealthRegistry registry, Duration hardLimit, Clock clock) {
        this.probes = probes;
        this.registry = registry;
        this.clock = clock;
        this.hardLimit = hardLimit;
    }

    /** Rate: {@code umdc.scheduler.service-health.fixed-rate} (default 15 000 ms); each probe still keeps its own interval. */
    @Scheduled(fixedRateString = "${umdc.scheduler.service-health.fixed-rate:15000}", initialDelayString = "${umdc.scheduler.service-health.initial-delay:5000}")
    public void tick() {
        runDueProbes();
        registry.flushPending();
    }

    /** Runs, in parallel, every probe whose interval has passed, and applies the results. */
    void runDueProbes() {
        var now = clock.instant();
        var running = new ArrayList<Map.Entry<ServiceProbe, CompletableFuture<ProbeResult>>>();
        for (ServiceProbe probe : probes) {
            var last = lastRun.get(probe.service());
            if (last != null && now.isBefore(last.plus(probe.interval()))) {
                continue;
            }
            lastRun.put(probe.service(), now);
            running.add(Map.entry(probe, CompletableFuture.supplyAsync(() -> run(probe), executor)
                    .orTimeout(hardLimit.toMillis(), TimeUnit.MILLISECONDS)));
        }
        for (var entry : running) {
            registry.apply(entry.getKey().service(), await(entry.getKey(), entry.getValue()));
        }
    }

    private static ProbeResult run(ServiceProbe probe) {
        try {
            return probe.check();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProbeResult.failed(HealthCause.CHECK_ERROR, -1);
        } catch (Exception e) {
            // the class only: the message may hold a host or a credential
            logger.warn("The {} check failed with {}", probe.service(), e.getClass().getSimpleName());
            return ProbeResult.failed(HealthCause.CHECK_ERROR, -1);
        }
    }

    private static ProbeResult await(ServiceProbe probe, CompletableFuture<ProbeResult> future) {
        try {
            return future.join();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof TimeoutException) {
                logger.warn("The {} check did not finish in time", probe.service());
                return ProbeResult.failed(HealthCause.TIMEOUT, -1);
            }
            return ProbeResult.failed(HealthCause.CHECK_ERROR, -1);
        }
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
