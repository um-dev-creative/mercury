package com.prx.mercury.benchmark;

import com.prx.mercury.jpa.sql.entity.VerificationCodeEntity;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks the in-memory streaming logic of
 * {@link com.prx.mercury.api.v1.service.VerificationCodeServiceImpl#confirmCode}.
 *
 * <p>The <em>current</em> implementation performs <b>two full passes</b> over the list:
 * <ol>
 *   <li>First pass: {@code stream().filter().map().toList()} – mutates every qualifying entity
 *       and collects them all into a new list.</li>
 *   <li>Second pass: {@code stream().filter().findFirst()} – re-scans the already-processed list
 *       just to check whether <em>any</em> entity was marked verified.</li>
 * </ol>
 * The second pass is entirely redundant; its result is knowable at the end of the first pass.
 *
 * <p>The <em>fixed</em> implementation performs a <b>single pass</b> using an {@link java.util.Optional}
 * reference captured during the mutation step.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ConfirmCodeStreamBenchmark {

    private static final String CORRECT_CODE = "ABC123";
    private static final String WRONG_CODE   = "WRONG0";

    // Lists of different sizes to show O(n) allocation difference
    private List<VerificationCodeEntity> entities1;
    private List<VerificationCodeEntity> entities10;
    private List<VerificationCodeEntity> entities50;

    @Setup(Level.Invocation)
    public void setup() {
        // Re-create for every invocation so state mutations don't cross benchmark runs
        entities1  = buildEntities(1,  CORRECT_CODE);
        entities10 = buildEntities(10, CORRECT_CODE);
        entities50 = buildEntities(50, CORRECT_CODE);
    }

    // ── Current (double-stream) ───────────────────────────────────────────────

    @Benchmark
    public void current_1entity(Blackhole bh) {
        bh.consume(isVerifiedCurrent(entities1, CORRECT_CODE));
    }

    @Benchmark
    public void current_10entities(Blackhole bh) {
        bh.consume(isVerifiedCurrent(entities10, CORRECT_CODE));
    }

    @Benchmark
    public void current_50entities(Blackhole bh) {
        bh.consume(isVerifiedCurrent(entities50, CORRECT_CODE));
    }

    // ── Fixed (single-stream) ─────────────────────────────────────────────────

    @Benchmark
    public void fixed_1entity(Blackhole bh) {
        bh.consume(isVerifiedFixed(entities1, CORRECT_CODE));
    }

    @Benchmark
    public void fixed_10entities(Blackhole bh) {
        bh.consume(isVerifiedFixed(entities10, CORRECT_CODE));
    }

    @Benchmark
    public void fixed_50entities(Blackhole bh) {
        bh.consume(isVerifiedFixed(entities50, CORRECT_CODE));
    }

    // ── Implementations ───────────────────────────────────────────────────────

    /**
     * Mirrors the current VerificationCodeServiceImpl.confirmCode streaming logic
     * (minus the repository save, which is an I/O side-effect we can't benchmark in isolation).
     */
    private static boolean isVerifiedCurrent(List<VerificationCodeEntity> list, String code) {
        var collectionResult = list.stream()
                .filter(e -> !e.getIsVerified() && e.getAttempts() < e.getMaxAttempts())
                .map(e -> {
                    e.setAttempts(e.getAttempts() + 1);
                    e.setModifiedAt(LocalDateTime.now());
                    if (e.getVerificationCode().equals(code)) {
                        e.setIsVerified(true);
                        e.setVerifiedAt(LocalDateTime.now());
                    }
                    return e;
                }).toList();

        // Second redundant pass
        return collectionResult.stream()
                .filter(VerificationCodeEntity::getIsVerified)
                .findFirst()
                .isPresent();
    }

    /**
     * Proposed fix: single-pass using a flag captured inside the mapping step.
     */
    private static boolean isVerifiedFixed(List<VerificationCodeEntity> list, String code) {
        final boolean[] verified = {false};
        list.stream()
                .filter(e -> !e.getIsVerified() && e.getAttempts() < e.getMaxAttempts())
                .forEach(e -> {
                    e.setAttempts(e.getAttempts() + 1);
                    e.setModifiedAt(LocalDateTime.now());
                    if (e.getVerificationCode().equals(code)) {
                        e.setIsVerified(true);
                        e.setVerifiedAt(LocalDateTime.now());
                        verified[0] = true;
                    }
                });
        return verified[0];
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<VerificationCodeEntity> buildEntities(int n, String code) {
        List<VerificationCodeEntity> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            VerificationCodeEntity e = new VerificationCodeEntity();
            e.setUserId(UUID.randomUUID());
            e.setVerificationCode(i == 0 ? code : WRONG_CODE);
            e.setIsVerified(false);
            e.setAttempts(0);
            e.setMaxAttempts(3);
            e.setCreatedAt(LocalDateTime.now());
            list.add(e);
        }
        return list;
    }
}
