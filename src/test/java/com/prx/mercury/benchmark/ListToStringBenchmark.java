package com.prx.mercury.benchmark;

import com.prx.mercury.api.v1.to.EmailContact;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Benchmarks the {@code listToString} logic inside {@link com.prx.mercury.mapper.MessageRecordMapper}.
 *
 * <p>The <em>current</em> implementation converts a {@code List<EmailContact>} to a delimited
 * string via {@code Arrays.toString(stream.toArray(...))} which:
 * <ol>
 *   <li>Streams every element through a filter</li>
 *   <li>Boxes the result into a {@code String[]} via an allocating {@code IntFunction}</li>
 *   <li>Calls {@link Arrays#toString} – which adds {@code [} / {@code ]} brackets and {@code ", "} separators</li>
 * </ol>
 * This produces incorrect output ({@code [a@x.com, b@x.com]}) AND allocates two intermediate
 * objects (the array + the final String).
 *
 * <p>The <em>fixed</em> implementation uses {@code String.join(",", stream...)} which is both
 * correct (no brackets) and requires only one intermediate collection.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ListToStringBenchmark {

    private List<EmailContact> contacts10;
    private List<EmailContact> contacts100;
    private List<EmailContact> contacts1000;

    @Setup
    public void setup() {
        contacts10   = buildContacts(10);
        contacts100  = buildContacts(100);
        contacts1000 = buildContacts(1000);
    }

    // ── Current (buggy) implementation ───────────────────────────────────────

    @Benchmark
    public void current_10(Blackhole bh) {
        bh.consume(listToStringCurrent(contacts10));
    }

    @Benchmark
    public void current_100(Blackhole bh) {
        bh.consume(listToStringCurrent(contacts100));
    }

    @Benchmark
    public void current_1000(Blackhole bh) {
        bh.consume(listToStringCurrent(contacts1000));
    }

    // ── Fixed implementation ──────────────────────────────────────────────────

    @Benchmark
    public void fixed_10(Blackhole bh) {
        bh.consume(listToStringFixed(contacts10));
    }

    @Benchmark
    public void fixed_100(Blackhole bh) {
        bh.consume(listToStringFixed(contacts100));
    }

    @Benchmark
    public void fixed_1000(Blackhole bh) {
        bh.consume(listToStringFixed(contacts1000));
    }

    // ── Implementations ───────────────────────────────────────────────────────

    /** Exact copy of the current MessageRecordMapper.listToString logic. */
    private static String listToStringCurrent(List<EmailContact> list) {
        final IntFunction<String[]> function = String[]::new;
        String sb = "";
        if (list != null) {
            sb = Arrays.toString(list.stream()
                    .filter(Objects::nonNull)
                    .map(EmailContact::email)
                    .toArray(function));
        }
        return sb;
    }

    /** Proposed replacement using String.join. */
    private static String listToStringFixed(List<EmailContact> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(EmailContact::email)
                .collect(Collectors.joining(","));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<EmailContact> buildContacts(int n) {
        EmailContact[] arr = new EmailContact[n];
        for (int i = 0; i < n; i++) {
            arr[i] = new EmailContact("user" + i + "@example.com", "User " + i, null);
        }
        return List.of(arr);
    }
}
