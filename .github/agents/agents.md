# Mercury Agents — Index

All specialist agents available for Mercury development tasks.

| Agent | File | User-Invocable | Role |
|---|---|---|---|
| Orchestrator | `orchestrator.agent.md` | ✅ | Decomposes features, delegates to specialists, tracks dependencies |
| Developer | `developer.agent.md` | ✅ | Implements all Java source: REST, service, JPA, Kafka, mapper, scheduler |
| Test Writer | `test-writer.agent.md` | ✅ | Writes JUnit 5 + Mockito tests, enforces JaCoCo thresholds |
| Code Reviewer | `code-reviewer.agent.md` | ✅ | PMD compliance, architecture conventions, APPROVE/REQUEST_CHANGES verdict |
| Security Reviewer | `security-reviewer.agent.md` | ✅ | Auth, JWT, Vault, ForbiddenException, CVE scan, APPROVE/BLOCK verdict |
| DevOps Engineer | `devops-engineer.agent.md` | ✅ | bootstrap.yml, Dockerfile, CI/CD, release tags, Kafka/Vault config |
| API Reviewer | `api-reviewer.agent.md` | ✅ | OpenAPI contract on `*Api` interfaces, DTO records, HTTP status codes |
| Database Architect | `database-architect.agent.md` | ✅ | Flyway migrations, JPA entities, MongoDB documents, schema design |

## Delegation Order (Feature Delivery)
```
1. database-architect  — schema first
2. api-reviewer        — contract second
3. developer           — implement third
4. test-writer         — tests fourth
5. code-reviewer       — review fifth
6. security-reviewer   — security (if auth-sensitive)
7. devops-engineer     — config/release (if needed)
```

## Skill Definitions
Each agent has a detailed skill file at `.github/skills/<agent-name>/SKILL.md`.

## Tools Available
See `.github/tools/tools.md` for the complete tool index.
