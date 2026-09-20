---
agent: orchestrator
version: 1.0
---

## 1. Project-Specific Patterns

Mercury follows a strict layered delivery order. A campaign feature delivery example:

1. `database-architect` — new entity + SQL migration
2. `developer` — `*Api` interface + `*Controller` + `*ServiceImpl` + mapper
3. `test-writer` — unit tests for service and mapper
4. `api-reviewer` — OpenAPI contract review
5. `code-reviewer` — PMD + convention review
6. `security-reviewer` — auth/token review if endpoint is user-scoped
7. `devops-engineer` — env var docs + release if applicable

Kafka-related features additionally require `devops-engineer` to confirm topic names match `bootstrap.yml` (`email-topic`, `sms-topic`, `telegram-topic`).

## 2. Naming Conventions

- Feature branches: `feature/<issue-id>-<short-description>` (e.g., `feature/ds-171-toggle-campaign`)
- Agent delegation messages: prefix with agent name (e.g., `[developer] Implement PATCH /api/v1/campaigns/{id}/toggle`)
- Sub-task IDs referenced in commit messages (e.g., `feat: add PATCH /api/v1/campaigns/{id}/toggle endpoint`)

## 3. Error Handling

- If a sub-agent returns REQUEST_CHANGES or FAIL, halt subsequent steps and report blocking issues
- PMD failure at `mvn clean test` blocks ALL subsequent steps
- JaCoCo failure at `mvn verify` blocks `devops-engineer` release step
- Build failure (`mvn -U clean package -DskipTests`) blocks everything

## 4. Key Files

- `pom.xml` — version, dependency management
- `src/main/resources/bootstrap.yml` — runtime config and Kafka toggles
- `ruleset.xml` — PMD rules enforced at test phase
- `CHANGELOG` — updated by `devops-engineer` on release

## 5. Constraints

- Never skip PMD or JaCoCo gates
- Never merge directly to `main` without a PR
- Private PRX dependencies require `~/.m2/settings.xml` with `https://repo.repsy.io/mvn/lmata/prx` credentials

## 6. Checklist

- [ ] All affected layers identified
- [ ] Sub-agents assigned in correct dependency order
- [ ] Each sub-agent returned APPROVED or pass
- [ ] `mvn -U clean package -DskipTests` passes
- [ ] `mvn clean verify` passes (PMD + JaCoCo)
- [ ] PR created targeting `main` or `develop`
