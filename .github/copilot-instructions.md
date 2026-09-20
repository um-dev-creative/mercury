# GitHub Copilot — Mercury Workspace Instructions

Mercury is a **Spring Boot 3.5.8 / Java 21** multi-channel messaging microservice.
Apply these conventions to every suggestion, completion, and agent action.

## Architecture

```
REST Controller (*Api interface + *Controller)
  └─> Service (*ServiceImpl, CompletableFuture for writes)
        ├─> JPA Repositories (PostgreSQL)  ─> MapStruct Mappers ─> *TO records
        ├─> MongoDB Repositories (EmailMessageDocument lifecycle)
        └─> KafkaTemplate (publish per-recipient to channel topics)

Kafka Listeners (MultiChannelListener)
  └─> MessageChannelRouter ─> ChannelService impls (Email / SMS / Telegram / WhatsApp)

SendEmailScheduler ─> MessageProcessor (OPENED → SENT → PostgreSQL → deleted)
```

## Build Commands

```bash
mvn -U clean package -DskipTests         # Fast compile
mvn clean test                            # PMD + JUnit 5
mvn -B -V -e clean verify                 # Full: PMD + tests + JaCoCo (70% line / 50% branch)
mvn -Pcoverage clean test                 # JaCoCo XML for SonarCloud
mvn -Dtest=FullyQualifiedClassName surefire:test  # Single test class
```

## Key Conventions

| Rule | Details |
|---|---|
| OpenAPI annotations | On `*Api` interfaces **only** — never on `*Controller` |
| DTOs | Java `record`s in `com.prx.mercury.api.v1.to` with `*Request`, `*Response`, `*TO` suffixes |
| Logging | `LoggerFactory.getLogger()` (SLF4J) only — never `System.out.println` |
| Async | Write operations return `CompletableFuture<T>`; read ops return `T` or `List<T>` |
| Scheduler rates | `${prx.scheduler.*}` property placeholders — never hardcoded milliseconds |
| Kafka topics | `${prx.consumer.topics.*}` from `bootstrap.yml` — never hardcoded strings |
| MapStruct | Inject mappers as Spring beans — never `new CampaignMapperImpl()` |
| Constructor | Every class needs at least one explicit constructor (PMD `AtLeastOneConstructor`) |
| Secrets | Vault-backed `${ENV_VAR}` references in `bootstrap.yml` — never in source |
| MongoDB | `EmailMessageDocument` lifecycle: OPENED → SENT → deleted (do not retain) |
| DDL | Strategy is `none` — schema changes require SQL in `src/main/resources/db/` |
| Auth failures | Throw `ForbiddenException` → HTTP 403 (not 401) |

## Exception → HTTP Mapping

| Exception | HTTP |
|---|---|
| `CampaignNotFoundException` | 404 |
| `ForbiddenException` | 403 |
| `IllegalStateException` (channel disabled) | 422 |
| `IllegalArgumentException` (bad input) | 400 |
| `MethodArgumentNotValidException` | 400 |

## Agent Roster

Use these agents via `.github/agents/` and `.github/prompts/`:

| Agent | Model | Use For |
|---|---|---|
| `orchestrator` | Claude Sonnet 4.6 | Full feature delivery |
| `developer` | Claude Sonnet 4.6 | Implementation |
| `test-writer` | Claude Sonnet 4.6 | JUnit 5 + coverage |
| `code-reviewer` | Gemma 4 27B | PMD, conventions |
| `security-reviewer` | Claude Sonnet 4.6 | CVE, secrets, auth |
| `devops-engineer` | Gemma 4 27B | Maven, Docker, CI |
| `api-reviewer` | Gemma 4 27B | OpenAPI contracts |
| `database-architect` | Claude Sonnet 4.6 | JPA schema, migrations |

## Prompts Quick Reference

| Task | Prompt |
|---|---|
| New endpoint | `.github/prompts/implement-feature.prompt.md` |
| Bug fix | `.github/prompts/fix-bug.prompt.md` |
| PMD failures | `.github/prompts/fix-lint-violations.prompt.md` |
| Write tests | `.github/prompts/write-unit-tests.prompt.md` |
| Coverage gap | `.github/prompts/improve-coverage.prompt.md` |
| PR review | `.github/prompts/review-code.prompt.md` |
| Security audit | `.github/prompts/security-audit.prompt.md` |
| Release | `.github/prompts/prepare-release.prompt.md` |
| Full feature | `.github/prompts/full-feature-delivery.prompt.md` |
