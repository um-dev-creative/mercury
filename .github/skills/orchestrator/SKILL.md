# Orchestrator SKILL

## Project-Specific Patterns

### Layer Decomposition Order
Always decompose Mercury features in this strict order before delegating:
1. **Persistence** (schema + entities + repositories) → `database-architect`
2. **API Contract** (OpenAPI, DTOs, status codes) → `api-reviewer`
3. **Implementation** (service, controller, mapper, Kafka) → `developer`
4. **Tests** (unit tests, coverage) → `test-writer`
5. **Review** (PMD, conventions, architecture) → `code-reviewer`
6. **Security** (if auth/Vault/JWT touched) → `security-reviewer`
7. **DevOps** (if bootstrap.yml/Kafka/Docker/release touched) → `devops-engineer`

### Dependency Tracking Pattern
```
Step N blocks Step N+1 when Step N output is required as input.
Example:
  Step 1 (database-architect: Flyway script) BLOCKS Step 3 (developer: ServiceImpl)
  Step 2 (api-reviewer: API contract) BLOCKS Step 3 (developer: Controller)
  Step 3 (developer: implementation) BLOCKS Step 4 (test-writer: tests)
  Step 4 (test-writer: coverage) BLOCKS Step 5 (code-reviewer: final review)
```

### Mercury Feature Categories
| Feature Type | Agents Involved (in order) |
|---|---|
| New REST endpoint | api-reviewer → developer → test-writer → code-reviewer |
| Schema change | database-architect → developer → test-writer → code-reviewer |
| Kafka consumer | devops-engineer → developer → test-writer → code-reviewer |
| Auth change | security-reviewer → developer → test-writer → code-reviewer |
| New Kafka topic | devops-engineer → developer → test-writer |
| New scheduler | devops-engineer → developer → test-writer |
| Bug fix | developer → test-writer → code-reviewer |
| Release | code-reviewer → security-reviewer → devops-engineer |

## Naming Conventions
- Agent delegation references: `@database-architect`, `@developer`, `@test-writer`,
  `@code-reviewer`, `@security-reviewer`, `@devops-engineer`, `@api-reviewer`
- Task IDs: `TASK-<feature>-<layer>-<seq>` (e.g., `TASK-campaign-persistence-01`)
- Delivery Plan table columns: `Step | Agent | Input | Expected Output | Blocking`

## Error Handling
- If `database-architect` signals a migration conflict → pause and resolve before continuing
- If `code-reviewer` returns `REQUEST_CHANGES` → route back to `developer` before proceeding
- If `security-reviewer` returns `BLOCK` → halt delivery; do not create PR
- If `test-writer` reports coverage below threshold → block merge; re-assign to `test-writer`
- If PMD violations > 0 → route to `code-reviewer` for inline fix instructions

## Key Files
- `.github/agents/` — all specialist agent definitions
- `.github/prompts/full-feature-delivery.prompt.md` — full workflow prompt
- `.github/hooks/pre-pull-request.hook.md` — pre-PR gate
- `src/main/java/com/umdc/mercury/` — source root
- `src/main/resources/db/migration/` — Flyway scripts
- `pom.xml` — Maven build definition
- `ruleset.xml` — PMD ruleset

## Constraints
- NEVER skip the `database-architect` step when a feature touches a JPA entity or MongoDB document
- NEVER skip the `api-reviewer` step when a feature adds or modifies a REST endpoint
- NEVER skip the `security-reviewer` step when auth, JWT, Vault, or `ForbiddenException` is involved
- NEVER create a PR without all blocking steps having passed
- NEVER delegate build/pipeline concerns to `developer` — use `devops-engineer`

## Checklist
- [ ] All Mercury layers identified for the feature
- [ ] Agent delegation order respects blocking dependencies
- [ ] Delivery Plan table produced before any delegation begins
- [ ] Each step result recorded (pass/fail + artifact)
- [ ] Final checklist confirmed: build passes, coverage ≥ 70%/50%, PMD clean, PR created
