package com.umdc.mercury.monitoring;

import com.umdc.mercury.api.v1.service.MonitoringService;
import com.umdc.mercury.monitoring.health.ServiceHealthService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The live feed of the dashboard (Server-Sent Events). A viewer subscribes for one application and range and receives, every few seconds, a
 * {@code summary} event (what the summary endpoint would answer) and a {@code health} event (the state of the services). The same events double
 * as the heartbeat. The data is computed once per application and range for all its viewers, and every send runs on its own virtual thread, so
 * a slow viewer cannot hold back the others. A stream ends after a while (the browser reconnects), which also makes it ask for the session again.
 */
@Component
public class MonitoringStreamService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringStreamService.class);

    private final MonitoringService monitoringService;
    private final ServiceHealthService healthService;
    private final int maxStreams;
    private final int maxPerApplication;
    private final long timeoutMillis;
    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();
    private final ExecutorService sender = Executors.newVirtualThreadPerTaskExecutor();

    public MonitoringStreamService(MonitoringService monitoringService, ServiceHealthService healthService,
                                   @Value("${umdc.monitoring.stream.max-streams:200}") int maxStreams,
                                   @Value("${umdc.monitoring.stream.max-per-application:20}") int maxPerApplication,
                                   @Value("${umdc.monitoring.stream.timeout-minutes:15}") long timeoutMinutes) {
        this.monitoringService = monitoringService;
        this.healthService = healthService;
        this.maxStreams = maxStreams;
        this.maxPerApplication = maxPerApplication;
        this.timeoutMillis = Math.max(1, timeoutMinutes) * 60_000;
    }

    /**
     * Opens a stream and sends the first events right away.
     *
     * @throws IllegalArgumentException if the range is unknown (a 400)
     * @throws ResponseStatusException  429 when the server or the application already has too many streams
     */
    public SseEmitter subscribe(UUID applicationId, String range) {
        var selected = MonitoringRange.parse(range).code();
        if (subscriptions.size() >= maxStreams || subscriptions.stream().filter(s -> s.application().equals(applicationId)).count() >= maxPerApplication) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many monitoring streams are open");
        }
        var emitter = newEmitter(timeoutMillis);
        var subscription = new Subscription(applicationId, selected, emitter);
        subscriptions.add(subscription);
        emitter.onCompletion(() -> subscriptions.remove(subscription));
        emitter.onTimeout(() -> {
            subscriptions.remove(subscription);
            emitter.complete();
        });
        emitter.onError(error -> subscriptions.remove(subscription));
        sender.execute(() -> deliver(subscription, summaryOf(subscription), healthService.health()));
        return emitter;
    }

    /** Creates the emitter of a stream; a seam so tests can watch what is sent. */
    protected SseEmitter newEmitter(long timeout) {
        return new SseEmitter(timeout);
    }

    /** How many streams are open. */
    public int open() {
        return subscriptions.size();
    }

    /** Pushes the current data to every open stream. Rate: {@code umdc.monitoring.stream.interval-seconds} (default 10). */
    @Scheduled(fixedRateString = "#{${umdc.monitoring.stream.interval-seconds:10} * 1000}")
    public void push() {
        if (subscriptions.isEmpty()) {
            return;
        }
        var health = healthService.health();
        Map<String, Object> summaries = new ConcurrentHashMap<>();
        for (Subscription subscription : subscriptions) {
            var key = subscription.application() + "/" + subscription.range();
            var summary = summaries.computeIfAbsent(key, k -> summaryOf(subscription));
            sender.execute(() -> deliver(subscription, summary, health));
        }
    }

    private Object summaryOf(Subscription subscription) {
        try {
            return monitoringService.summary(subscription.application(), subscription.range());
        } catch (RuntimeException e) {
            logger.warn("Could not build the summary for a stream: {}", e.getClass().getSimpleName());
            return Map.of("error", "SUMMARY_UNAVAILABLE");
        }
    }

    private void deliver(Subscription subscription, Object summary, Object health) {
        var emitter = subscription.emitter();
        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event().name("summary").data(summary));
                emitter.send(SseEmitter.event().name("health").data(health));
            } catch (IOException | IllegalStateException e) {
                // the viewer left or the stream already ended
                subscriptions.remove(subscription);
                emitter.complete();
            }
        }
    }

    /** Ends every stream and stops sending; called when Mercury shuts down. */
    @PreDestroy
    public void stop() {
        subscriptions.forEach(s -> s.emitter().complete());
        subscriptions.clear();
        sender.shutdownNow();
    }

    private record Subscription(UUID application, String range, SseEmitter emitter) {
    }
}
