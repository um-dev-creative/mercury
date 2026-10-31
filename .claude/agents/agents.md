---
title: Mercury Agent Index
---

## Agent Registry

| File | Agent | User-Invocable | Subagent-Only | Purpose |
|---|---|---|---|---|
| `orchestrator.agent.md` | Orchestrator | true | false | Coordinates multi-agent feature delivery workflows |
| `developer.agent.md` | Developer | true | false | Implements features across all Mercury service layers |
| `test-writer.agent.md` | Test Writer | true | false | Writes JUnit 5 tests, enforces JaCoCo coverage thresholds |
| `code-reviewer.agent.md` | Code Reviewer | true | false | Reviews diffs for PMD compliance and Mercury conventions |
| `security-reviewer.agent.md` | Security Reviewer | true | false | Audits auth, JWT, Vault secrets, and OWASP Top-10 |
| `devops-engineer.agent.md` | DevOps Engineer | true | false | Manages build pipeline, Kafka, Docker, releases |
| `api-reviewer.agent.md` | API Reviewer | true | false | Reviews REST contracts, OpenAPI annotations, DTO validation |
| `database-architect.agent.md` | Database Architect | true | false | Designs JPA entities, MongoDB documents, SQL migrations |

## Agent Skill Definitions

| Agent | Skill File |
|---|---|
| Orchestrator | `.claude/skills/orchestrator/SKILL.md` |
| Developer | `.claude/skills/developer/SKILL.md` |
| Test Writer | `.claude/skills/test-writer/SKILL.md` |
| Code Reviewer | `.claude/skills/code-reviewer/SKILL.md` |
| Security Reviewer | `.claude/skills/security-reviewer/SKILL.md` |
| DevOps Engineer | `.claude/skills/devops-engineer/SKILL.md` |
| API Reviewer | `.claude/skills/api-reviewer/SKILL.md` |
| Database Architect | `.claude/skills/database-architect/SKILL.md` |

## Typical Invocation Order for Feature Delivery

1. Orchestrator (plan)
2. Database Architect (if schema change needed)
3. API Reviewer (contract definition)
4. Developer (implementation)
5. Test Writer (unit tests + coverage)
6. Code Reviewer (PMD + conventions)
7. Security Reviewer (if user-scoped endpoint)
8. DevOps Engineer (PR + release)
