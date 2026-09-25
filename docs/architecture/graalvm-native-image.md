# MER-5: GraalVM Native Image for Mercury

## Objective and result

The ticket asked for a Java 25 + GraalVM Native Image build to cut startup
time and RSS footprint. Java 25 was already the runtime target as of MER-4.
This covers the native-image work: what was added, what broke and why
(all found empirically — the real forked-process command was extracted and
run directly, with `-Ddebug=true -Dlogging.file.name=...` to defeat AOT's
own log suppression, whenever a failure needed a real root cause instead of
a guess), what was fixed vs. deliberately left alone, and the measured
result.

**Bottom line: the full production application — Tomcat, Hibernate/JPA,
MongoDB, Kafka, Spring Security/JWT/OAuth2, the real bean graph, no
shortcuts — compiles to a native executable with `--no-fallback` and boots
successfully**, serving a real authenticated HTTPS request end to end.

## What was added

- **`pom.xml`**: a `native` Maven profile (activates the `native`/`nativeTest`
  plumbing `spring-boot-starter-parent` already `pluginManagement`s) with
  `--no-fallback` on `native-maven-plugin`'s build args, so a build that
  can't produce a true native binary fails loudly instead of silently
  falling back to a JVM-mode jar disguised as a native image.
- **`config/native.env`**: safe placeholder values for the ~46 undefaulted
  `${VAR}` properties in `application.yml` that are normally supplied by
  Vault/Config Server at runtime. `spring-boot:process-aot` (and the native
  binary itself) require a full `ApplicationContext` refresh at build time —
  there is no mocking around this — and this build has no access to (and
  should not depend on) live QA infrastructure. See the file's own header
  comment for the full rationale and every property's purpose.
- **`src/main/java/com/umdc/mercury/config/ThirdPartyNativeRuntimeHints.java`**:
  GraalVM reflection/proxy registrations for third-party code this project
  depends on but doesn't control (§ Reflection gaps below).
- **`Dockerfile.native`**: a two-stage build —
  `ghcr.io/graalvm/native-image-community:25` compiles the binary,
  `gcr.io/distroless/cc-debian12:nonroot` runs it (not the smaller
  `base-debian12` — this build is dynamically linked against glibc *and*
  libstdc++, not "mostly static"). Maven repo credentials are passed via a
  BuildKit secret mount (`--secret id=maven_settings`), never `COPY`'d into
  a layer. Three things the plain "GraalVM builder + native-image" mental
  model misses, all found by actually running the build rather than assuming
  it would work: (1) the builder image ships the JDK and `native-image` only
  — no Maven — installed here from a pinned `archive.apache.org` tarball
  (not `dlcdn.apache.org`, which only mirrors the latest 3.9.x and 404s once
  a pinned older version rolls off); (2) `ruleset.xml` (referenced by the PMD
  plugin via `${project.basedir}`) has to be `COPY`'d alongside `pom.xml` —
  easy to miss since it's not under `src/`; (3) `config/native.env` has to be
  sourced *inside* the build (`COPY`'d in, then `. ./config/native.env`
  before `mvn`) for exactly the same reason it's needed locally — the
  container has no more access to Vault/Config Server than this host does.
  **Not fully verified end-to-end**: `docker build` on `Dockerfile.native` was run repeatedly to
  find and fix the three issues above (each confirmed by actually building, not inspection), but
  the final full build was twice killed by this host's own memory-pressure protection before
  completing `native-image` — competing with everything else already running on this machine, not
  a flaw in the Dockerfile. The OrbStack VM itself has 15.66GB allocated, comfortably above the
  ~7–9GB peak RSS `native-image` used in the (successful, repeated) direct-on-host builds, so this
  is a "free up host RAM or build in CI" problem, not a build-logic problem. The exact same
  `mvn -Pnative clean package native:compile` invocation this Dockerfile runs is the one already
  proven, multiple times, directly on this host. Recommend one confirming `docker build` run in CI
  or on a quieter machine before treating the Docker image itself as proven.
