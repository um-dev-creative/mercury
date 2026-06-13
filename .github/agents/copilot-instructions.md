# Copilot Instructions for Mercury

## Build, Test, and Lint Commands
- `mvn -U clean package -DskipTests` – Fast path to confirm the project compiles without running tests (README.md).
- `mvn clean test` – Default unit-test suite with Surefire/JUnit 5 (README.md).
- `mvn -U clean package` – Full build (tests + shaded JAR in `target/mercury.jar`) used before Docker packaging (README.md, Dockerfile).
- `mvn -Pcoverage clean test` – Runs the coverage profile that JaCoCo/Sonar expect (README.md, README_BUILD.md).
- `mvn -U -DskipITs clean verify` – Generates the JaCoCo XML consumed by SonarCloud quality gates (README.md L120-L129).
- `mvn -B -V -e clean verify` – CI baseline invoked in `.github/workflows/ci.yml`.
- `mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=umdc-mercury` – SonarCloud analysis command from `.github/workflows/build.yml`.
- `mvn -Dtest="com.prx.mercury.api.v1.service.EmailServiceImplTest,com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest,com.prx.mercury.api.v1.to.VerificationCodeRequestTest,com.prx.mercury.mapper.VerificationCodeMapperTest,com.prx.mercury.mapper.TemplateMapperTest,com.prx.mercury.mapper.MessageRecordMapperTest" surefire:test -DtrimStackTrace=false` – Focused Surefire batch to debug without triggering PMD (README_BUILD.md).
- `mvn -Dtest=com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest surefire:test` – Run a single test class; append `#methodName` for method-level runs per Surefire (README_BUILD.md).
- `mvn dependency:tree -DoutputFile=dependency-tree.txt -DoutputType=text` – Audit dependency graph when addressing CVEs (README.md, README_BUILD.md).
- `docker build -t prx/mercury:latest . && docker run --rm -p 8118:8118 prx/mercury:latest` – Container workflow that mirrors CI releases (README.md, Dockerfile).
- `qodana scan --config qodana.yaml` – Reproduce the JetBrains Qodana check used in `.github/workflows/qodana_code_quality.yml` (requires Qodana CLI and `qodana-jvm-community:2025.3` image).

## High-Level Architecture
- **Entry point & cross-cutting config:** `src/main/java/com/prx/mercury/MercuryApplication.java` boots Spring Boot 3.5.8, enables Feign clients (`com.prx.mercury.client`), scheduling, and scans commons/security packages so shared beans are picked up.
- **API layer:** REST controllers live in `src/main/java/com/prx/mercury/api/v1/controller`, each implementing a `*Api` interface to centralize request mappings and OpenAPI annotations (e.g., `CampaignController` + `CampaignApi`).
- **Service layer:** Business logic resides under `api/v1/service`, usually returning `CompletableFuture` instances so controllers can respond with ACCEPTED while work completes; services orchestrate repositories plus Kafka/email processors (`CampaignServiceImpl`, `VerificationCodeServiceImpl`).
- **Persistence/mapping:** SQL entities and repositories are in `jpa/sql/**`, Mongo documents/repositories in `jpa/nosql/**`, and MapStruct mappers under `mapper/*` bridge entities ↔ DTO/TO records (e.g., `MessageRecordMapper`, `TemplateMapper`).
- **Messaging & schedulers:** Kafka listeners (`kafka/listener/*`) and channel services (`kafka/consumer/service/*`) enqueue work; `scheduler/SendEmailScheduler` triggers `processor/MessageProcessor` to send and reconcile messages, using `DeliveryStatusType` transitions plus verification-code hooks.
- **Configuration & secrets:** `src/main/resources/bootstrap.yml` wires Spring Cloud Config + Vault, TLS bundles, Kafka topics, and DB credentials; `default.env` supplies sample values but currently contains real secrets that must be treated as placeholders only.
- **Tooling:** Static analysis is enforced via `ruleset.xml`, SonarCloud workflows (`.github/workflows/build.yml`, `ci.yml`), and JetBrains Qodana (`qodana.yaml`, `.github/workflows/qodana_code_quality.yml`).

## Key Conventions
- **Layered flow:** Controllers → Services → Repositories → Mappers; keep controller classes thin by delegating to services and reusing `api/v1/to` records (see `CampaignController` + `CampaignService`).
- **DTO naming/location:** Transfer objects live under `api/v1/to` and follow `*Request`, `*Response`, `*TO` suffixes with Bean Validation annotations applied at record definition time.
- **Messaging contracts:** Channel services (`kafka/consumer/service/*`) must implement `send`, `updateStatus`, and `findByDeliveryStatus`; empty stubs signal pending work (Email/Telegram services).
- **Schedulers:** `@Scheduled` cadences belong in configuration properties—existing hardcoded `fixedRate=1500` values in `SendEmailScheduler` contradict comment expectations, so prefer `${scheduler.*}` placeholders when adding jobs.
- **Configuration hygiene:** Never commit live secrets; rotate and replace the real `VAULT_TOKEN` currently present in `default.env` with documented placeholders, and rely on Spring Cloud Config/Vault lookups defined in `bootstrap.yml`.
- **Testing style:** Use `mvn -Dtest=<Class>[#method] surefire:test` for targeted runs, `-Pcoverage` for JaCoCo, and keep new tests under `src/test/java` mirroring package names of the code they cover; Mockito + JUnit 5 is the default stack.
- **Logging & async patterns:** Keep SLF4J `LoggerFactory` usage and `CompletableFuture` chaining consistent with existing services; avoid introducing other logging frameworks.
- **AI assistant coordination:** This repository already defines `.github/agents/repo-requirements-analyst.agent.md`; defer to that file for requirements-analysis workflows and use this document strictly for Copilot’s command/architecture/convention context.
