# Mercury Hooks — Index

All automation hooks for Mercury CI/CD quality gates.

| Hook | File | Trigger | Auto-Block | Agents |
|---|---|---|---|---|
| Pre-Pull-Request | `pre-pull-request.hook.md` | PR created/updated targeting `main` | ✅ Yes | code-reviewer, test-writer |
| Post-Implementation Review | `post-implementation-review.hook.md` | Developer signals implementation complete | ✅ Yes | code-reviewer, test-writer |
| Post-Merge Security | `post-merge-security.hook.md` | Successful merge to `main` | ❌ No (alert only) | security-reviewer, code-reviewer |
| Pre-Release Gate | `pre-release-gate.hook.md` | Release initiation via `prepare-release` prompt | ✅ Yes | code-reviewer, security-reviewer, devops-engineer |

## Hook → Prompt Cross-Reference

| Hook | Prompts Used |
|---|---|
| `pre-pull-request` | `fix-lint-violations.prompt.md`, `improve-coverage.prompt.md`, `review-code.prompt.md` |
| `post-implementation-review` | `fix-lint-violations.prompt.md`, `review-code.prompt.md`, `write-unit-tests.prompt.md`, `improve-coverage.prompt.md` |
| `post-merge-security` | `security-audit.prompt.md` |
| `pre-release-gate` | `security-audit.prompt.md`, `prepare-release.prompt.md` |

## Gate Hierarchy

```
Developer completes code
         ↓
[post-implementation-review]  ← Code quality gate (blocking)
         ↓ PASS
[pre-pull-request]            ← PR merge gate (blocking)
         ↓ PASS
PR merged to main
         ↓
[post-merge-security]         ← Security regression scan (alerting)
         ↓
[pre-release-gate]            ← Release quality gate (blocking)
         ↓ PASS
Release tag created
```

## Blocking Rules Summary

| Check | Blocks PR | Blocks Release |
|---|---|---|
| PMD violations > 0 | ✅ | ✅ |
| Build failure | ✅ | ✅ |
| Line coverage < 70% | ✅ | ✅ |
| Branch coverage < 50% | ✅ | ✅ |
| Architecture violations | ✅ | ✅ |
| Security BLOCK verdict | ❌ (advisory) | ✅ |
| SonarCloud FAILED | ❌ (advisory) | ✅ |
| CHANGELOG missing | ❌ | ✅ |