- **`README.md`**: a "Build and run as a GraalVM Native Image" section
  covering both the local CLI flow and the Docker flow.

## The real blocker: legacy Spring Cloud Bootstrap vs. AOT

The single largest issue, and the reason this took multiple rebuild cycles
to resolve, had nothing to do with native-image mechanics — it was
architectural.

**Symptom:** `spring-boot:process-aot` completed "successfully" but
generated almost nothing (19 files, all named `Bootstrap*`/`*Config*` from
`org.springframework.cloud.bootstrap`) — no Tomcat, no JPA, no Kafka, no
Mongo, no security. The resulting native binary crashed immediately with
`No qualifying bean of type 'ServletWebServerFactory'`.

**Root cause, confirmed by extracting and running the actual forked AOT
process directly:** this app used Spring Cloud's **legacy Bootstrap
Context** (`spring-cloud-starter-bootstrap`, `spring.cloud.bootstrap.enabled`)
to fetch config from Vault/Config Server. That mechanism works by running
its *own* separate, nested `SpringApplication` — a tiny bootstrap-only
context — *before* the real `MercuryApplication` context ever starts.
Spring Boot's AOT processor (`SpringApplicationAotProcessor`) captures
whichever context is created first, so it captured that throwaway bootstrap
context instead of the real one, generated code for it, and stopped — with
no error, because from its perspective a context genuinely did refresh
successfully. Setting `spring.cloud.bootstrap.enabled=false` (as a shell env
var, and later forced as a JVM system property directly on `process-aot`'s
own forked process to rule out env-var propagation as the culprit) had zero
effect: the nested context creation is structural, not gated by that flag
the way the fetch-from-Config-Server behavior is.

**Fix: migrated off the legacy mechanism entirely.**
- `src/main/resources/bootstrap.yml` → renamed to `application.yml`, with
  `spring.config.import: "optional:vault://,optional:configserver:"` added
  (the modern `ConfigDataLoader` mechanism). All existing
  `spring.cloud.vault.*` / `spring.cloud.config.*` properties (uri, token,
  kv backend, TLS trust-stores, profile, label) carry over unchanged — they
  configure the same underlying client classes either way.
  `spring.cloud.bootstrap.enabled` and its `SPRING_BOOT_CLOUD_BOOTSTRAP_ENABLED`
  env var were removed as meaningless.
- `spring-cloud-starter-bootstrap` removed from `pom.xml` — with it gone,
  the legacy nested-context mechanism can't activate at all, regardless of
  any property.
- `ConfigurationPlaceholderTest` (a regression test asserting every
  undefaulted `${VAR}` placeholder in Java sources resolves against the
  config YAML) updated to read `application.yml` instead of `bootstrap.yml`.
- Verified with the full JVM-mode test suite (562/562 passing, unchanged)
  and a real JVM-mode boot (`spring-boot:run`, real HTTPS 401 from
  `/actuator/health`) both before and after, to confirm the migration is
  behaviorally transparent outside of AOT/native builds.
- **After this fix, `process-aot` correctly generated 360+ files** including
  `TomcatServletWebServerAutoConfiguration__BeanDefinitions.java` and the
  rest of the real application's bean graph.

This is a structural fact worth restating for whoever deploys this: the
*real* production Vault/Config Server fetch path (`VAULT_ENABLED=true`) has
only been proven correct in JVM mode (unchanged behavior, verified). The
`optional:vault://,optional:configserver:` import path itself has only been
exercised here with Vault/Config Server *disabled* — there was no access to
live Vault/Config Server infrastructure from this environment to verify the
live-fetch path end-to-end. Recommend one real staging boot with
`VAULT_ENABLED=true` before treating this as fully proven.

## A separate, genuine upstream bug found along the way

