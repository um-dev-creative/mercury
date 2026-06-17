# Mercury Skills — Index

All skill definition files for Mercury agents.

## Agent-Specific Skills

| Agent | Skill File | Key Patterns |
|---|---|---|
| Orchestrator | `orchestrator/SKILL.md` | Layer decomposition order, delegation routing, delivery plan format |
| Developer | `developer/SKILL.md` | REST/Service/Kafka/Mapper patterns, naming conventions, anti-patterns |
| Test Writer | `test-writer/SKILL.md` | JUnit 5 patterns, CompletableFuture testing, coverage targets |
| Code Reviewer | `code-reviewer/SKILL.md` | PMD rules, architecture checks, APPROVE/REQUEST_CHANGES criteria |
| Security Reviewer | `security-reviewer/SKILL.md` | Vault patterns, ForbiddenException flow, JWT handling, CVE checks |
| DevOps Engineer | `devops-engineer/SKILL.md` | bootstrap.yml structure, Docker, release workflow, Kafka governance |
| API Reviewer | `api-reviewer/SKILL.md` | OpenAPI annotation placement, DTO records, HTTP status codes |
| Database Architect | `database-architect/SKILL.md` | Flyway naming, JPA entity template, MongoDB lifecycle, entity inventory |

## Shared Skills

| Skill | File | Used By |
|---|---|---|
| API Design | `api-design.skill.md` | developer, api-reviewer |
| Persistence | `persistence.skill.md` | developer, database-architect |
| Release Process | `release-process.skill.md` | devops-engineer |

## Key Mercury Conventions (Summary)

| Convention | Rule |
|---|---|
| OpenAPI | On `*Api` interfaces ONLY |
| DTOs | Java `record`s in `com.umdc.mercury.api.v1.to` |
| Logging | SLF4J `LoggerFactory.getLogger()` only |
| Async | Write ops return `CompletableFuture<T>` |
| Kafka topics | `${prx.consumer.topics.*}` — never literals |
| Scheduler rates | `${prx.scheduler.*}` — never literals |
| MapStruct | Spring beans — never `new *MapperImpl()` |
| Constructors | Every class needs explicit constructor (PMD) |
| Secrets | Vault-backed `${ENV_VAR}` — never in source |
| DDL | Flyway only — `ddl-auto: none` always |
