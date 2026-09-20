---
title: Mercury Prompts Index
---

## Prompt Registry

| File | Agent | Mode | Trigger / Use Case |
|---|---|---|---|
| `implement-feature.prompt.md` | developer | agent | Implementing a new REST endpoint across all Mercury layers |
| `fix-bug.prompt.md` | developer | agent | Diagnosing and fixing a defect in any Mercury layer |
| `fix-lint-violations.prompt.md` | developer, code-reviewer | agent | Resolving PMD violations blocking `mvn clean test` |
| `write-unit-tests.prompt.md` | test-writer | agent | Writing JUnit 5 tests for a specific class |
| `improve-coverage.prompt.md` | test-writer | agent | Adding tests to meet 70% line / 50% branch JaCoCo thresholds |
| `review-code.prompt.md` | code-reviewer | ask | Reviewing a PR or diff for PMD + convention compliance |
| `security-audit.prompt.md` | security-reviewer | agent | Auditing auth, secrets, and OWASP Top-10 in Mercury changes |
| `prepare-release.prompt.md` | devops-engineer | agent | Full release preparation: version bump, CHANGELOG, tag, Docker |
| `review-api-contract.prompt.md` | api-reviewer | ask | Reviewing `*Api` interface for OpenAPI completeness and correctness |
| `full-feature-delivery.prompt.md` | orchestrator | agent | End-to-end feature delivery orchestrating all agents in sequence |

## Quick Reference

### For a new feature
Run: `full-feature-delivery.prompt.md` via orchestrator

### For a bug fix
Run: `fix-bug.prompt.md` via developer

### For a PR review
Run: `review-code.prompt.md` via code-reviewer + `review-api-contract.prompt.md` if API changed

### For a failing build (PMD)
Run: `fix-lint-violations.prompt.md` via developer

### For a failing build (JaCoCo)
Run: `improve-coverage.prompt.md` via test-writer

### For a release
Run: `prepare-release.prompt.md` via devops-engineer (will invoke `pre-release-gate.hook.md` internally)
