package com.umdc.mercury.monitoring.health;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** MongoDB: answers a {@code ping}, and fast enough. */
@Component
public class MongoProbe implements ServiceProbe {

    private final MongoTemplate mongoTemplate;
    private final long slowMs;
    private final Duration interval;

    public MongoProbe(MongoTemplate mongoTemplate, @Value("${umdc.monitoring.health.slow-ms:2000}") long slowMs,
                      @Value("${umdc.monitoring.health.interval-seconds:30}") long intervalSeconds) {
        this.mongoTemplate = mongoTemplate;
        this.slowMs = slowMs;
        this.interval = Duration.ofSeconds(Math.max(1, intervalSeconds));
    }

    @Override
    public String service() {
        return "MONGODB";
    }

    @Override
    public Duration interval() {
        return interval;
    }

    @Override
    public ProbeResult check() {
        var start = System.nanoTime();
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
        } catch (RuntimeException e) {
            var took = (System.nanoTime() - start) / 1_000_000;
            var timedOut = e.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("timeout");
            return ProbeResult.failed(timedOut ? HealthCause.TIMEOUT : HealthCause.CONNECTION_FAILED, took);
        }
        long took = (System.nanoTime() - start) / 1_000_000;
        return took > slowMs ? ProbeResult.failed(HealthCause.SLOW, took) : ProbeResult.up(took);
    }
}
