---
title: Mercury Agent Infrastructure — Master Index
project: mercury
framework: Spring Boot 3.5.8 / Java 21
build: Maven
version: 1.0
---

## Overview

Mercury is a multi-channel messaging microservice. This agent infrastructure provides specialized agents, skills, tools, prompts, and hooks to support feature delivery, testing, code review, security auditing, and release management.

## Directory Structure

```
.claude/
├── copilot-agents.md              ← this file (master index)
├── agents/
│   ├── agents.md                  ← agent registry table
│   ├── orchestrator.agent.md
│   ├── developer.agent.md
│   ├── test-writer.agent.md
│   ├── code-reviewer.agent.md
│   ├── security-reviewer.agent.md
│   ├── devops-engineer.agent.md
│   ├── api-reviewer.agent.md
│   └── database-architect.agent.md
├── skills/
│   ├── skills.md                  ← skills registry table
│   ├── api-design.skill.md        ← shared: developer + api-reviewer
│   ├── persistence-patterns.skill.md  ← shared: developer + database-architect
│   ├── kafka-messaging.skill.md   ← shared: developer + devops-engineer
│   ├── orchestrator/SKILL.md
│   ├── developer/SKILL.md
│   ├── test-writer/SKILL.md
│   ├── code-reviewer/SKILL.md
│   ├── security-reviewer/SKILL.md
│   ├── devops-engineer/SKILL.md
│   ├── api-reviewer/SKILL.md
│   └── database-architect/SKILL.md
├── tools/
│   ├── tools.md                   ← tools registry table
│   ├── maven.tool.md
│   ├── git.tool.md
│   ├── pmd.tool.md
│   ├── jacoco.tool.md
│   ├── docker-build.tool.md
│   └── kafka-cli.tool.md
├── prompts/
│   ├── prompts.md                 ← prompts registry table
│   ├── implement-feature.prompt.md
│   ├── fix-bug.prompt.md
│   ├── fix-lint-violations.prompt.md
│   ├── write-unit-tests.prompt.md
│   ├── improve-coverage.prompt.md
│   ├── review-code.prompt.md
│   ├── security-audit.prompt.md
│   ├── prepare-release.prompt.md
│   ├── review-api-contract.prompt.md
│   └── full-feature-delivery.prompt.md
└── hooks/
    ├── hooks.md                   ← hooks registry table
    ├── pre-pull-request.hook.md
    ├── post-implementation-review.hook.md
    ├── post-merge-security.hook.md
    └── pre-release-gate.hook.md
```

## Quick-Start Guide

### Deliver a new feature end-to-end
```
Use prompt: .claude/prompts/full-feature-delivery.prompt.md
Agent: orchestrator
Variables: featureName, issueId, httpMethod, endpointPath, channelScope, requiresNewTable, isUserScoped
```

### Implement a specific layer only
```
Use prompt: .claude/prompts/implement-feature.prompt.md
Agent: developer
```

### Fix a failing PMD build
```
Use prompt: .claude/prompts/fix-lint-violations.prompt.md
Agent: developer
```

### Fix failing JaCoCo coverage
```
Use prompt: .claude/prompts/improve-coverage.prompt.md
Agent: test-writer
```

### Review a PR
```
Use prompt: .claude/prompts/review-code.prompt.md
Agent: code-reviewer
If API changed: also .claude/prompts/review-api-contract.prompt.md
```

### Run a security audit
```
Use prompt: .claude/prompts/security-audit.prompt.md
Agent: security-reviewer
```

### Cut a release
```
Use prompt: .claude/prompts/prepare-release.prompt.md
Agent: devops-engineer
Gate: .claude/hooks/pre-release-gate.hook.md (blocking)
```

## Mercury-Specific Invariants

These rules apply across ALL agents and prompts:

1. OpenAPI annotations (`@Operation`, `@ApiResponse`) belong on `*Api` interfaces — NEVER on `*Controller` classes
2. Scheduler fixed rates use `${prx.scheduler.*}` property placeholders — never hardcoded milliseconds
3. All secrets are Vault-backed `${ENV_VAR}` placeholders in `bootstrap.yml` — never in source
4. `EmailMessageDocument` lifecycle must remain: OPENED → SENT → deleted (do not break)
5. `PRX_KAFKA_AUTO_STARTUP=false` stays as default in `bootstrap.yml`
6. Every class needs an explicit constructor (PMD `AtLeastOneConstructor`)
7. SLF4J `LoggerFactory.getLogger()` is the only permitted logging mechanism
8. MapStruct mappers are Spring beans — never instantiated with `new`
9. DDL strategy is `none` — schema changes require SQL migration scripts in `src/main/resources/db/`
10. `ForbiddenException` → 403 is the auth failure pattern (Mercury does not use 401)
