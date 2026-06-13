# Mercury — GitHub Copilot Prompt Registry

All prompts in `.github/prompts/`. Each prompt targets a specific agent and supports GitHub Copilot agent mode (`mode: agent`) or clarification mode (`mode: ask`).

## Prompt Roster

| File | Agent | Mode | Trigger |
|---|---|---|---|
| `implement-feature.prompt.md` | developer | agent | New feature story or endpoint |
| `fix-bug.prompt.md` | developer | agent | Bug report with stack trace |
| `fix-lint-violations.prompt.md` | developer | agent | PMD build failure |
| `write-unit-tests.prompt.md` | test-writer | agent | New code without tests / coverage gap |
| `improve-coverage.prompt.md` | test-writer | agent | JaCoCo gate failure |
| `review-code.prompt.md` | code-reviewer | ask | PR open / code change |
| `security-audit.prompt.md` | security-reviewer | agent | Pre-release / new auth endpoint / dep change |
| `prepare-release.prompt.md` | devops-engineer | agent | Release cut |
| `review-api-contract.prompt.md` | api-reviewer | ask | `*Api.java` file changed |
| `full-feature-delivery.prompt.md` | orchestrator | agent | Complex multi-layer feature |
| `bootstrap-agent-infrastructure.prompt.md` | orchestrator | agent | Initial agent infrastructure setup |

## Input Variable Convention

All prompts use `${variableName}` placeholders. Substitute before invoking:
- `${featureName}` — human-readable name (e.g., "Campaign Pause")
- `${issueId}` — issue tracker ID (e.g., `ds-174`)
- `${httpMethod}` — GET | POST | PUT | PATCH | DELETE
- `${endpointPath}` — full REST path
- `${targetClass}` — fully qualified Java class name
- `${prScope}` — PR number or branch name
- `${version}` — semver string for releases
