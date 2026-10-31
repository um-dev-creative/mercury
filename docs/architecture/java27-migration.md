# MER-4: Java runtime upgrade — targeted Java 25 instead of Java 27

> Filename kept as `java27-migration.md` to match the MER-4 ticket's tracked
> reference, even though the actual target changed to Java 25. See the
> decision below.

## Decision: Java 25 (LTS), not Java 27

The ticket originally called for a Java 21 → 27 upgrade. During planning
(2026-09-24) we changed the target to **Java 25**, the current LTS release,
for two concrete reasons:

1. **Java 27 is a short-term, non-LTS release.** It reached GA on
   2026-09-15 with only 6 months of Oracle support (EOL ≈ 2027-03). Adopting
   it would mean committing to another JDK hop within roughly half a year.
   Java 25 (GA September 2025) is a long-term-support release with a much
   longer support horizon.
2. **The ecosystem is materially better verified on 25.** Spring Boot,
   JaCoCo, and PMD/ASM all have confirmed, released support for Java 25's
   class file version today. Java 27 support for several of these was still
   landing upstream as of this migration (e.g. Spring Boot's own Java 27
   support documentation was still being finalized in September 2026).

Java 25 delivers the same practical objectives the ticket was after —
modern language features, a real memory-footprint win via Compact Object
Headers — without the forced-re-migration risk or the unverified dependency
chain.

## What actually changed

### Build (`pom.xml`)
- `java.version`: `21` → `25`. This single property drives
  `maven.compiler.source/target`, the compiler `<release>` flag, and PMD's
  `targetJdk`.
- `asm.version`: `9.7` → `9.10.1` (**required**). ASM 9.7 cannot parse Java
  25 bytecode (class file major version 69) — this pin exists to override
  the ASM version pulled in transitively by the Surefire Mockito-agent
  `argLine`, so it has to move in lockstep with the JDK target. Confirmed by
  reproducing the failure and fix locally, not just by reading changelogs.
- `jacoco-maven-plugin` (0.8.14) and `maven-pmd-plugin` (3.28.0, bundling PMD
  7.17.0) needed **no version bump** — both already support Java 25.
- `spring-boot-starter-parent` (4.1.0) needed no version bump — documented
  compatible up to Java 26.

### The real gotcha: annotation processing silently stopped working
Compiling under real JDK 25 revealed that **MapStruct's generated
`*Impl` classes stopped being produced** — not a warning, not a build
failure, just silently missing implementation classes, surfaced only when
tests failed with `ClassNotFoundException`. Root cause: javac dropped
implicit classpath-based annotation-processor discovery by default in a
recent JDK release ([JDK-8321319](https://bugs.openjdk.org/browse/JDK-8321319)).
This project declared `mapstruct-processor` (and
`spring-boot-configuration-processor`) as plain compile-scope dependencies,
relying on that now-removed implicit discovery.

Fix: `maven-compiler-plugin` now declares both processors explicitly via
`<annotationProcessorPaths>` (with `annotationProcessorPathsUseDepMgmt`
so `spring-boot-configuration-processor`'s version stays managed by the
Spring Boot BOM). This is the correct, current way to wire annotation
processors regardless of which JDK is targeted — it was latent
technical debt that this migration happened to surface.

**This is the actual reason "just bump java.version" isn't safe without
compiling and running the real test suite on the real target JDK** — the
break here had nothing to do with library version compatibility tables and
everything to do with a javac behavior change that no dependency-audit
spreadsheet would have caught.

### Docker & CI
- `Dockerfile`: base image `amazoncorretto:21.0.11-alpine3.24` →
  `amazoncorretto:25.0.4-alpine3.24` (confirmed published tag).
- `docker-entrypoint.sh`: added `-XX:+UseCompactObjectHeaders`.
- `.github/workflows/ci.yml`, `build.yml`, `qodana_code_quality.yml`: Java
  matrix/version bumped `21` → `25`.

### JVM tuning — Compact Object Headers
[JEP 519](https://openjdk.org/jeps/519) made Compact Object Headers a
stable **product feature** in Java 25 (promoted from JDK 24's experimental
flag). It is not the default yet — that only happens by default starting in
JDK 27 (JEP 534) — but it's fully stable and opt-in via a single flag:
`-XX:+UseCompactObjectHeaders`, now set in `docker-entrypoint.sh`. Published
benchmarks (SPECjbb2015) show roughly 15–25% heap reduction and a modest CPU
reduction from shrinking the object header from 12–16 bytes down to 8. This
is the concrete instance of the ticket's "leverage Compact Object Headers"
objective — achieved on a fully-supported LTS release rather than on 27.

### Code modernization — stable features only
Explicitly **excluded**: Structured Concurrency and Primitive Type Pattern
Matching. Both remain **preview** APIs even in Java 27 (let alone 25), and
the ticket's own acceptance criteria rule out preview APIs without explicit
approval.

Applied: the unnamed-variable pattern (`_`, stable since Java 22) on 5 catch
blocks that provably never used their exception variable:
`MongoHealthConfig.java`, `NotificationEventConsumerServiceImpl.java`,
`VerificationCodeServiceImpl.java` (×2), `AuthServiceImpl.java`.

Evaluated and **not applied**, because no real candidates exist in this
codebase: Scoped Values (no `ThreadLocal` usage anywhere), Sequenced
Collections (no manual first/last-element access in `src/main/java`), and
record patterns (no record destructuring via `instanceof`/`switch`
anywhere). These were checked by direct codebase search, not assumed absent.

G1 has been the default garbage collector for years already — no GC change
was needed or made.

### Verification performed
- Full `mvn -Pcoverage clean verify` run on both JDK 21 (baseline) and JDK 25
  (post-migration): **562/562 tests pass on both**, and JaCoCo LINE/BRANCH
  counters are identical between the two runs (1452/2410 lines, 222/447
  branches covered) — zero coverage regression.
- PMD (7.17.0, via maven-pmd-plugin 3.28.0) passes clean under `targetJdk=25`.
- JMH benchmark profile (`mvn -Pbenchmark clean test`) was run as a smoke
  check; it surfaced a pre-existing `ClassNotFoundException:
  org.openjdk.jmh.runner.ForkedMain` in the forked-VM benchmark runner —
  confirmed present identically on JDK 21 before this migration, so it is
  **not** a regression introduced here and is out of this ticket's scope.

## What this migration does not cover

The ticket's Definition of Done includes items that need the team or CI
infrastructure, not a code change:
- Production-traffic performance-baseline comparison (no such baseline
  exists to compare against in this environment).
- SonarCloud scan review (`sonar.projectKey=umdc-mercury` — runs in CI).
- Staging deployment verification.
- Two-reviewer PR approval.

The JaCoCo coverage gate itself was **left at its currently-enforced 70%
line / 50% branch** (`pom.xml` jacoco-check rules). The ticket text cites
85%/80% as an example target, but that is not this repo's actual configured
gate — raising it is a separate, orthogonal decision from a JDK migration
and wasn't made silently as a side effect here.
