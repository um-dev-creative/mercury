# Mercury Prompts — Index

All executable prompt files for Mercury agent tasks.

| Prompt | File | Agent | Use Case |
|---|---|---|---|
| Implement Feature | `implement-feature.prompt.md` | developer | Build a feature across Mercury layers |
| Fix Bug | `fix-bug.prompt.md` | developer | Diagnose and fix a bug with tests |
| Fix Lint Violations | `fix-lint-violations.prompt.md` | code-reviewer | Resolve all PMD violations |
| Write Unit Tests | `write-unit-tests.prompt.md` | test-writer | Write JUnit 5 tests for a class |
| Improve Coverage | `improve-coverage.prompt.md` | test-writer | Close JaCoCo coverage gaps |
| Review Code | `review-code.prompt.md` | code-reviewer | Full PR code review |
| Security Audit | `security-audit.prompt.md` | security-reviewer | Auth/Vault/secrets security review |
| Prepare Release | `prepare-release.prompt.md` | devops-engineer | Version bump → tag → GitHub release |
| Full Feature Delivery | `full-feature-delivery.prompt.md` | orchestrator | End-to-end orchestrated delivery |
| Review API Contract | `review-api-contract.prompt.md` | api-reviewer | OpenAPI contract definition + review |
| Bootstrap Agent Infrastructure | `bootstrap-agent-infrastructure.prompt.md` | orchestrator | Generate all `.github/` agent artifacts |

## Quick Reference — Prompt → Hook Relationship

| Hook | Prompts Referenced |
|---|---|
| `pre-pull-request.hook.md` | `fix-lint-violations`, `improve-coverage`, `review-code` |
| `post-implementation-review.hook.md` | `fix-lint-violations`, `review-code`, `write-unit-tests`, `improve-coverage` |
| `post-merge-security.hook.md` | `security-audit` |
| `pre-release-gate.hook.md` | `security-audit`, `prepare-release` |

## Input Variable Summary

Each prompt uses `${variableName}` syntax for inputs:

| Prompt | Key Variables |
|---|---|
| `implement-feature` | `${featureName}`, `${layersAffected}` |
| `fix-bug` | `${bugDescription}`, `${affectedClass}` |
| `fix-lint-violations` | `${targetScope}` |
| `write-unit-tests` | `${targetClass}` |
| `improve-coverage` | `${targetPackage}`, `${currentLineCoverage}` |
| `review-code` | `${prBranch}`, `${baseBranch}` |
| `security-audit` | `${auditScope}`, `${newDependencies}` |
| `prepare-release` | `${releaseVersion}`, `${releaseNotes}` |
| `full-feature-delivery` | `${featureName}`, `${layersAffected}`, `${securitySensitive}` |
| `review-api-contract` | `${httpMethod}`, `${resourcePath}`, `${apiInterfaceName}` |
