package com.umdc.mercury.monitoring.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka: the cluster answers a {@code describeCluster}. Without the listeners (UMDC_KAFKA_AUTO_STARTUP=false, the local default) or with Kafka
 * disabled there is nothing to monitor and the state is NOT_CONFIGURED, not DOWN.
 */
@Component
public class KafkaProbe implements ServiceProbe {

    private final boolean enabled;
    private final boolean listenersOn;
    private final ClusterPing ping;
    private final Duration timeout;
    private final long slowMs;
    private final Duration interval;

    @Autowired
    public KafkaProbe(ObjectProvider<KafkaAdmin> admin, @Value("${umdc.kafka.enabled:true}") boolean enabled,
                      @Value("${umdc.kafka.auto-startup:false}") boolean listenersOn,
                      @Value("${umdc.monitoring.health.timeout-seconds:5}") int timeoutSeconds,
                      @Value("${umdc.monitoring.health.slow-ms:2000}") long slowMs,
                      @Value("${umdc.monitoring.health.interval-seconds:30}") long intervalSeconds) {
        this(enabled, listenersOn, adminPing(admin), timeoutSeconds, slowMs, intervalSeconds);
    }

    KafkaProbe(boolean enabled, boolean listenersOn, ClusterPing ping, int timeoutSeconds, long slowMs, long intervalSeconds) {
        this.enabled = enabled;
        this.listenersOn = listenersOn;
        this.ping = ping;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.slowMs = slowMs;
        this.interval = Duration.ofSeconds(Math.max(1, intervalSeconds));
    }

    private static ClusterPing adminPing(ObjectProvider<KafkaAdmin> admin) {
        return timeout -> {
            var configured = admin.getIfAvailable();
            if (configured == null) {
                throw new IllegalStateException("no KafkaAdmin");
            }
            try (var client = AdminClient.create(configured.getConfigurationProperties())) {
                client.describeCluster().nodes().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
        };
    }

    @Override
    public String service() {
        return "KAFKA";
    }

    @Override
    public Duration interval() {
        return interval;
    }

    @Override
    public ProbeResult check() {
        if (!enabled) {
            return ProbeResult.notConfigured(HealthCause.NOT_CONFIGURED);
        }
        if (!listenersOn) {
            return ProbeResult.notConfigured(HealthCause.LISTENERS_OFF);
        }
        var start = System.nanoTime();
        try {
            ping.ping(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProbeResult.failed(HealthCause.CHECK_ERROR, (System.nanoTime() - start) / 1_000_000);
        } catch (TimeoutException | org.apache.kafka.common.errors.TimeoutException e) {
            return ProbeResult.failed(HealthCause.TIMEOUT, (System.nanoTime() - start) / 1_000_000);
        } catch (ExecutionException e) {
            var timedOut = e.getCause() instanceof org.apache.kafka.common.errors.TimeoutException;
            return ProbeResult.failed(timedOut ? HealthCause.TIMEOUT : HealthCause.CONNECTION_FAILED, (System.nanoTime() - start) / 1_000_000);
        } catch (Exception e) {
            return ProbeResult.failed(HealthCause.CONNECTION_FAILED, (System.nanoTime() - start) / 1_000_000);
        }
        long took = (System.nanoTime() - start) / 1_000_000;
        return took > slowMs ? ProbeResult.failed(HealthCause.SLOW, took) : ProbeResult.up(took);
    }

    /** Asks the cluster for its nodes; throws when it cannot. */
    @FunctionalInterface
    interface ClusterPing {
        void ping(Duration timeout) throws Exception;
    }
}
