---
title: Mercury Hooks Index
---

## Hook Registry

| File | Trigger | Blocking | Agents | Purpose |
|---|---|---|---|---|
| `pre-pull-request.hook.md` | PR opened to `main`/`develop` | true | code-reviewer, test-writer | Full gate: build + PMD + JaCoCo + code review |
| `post-implementation-review.hook.md` | Push to `feature/*` branch | false | code-reviewer | Early warning: compile + PMD quick scan + missing tests |
| `post-merge-security.hook.md` | Merge to `develop` with deps/config changes | false | security-reviewer | Secret scan + auth review + env var docs check |
| `pre-release-gate.hook.md` | Before release tag creation | true | devops-engineer, security-reviewer, code-reviewer | Full release gate: build + PMD + JaCoCo + security + docs |

## Hook Dependency on Prompts

| Hook | Prompts Invoked |
|---|---|
| `pre-pull-request.hook.md` | `review-code.prompt.md`, `review-api-contract.prompt.md`, `improve-coverage.prompt.md` |
| `post-implementation-review.hook.md` | `review-code.prompt.md` (quick depth) |
| `post-merge-security.hook.md` | `security-audit.prompt.md` |
| `pre-release-gate.hook.md` | `security-audit.prompt.md`, `prepare-release.prompt.md` |

## Blocking vs Non-Blocking

- **Blocking hooks** (`auto-block: true`) — prevent the triggering action (PR merge, tag creation) from proceeding
- **Non-blocking hooks** (`auto-block: false`) — surface warnings and recommendations without preventing the action