`spring-boot:process-aot` also failed to compile its own generated sources
with `jakarta.validation.Constraint is not a repeatable annotation
interface`, for every JPA repository method with a Bean Validation
annotation on a parameter (`findByName(@Size(max=120) @NotNull String
name)`, etc.). This is a confirmed, known upstream regression
([spring-projects/spring-data-commons#3499](https://github.com/spring-projects/spring-data-commons/issues/3499),
regressed between Spring Data 4.0.3 and 4.0.6+; this project is on 4.1.1).
Worked around via `spring.aot.repositories.enabled=false`
(`SPRING_AOT_REPOSITORIES_ENABLED=false` in `config/native.env`) — Spring
Data's own documented escape hatch, which falls back to normal runtime
repository proxies (still fully native-image compatible, just without that
one AOT optimization) until upstream ships a fix. Not related to native
image or the bootstrap migration; would reproduce identically on a plain
AOT-enabled JVM run.

## Reflection/proxy gaps found and fixed

None of these were guessed — each is the *exact* class/type GraalVM's own
`MissingReflectionRegistrationError` named, found by running the binary and
reading the crash. All three are registered in
`ThirdPartyNativeRuntimeHints.java`:

1. **`org.springframework.cloud.vault.config.VaultProperties`** —
   `NoSuchMethodException: VaultProperties.<init>()` despite a public no-arg
   constructor existing. `spring-cloud-vault-config` ships no GraalVM
   reachability metadata of its own (confirmed: no `META-INF/native-image`
   entry in its jar) and this particular `@ConfigurationProperties` binding
   isn't covered by Spring's AOT-generated code, so it falls back to plain
   reflection that native-image never learned to allow.
2. **`java.util.UUID[]`** — Hibernate's multi-ID batch loader
   (`MultiIdEntityLoaderArrayParam`) reflectively allocates a `UUID[]` for
   every UUID-keyed entity in this schema. GraalVM's error message named the
   exact fix (`{"type": "java.util.UUID[]"}`); registered as a Spring
   `RuntimeHints` type instead of a raw `reachability-metadata.json`, for
   consistency with the other two hints.
3. **`com.umdc.security.config.SecurityConfig` and its property tree
   (`SecurityProperties`, `AuthProperties`, `ClientProperties`,
   `StoreProperties`, `ManagementAuthenticatorProperties`) plus
   `KeystoreUtil`, and a JDK dynamic proxy for
   `jakarta.servlet.http.HttpServletRequest`** — the subtlest of the three.
   This one did **not** throw GraalVM's usual loud
   `MissingReflectionRegistrationError`; it surfaced as a plain
   `NullPointerException` inside `KeystoreUtil.getKeyStore` (a private,
   closed-source dependency — `com.umdc:security-oauth`, no source available
   in this repo). Confirmed native/AOT-specific (not a real bug) by running
   the identical config through a plain, non-AOT JVM boot first, which
   worked correctly end-to-end. The most likely explanation: `SecurityConfig`
   field-injects `SecurityProperties`, and native-image's closed-world
   reflection silently left that field unpopulated (null) under the
   AOT-generated bean-instantiation path, rather than failing loudly, because
   this whole dependency ships no native-image metadata either. Registering
   full reflective access (constructors, methods, fields) for the class tree
   fixed it. The `HttpServletRequest` proxy hint was a separate, second issue
   in the same investigation: `com.umdc.commons-services`'
   `requestBodyInterceptor` bean autowires a request-scoped
   `HttpServletRequest` into a singleton, which Spring resolves via a JDK
   dynamic proxy that also needs explicit native-image registration.

The GraalVM tracing agent (`-agentlib:native-image-agent`) that the ticket's
technical notes mention was not needed — every failure above was fully
diagnosable from GraalVM's own error output (or, for the one silent
NullPointerException, by isolating AOT as the variable via a same-config
plain-JVM run) without it. If a reflection gap surfaces later in a code path
this smoke test didn't reach — Telegram bot webhooks, or Apache POI (a
declared dependency, confirmed unused anywhere in `src/main/java`) — the
tracing agent is the documented next step.

