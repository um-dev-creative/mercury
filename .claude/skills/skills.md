---
title: Mercury Skills Index
---

## Shared Skills

| File | Used By | Purpose |
|---|---|---|
| `api-design.skill.md` | developer, api-reviewer | REST conventions, OpenAPI patterns, DTO validation |
| `persistence-patterns.skill.md` | developer, database-architect | JPA/MongoDB patterns, DDL-none, mapper bridge |
| `kafka-messaging.skill.md` | developer, devops-engineer | Topic config, ChannelService contract, publish/consume patterns |

## Agent Skill Folders

| Agent | Folder | SKILL.md |
|---|---|---|
| Orchestrator | `orchestrator/` | `orchestrator/SKILL.md` |
| Developer | `developer/` | `developer/SKILL.md` |
| Test Writer | `test-writer/` | `test-writer/SKILL.md` |
| Code Reviewer | `code-reviewer/` | `code-reviewer/SKILL.md` |
| Security Reviewer | `security-reviewer/` | `security-reviewer/SKILL.md` |
| DevOps Engineer | `devops-engineer/` | `devops-engineer/SKILL.md` |
| API Reviewer | `api-reviewer/` | `api-reviewer/SKILL.md` |
| Database Architect | `database-architect/` | `database-architect/SKILL.md` |

## Key Mercury Patterns Covered

- **Controller/Api split** — `developer/SKILL.md`, `api-reviewer/SKILL.md`
- **Exception hierarchy** — `developer/SKILL.md`, `code-reviewer/SKILL.md`
- **EmailMessageDocument lifecycle** — `database-architect/SKILL.md`, `persistence-patterns.skill.md`
- **Vault-backed secrets** — `security-reviewer/SKILL.md`
- **JaCoCo thresholds** — `test-writer/SKILL.md`
- **PMD rules** — `code-reviewer/SKILL.md`
- **Kafka topics and auto-startup** — `kafka-messaging.skill.md`, `devops-engineer/SKILL.md`
