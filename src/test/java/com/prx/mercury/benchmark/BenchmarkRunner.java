package com.prx.mercury.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Entry-point for running all Mercury JMH benchmarks from the {@code benchmark} Maven profile.
 *
 * <p>Run via:
 * <pre>
 *   mvn -Pbenchmark test-compile exec:java \
 *       -Dexec.mainClass=com.prx.mercury.benchmark.BenchmarkRunner \
 *       -Dexec.classpathScope=test
 * </pre>
 */
public class BenchmarkRunner {

    private BenchmarkRunner() {}

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(ListToStringBenchmark.class.getSimpleName())
                .include(ConfirmCodeStreamBenchmark.class.getSimpleName())
                .include(TopicForBenchmark.class.getSimpleName())
                .warmupIterations(2)
                .measurementIterations(3)
                .forks(1)
                .build();

        new Runner(opts).run();
    }
}
