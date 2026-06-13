---
name: Post-Merge Security Scan
description: Non-blocking security scan triggered after merge to develop when dependency-related files change. Checks for new secrets, auth gaps, and new env var documentation.
trigger: merge-to-develop-deps-changed
agents: [security-reviewer]
auto-block: false
---

## Trigger Conditions

Activates when:
- A merge to `develop` occurs AND any of these files were changed:
  - `pom.xml` (dependency changes)
  - `src/main/resources/bootstrap.yml` (config changes)
  - `environment_variables.md` (env var documentation)
  - Any `*Config.java` in `src/main/java/com/prx/mercury/config/`
  - `src/main/java/com/prx/mercury/security/`
  - `src/main/java/com/prx/mercury/client/`

## Steps

1. **Secret scan**:
   ```bash
   grep -r "password\s*=\s*['\"]" src/main/
   grep -r "secret\s*=\s*['\"]" src/main/
   grep -r "token\s*=\s*['\"]" src/main/
   ```
   Any match not using `${ENV_VAR}` placeholder → HIGH security finding.

2. **New dependency audit** (if `pom.xml` changed):
   - List newly added `<dependency>` entries
   - Flag any dependency not from trusted sources (`spring.io`, `apache.org`, `repsy.io/mvn/lmata/prx`)

3. **Security review** — invoke **security-reviewer** with `.claude/prompts/security-audit.prompt.md`:
   - Scope: `DIFF` of the merged changes
   - Focus: new auth endpoints, new env vars, config changes

4. **Env var documentation check**:
   - Compare new `${ENV_VAR}` placeholders in `bootstrap.yml` against `environment_variables.md`
   - Any undocumented placeholder → WARNING

## Fail Behavior

- Non-blocking — does not revert the merge
- CRITICAL or HIGH findings trigger an immediate issue/notification for follow-up
- MEDIUM and LOW findings are logged as informational

## Output

```
Post-Merge Security Scan:
  [INFO] Secret scan: CLEAN / N findings
  [INFO] New dependencies: N added, N flagged
  [INFO] Auth review: PASS / N findings
  [INFO] Env var docs: COMPLETE / N undocumented vars

Follow-up required: YES / NO
(List any HIGH+ findings requiring immediate action)
```
