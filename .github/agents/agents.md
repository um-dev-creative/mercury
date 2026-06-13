# Mercury — GitHub Copilot Agent Registry

All agents in `.github/agents/`. Agents marked `subagent-only: true` are called by the orchestrator or other agents — they cannot be invoked directly by the user.

## Agent Roster

| File | Name | Model | User-Invocable | Purpose |
|---|---|---|---|---|
| `orchestrator.agent.md` | Orchestrator | Claude Sonnet 4.6 | Yes | Decomposes requests, delegates to specialists, tracks delivery |
| `developer.agent.md` | Developer | Claude Sonnet 4.6 | No | Implements features across all Mercury layers |
| `test-writer.agent.md` | QA / Test Writer | Claude Sonnet 4.6 | No | JUnit 5 + Mockito tests, JaCoCo coverage |
| `code-reviewer.agent.md` | Code Reviewer | Gemma 4 27B | No | PMD compliance, Spring Boot conventions, layer separation |
| `security-reviewer.agent.md` | Security Reviewer | Claude Sonnet 4.6 | No | CVE scanning, secrets audit, auth flow review |
| `devops-engineer.agent.md` | DevOps Engineer | Gemma 4 27B | No | Maven, Docker, CI/CD, Kafka config, release tagging |
| `api-reviewer.agent.md` | API Reviewer | Gemma 4 27B | No | OpenAPI contract, HTTP semantics, operationId consistency |
| `database-architect.agent.md` | Database Architect | Claude Sonnet 4.6 | No | JPA entities, MongoDB documents, SQL migrations, mappers |
| `repo-requirements-analyst.agent.md` | Repo Requirements Analyst | Claude Sonnet 4.6 | Yes | Discovers and documents requirements from the codebase |
| `product-owner.agent.md` | Product Owner | — | Yes | User story definition and acceptance criteria |
| `project-manager.agent.md` | Project Manager | — | Yes | Sprint planning and delivery coordination |
| `documentator.agent.md` | Documentator | — | No | Technical documentation generation |

## Delegation Map

```
User
 └─> orchestrator
       ├─> database-architect  (schema changes first)
       ├─> api-reviewer         (contract before implementation)
       ├─> developer            (implementation)
       ├─> test-writer          (coverage)
       ├─> code-reviewer        (quality gate)
       ├─> security-reviewer    (auth/secrets audit)
       └─> devops-engineer      (config, Docker, release)
```

## Model Assignment Rationale

- **Claude Sonnet 4.6** — complex reasoning: orchestration, security, schema design, requirements analysis
- **Gemma 4 27B** — pattern matching and structured tasks: code review, API contract checks, DevOps scripting
