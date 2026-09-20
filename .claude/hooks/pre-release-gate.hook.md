---
name: Pre-Release Gate
description: Blocking gate that must pass before a release tag is created. Enforces full build health, security audit, and documentation completeness.
trigger: pre-release-tag
agents: [devops-engineer, security-reviewer, code-reviewer]
auto-block: true
---

## Trigger Conditions

Activates when:
- A release version tag (e.g., `v0.1.0`) is about to be created
- The `prepare-release.prompt.md` workflow reaches Step 6 (git tag)

## Steps

1. **Full build verification**:
   ```bash
   mvn -B -V -e clean verify
   ```
   Must pass. If FAILURE: block tag creation, report cause.

2. **PMD zero-violations check**:
   - Parse `target/pmd.xml`
   - Any violation → BLOCK

3. **JaCoCo threshold confirmation**:
   - LINE ≥ 70%, BRANCH ≥ 50%
   - Below threshold → BLOCK, require test additions first

4. **Security audit** — invoke **security-reviewer** with `.claude/prompts/security-audit.prompt.md`:
   - Scope: `FULL`
   - Must return PASS (no CRITICAL or HIGH findings)

5. **Documentation completeness**:
   - All `${ENV_VAR}` placeholders in `bootstrap.yml` present in `environment_variables.md`
   - `CHANGELOG` has an entry for the release version
   - `pom.xml` version matches the tag version

6. **No uncommitted changes**:
   ```bash
   git status
   ```
   Working tree must be clean.

7. **No snapshot dependencies**:
   ```bash
   grep -c "SNAPSHOT" pom.xml
   ```
   Must return 0 — no snapshot deps in a release build.

## Fail Behavior

- Any failure blocks tag creation
- Report includes which step failed, command output, and which prompt to run to remediate
- On security failure: invoke `.claude/prompts/security-audit.prompt.md` for details

## Output

```
Pre-Release Gate Results for v${releaseVersion}:
  [PASS/FAIL] mvn clean verify
  [PASS/FAIL] PMD — 0 violations required
  [PASS/FAIL] JaCoCo — LINE: X%, BRANCH: Y%
  [PASS/FAIL] Security audit — PASS / N findings
  [PASS/FAIL] Docs complete — CHANGELOG, env vars
  [PASS/FAIL] Clean working tree
  [PASS/FAIL] No SNAPSHOT dependencies

Gate Status: PASS → tag v${releaseVersion} created / BLOCKED → list reasons
```
