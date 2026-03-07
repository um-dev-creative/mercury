package com.prx.mercury.benchmark;

import com.prx.mercury.constant.ChannelType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks {@link com.prx.mercury.api.v1.service.CampaignMessageFactory#topicFor}.
 *
 * <p>The <em>current</em> implementation concatenates three strings on <b>every call</b>:
 * {@code "mercury-" + channelCode + "-messages"}.  For a campaign with 10 000 recipients
 * this allocates 10 000 identical intermediate {@code String} objects.
 *
 * <p>The <em>fixed</em> implementation pre-computes the topic name once per
 * {@link ChannelType} at construction time and stores results in an {@link EnumMap}.
 * Subsequent calls are a single map lookup with zero allocation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TopicForBenchmark {

    private static final Map<ChannelType, String> TOPIC_CACHE;

    static {
        TOPIC_CACHE = new EnumMap<>(ChannelType.class);
        for (ChannelType type : ChannelType.values()) {
            TOPIC_CACHE.put(type, "mercury-" + type.getCode() + "-messages");
        }
    }

    @Benchmark
    public void current_email(Blackhole bh) {
        bh.consume(topicForCurrent("email"));
    }

    @Benchmark
    public void current_sms(Blackhole bh) {
        bh.consume(topicForCurrent("sms"));
    }

    @Benchmark
    public void fixed_email(Blackhole bh) {
        bh.consume(topicForFixed(ChannelType.EMAIL));
    }

    @Benchmark
    public void fixed_sms(Blackhole bh) {
        bh.consume(topicForFixed(ChannelType.SMS));
    }

    // ── Implementations ───────────────────────────────────────────────────────

    /** Exact copy of the current CampaignMessageFactory.topicFor. */
    private static String topicForCurrent(String channelCode) {
        return "mercury-" + channelCode + "-messages";
    }

    /** Proposed cache-based replacement. */
    private static String topicForFixed(ChannelType type) {
        return TOPIC_CACHE.get(type);
    }
}
