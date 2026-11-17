package com.umdc.mercury.monitoring.health;

import com.umdc.mercury.provider.TelegramMessageDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.util.Locale;

/** Telegram Bot API: {@code getMe} works. Without a bot token the state is NOT_CONFIGURED, not DOWN. */
@Component
public class TelegramProbe implements ServiceProbe {

    private final TelegramMessageDispatcher dispatcher;
    private final long slowMs;
    private final Duration interval;

    public TelegramProbe(TelegramMessageDispatcher dispatcher, @Value("${umdc.monitoring.health.slow-ms:2000}") long slowMs,
                         @Value("${umdc.monitoring.health.interval-seconds:30}") long intervalSeconds) {
        this.dispatcher = dispatcher;
        this.slowMs = slowMs;
        this.interval = Duration.ofSeconds(Math.max(1, intervalSeconds));
    }

    @Override
    public String service() {
        return "TELEGRAM";
    }

    @Override
    public Duration interval() {
        return interval;
    }

    @Override
    public ProbeResult check() {
        if (!dispatcher.isEnabled()) {
            return ProbeResult.notConfigured(HealthCause.NOT_CONFIGURED);
        }
        var start = System.nanoTime();
        try {
            dispatcher.checkReachable();
        } catch (TelegramApiException e) {
            var detail = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
            var cause = detail.contains("401") || detail.contains("unauthorized") ? HealthCause.AUTH_REFUSED
                    : detail.contains("timed out") || detail.contains("timeout") ? HealthCause.TIMEOUT : HealthCause.CONNECTION_FAILED;
            return ProbeResult.failed(cause, (System.nanoTime() - start) / 1_000_000);
        }
        long took = (System.nanoTime() - start) / 1_000_000;
        return took > slowMs ? ProbeResult.failed(HealthCause.SLOW, took) : ProbeResult.up(took);
    }
}
