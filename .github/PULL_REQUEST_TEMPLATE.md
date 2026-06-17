## Summary

<!-- What does this PR do? Reference the issue: Closes ds-XXX -->

## Type of change

- [ ] New feature (`implement-feature.prompt.md`)
- [ ] Bug fix (`fix-bug.prompt.md`)
- [ ] PMD / lint fix (`fix-lint-violations.prompt.md`)
- [ ] Test / coverage (`write-unit-tests.prompt.md` / `improve-coverage.prompt.md`)
- [ ] Config / DevOps (bootstrap.yml, Dockerfile, workflows)
- [ ] Refactor (no behavior change)

## Mercury Layer Checklist

- [ ] `*Api` interface updated with `@Operation` + `@ApiResponses` (not on controller)
- [ ] Request/response records are Java `record`s in `com.prx.mercury.api.v1.to`
- [ ] Service returns `CompletableFuture<T>` for write operations
- [ ] Exception types used: `CampaignNotFoundException` (404), `ForbiddenException` (403), `IllegalStateException` (422)
- [ ] SLF4J `LoggerFactory.getLogger()` only — no other logging
- [ ] No hardcoded Kafka topics or scheduler rates
- [ ] No secrets committed — Vault-backed `${ENV_VAR}` placeholders only
- [ ] Every new class has an explicit constructor (PMD `AtLeastOneConstructor`)

## Quality Gates

- [ ] `mvn -B -V -e clean verify` passes locally
- [ ] PMD: zero violations (`target/pmd.xml`)
- [ ] JaCoCo: LINE ≥ 70%, BRANCH ≥ 50%

## If API contract changed

- [ ] `review-api-contract.prompt.md` run with `api-reviewer` agent
- [ ] `operationId` is unique and follows `{verb}{Resource}` camelCase

## If auth / secrets / Kafka config changed

- [ ] `security-audit.prompt.md` run with `security-reviewer` agent

## Agents Used

<!-- List which agents / prompts were used to generate or review this change -->
- Agent: `<!-- developer | test-writer | code-reviewer | ... -->`
- Prompt: `.github/prompts/<!-- prompt file -->`