## Testing & verification

**Not done, and why:** running the existing 562-test unit suite against the
native image (`mvn -Pnative test`, the `nativeTest` profile). Mockito's
default inline mock maker — used throughout this test suite — does not
support native-image test execution
([mockito/mockito#2435](https://github.com/mockito/mockito/issues/2435));
Mockito's own guidance for GraalVM is to switch to the subclass mock maker,
which cannot mock static methods or certain final types and would require
auditing and likely rewriting a meaningful fraction of the suite — a large,
separate effort with real behavioral trade-offs, not something to take on
silently inside a runtime-migration ticket. The `nativeTest` Maven profile
is wired (per Spring Boot's own convention) for completeness, but **the JVM
test suite remains the correctness gate** (`mvn clean test` /
`mvn -Pcoverage clean verify`, unchanged, still 562/562 passing after every
change in this ticket, including the bootstrap-to-config-import migration).

**What was done instead:** a runtime smoke test of the actual compiled
native binary — start it with `config/native.env`, confirm it serves a real
HTTPS request end to end (`401` from `/actuator/health`, meaning Tomcat,
security filter chain, and JWT decoder are all live), confirm clean startup
with no crash, measure cold-start time and RSS, and compare against a JVM
baseline built from the exact same jar.

### Measured results

Apple M4, 32GB RAM, macOS. Both modes run from the identical AOT-processed
`target/mercury.jar` build; native additionally compiled via
`mvn -Pnative native:compile`.

| Metric | JVM mode (JDK 25) | Native image | Improvement |
|---|---|---|---|
| Startup (`Started MercuryApplication in ...`, self-reported) | 9.964 s | **1.374 s** | ~7.3× faster |
| RSS during smoke test | ~525 MB (537,584 KB) | ~252 MB (257,664 KB) | ~2.1× smaller |
| `native-image` compile time | n/a | ~2–4 min per build | — |
| Native executable size | — | 327 MB | — |

**On the ticket's "&lt;1 second cold start" target: not quite met (1.37s),
and worth saying plainly why** rather than rounding favorably. This app
carries substantial real auto-configuration — Hibernate building a full
`SessionFactory` against a real dialect, Kafka producer/consumer factories,
MongoDB client setup, a full Spring Security filter chain with JWT decoding
— and native image does not make that per-bean initialization work
disappear, it only removes JIT warmup and classloading overhead around it.
The commonly-cited "tens of milliseconds" native Spring Boot startup numbers
come from trivial demo apps with a handful of beans; 1.37s for an app this
size, next to 9.96s for the identical code on a warm JVM, is the accurate
comparison. Reaching sub-1s would mean deferring or lazily initializing some
of that auto-configuration (e.g. `spring.jpa.open-in-view`-style
lazy-Hibernate strategies) — a real optimization, but a separate piece of
work from "make native image work," and not attempted here to avoid
changing runtime behavior as a side effect of a build-tooling ticket.

## Non-goals / explicitly out of scope for this ticket

- Verifying the real Vault/Config Server fetch path
  (`spring.config.import=vault://,configserver:` with real credentials)
  end-to-end — no access to that live infrastructure from this environment.
  Flagged above as the one thing to verify before calling this
  production-ready.
- Fixing the upstream Spring Data AOT-repository bug
  (spring-data-commons#3499) — worked around, not patched; revisit when
  upstream ships a fix.
- Further shrinking cold-start below 1s via lazy initialization — a genuine
  follow-on optimization, deliberately not bundled into this ticket.
- Auditing Apache POI or `telegrambots`/Javalin for native-image reflection
  issues beyond what automatic AOT processing already covers — neither is
  exercised by the code paths this smoke test reached (POI isn't imported
  anywhere in `src/main/java` at all).
- Raising the JaCoCo coverage gate, converting the test suite to a
  native-image-compatible mock maker, or any change not directly required
  to produce and verify a working native build.
