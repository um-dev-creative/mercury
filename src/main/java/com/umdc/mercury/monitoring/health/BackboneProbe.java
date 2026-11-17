package com.umdc.mercury.monitoring.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Backbone (identity service): it answers an HTTP request. Any answer below 500 means the server is up (a 401 or 404 on the health path still
 * proves it is reachable); a 5xx or no answer is DOWN. The address is never part of the result.
 */
@Component
public class BackboneProbe implements ServiceProbe {

    private static final int SERVER_ERROR = 500;

    private final HttpClient client;
    private final URI target;
    private final Duration timeout;
    private final long slowMs;
    private final Duration interval;

    public BackboneProbe(@Value("${umdc.backbone.base-url:https://api.umdc-qa.tst/backbone}") String baseUrl,
                         @Value("${umdc.monitoring.health.backbone-path:/actuator/health}") String path,
                         @Value("${umdc.monitoring.health.timeout-seconds:5}") int timeoutSeconds,
                         @Value("${umdc.monitoring.health.slow-ms:2000}") long slowMs,
                         @Value("${umdc.monitoring.health.interval-seconds:30}") long intervalSeconds) {
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.target = URI.create(baseUrl.replaceAll("/+$", "") + (path.startsWith("/") ? path : "/" + path));
        this.slowMs = slowMs;
        this.interval = Duration.ofSeconds(Math.max(1, intervalSeconds));
    }

    @Override
    public String service() {
        return "BACKBONE";
    }

    @Override
    public Duration interval() {
        return interval;
    }

    @Override
    public ProbeResult check() throws InterruptedException {
        var start = System.nanoTime();
        try {
            var response = client.send(HttpRequest.newBuilder(target).timeout(timeout).GET().build(), HttpResponse.BodyHandlers.discarding());
            long took = (System.nanoTime() - start) / 1_000_000;
            if (response.statusCode() >= SERVER_ERROR) {
                return ProbeResult.failed(HealthCause.HTTP_ERROR, took);
            }
            return took > slowMs ? ProbeResult.failed(HealthCause.SLOW, took) : ProbeResult.up(took);
        } catch (HttpTimeoutException e) {
            return ProbeResult.failed(HealthCause.TIMEOUT, (System.nanoTime() - start) / 1_000_000);
        } catch (IOException e) {
            return ProbeResult.failed(HealthCause.CONNECTION_FAILED, (System.nanoTime() - start) / 1_000_000);
        }
    }
}
