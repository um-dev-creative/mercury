---
name: pre-release-gate
description: Final validation gate before a Mercury release tag is created — ensures all quality and security bars are met
trigger: pre-release
agents:
  - code-reviewer
  - security-reviewer
  - devops-engineer
auto-block: true
---
# Pre-Release Gate Hook

## Trigger Conditions
- **Event:** `devops-engineer` initiates release via `.github/prompts/prepare-release.prompt.md`
- **File Filters:** `pom.xml` version change + `CHANGELOG` update
- **Branch Filters:** `main` only

## Steps (Ordered — all must pass before tag creation)

### Step 1: Full Build and Quality Gate
**Agent:** `code-reviewer`
```bash
mvn -B -V -e clean verify
```
**Pass condition:** `BUILD SUCCESS`
- PMD violations = 0
- JaCoCo line ≥ 70%
- JaCoCo branch ≥ 50%
- All JUnit 5 tests pass

**Fail behavior:** BLOCK — do not proceed. Emit failure details. Release cancelled.

### Step 2: SonarCloud Quality Gate
**Agent:** `devops-engineer`
Check latest CI workflow run on `main`:
```bash
gh run list --workflow ci.yml --branch main --limit 1
```
**Pass condition:** Latest CI run = SUCCESS and SonarCloud quality gate = PASSED
**Fail behavior:** BLOCK — emit CI run URL and SonarCloud dashboard link. Release cancelled.

### Step 3: Security Audit
**Agent:** `security-reviewer`
**Prompt:** `.github/prompts/security-audit.prompt.md` (full scope)
Run full security audit on `main` HEAD:
- Secrets scan
- Vault config integrity
- ForbiddenException flow
- JWT handling
- CVE scan for all dependencies

**Pass condition:** Security reviewer verdict = APPROVE
**Fail behavior:** BLOCK — emit security findings. Release cancelled until all CRITICAL/HIGH resolved.

### Step 4: CHANGELOG Completeness
**Agent:** `devops-engineer`
Verify `CHANGELOG` has an entry for the release version:
```bash
head -20 CHANGELOG
```
**Pass condition:** Entry exists for `v${releaseVersion}` with Added/Fixed/Changed sections
**Fail behavior:** BLOCK — "CHANGELOG entry missing for v${releaseVersion}. Update before release."

### Step 5: Version Consistency
**Agent:** `devops-engineer`
Verify `pom.xml` version matches intended release version:
```bash
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```
**Pass condition:** Output = `${releaseVersion}` (no `-SNAPSHOT` suffix)
**Fail behavior:** BLOCK — "pom.xml version mismatch. Run `mvn versions:set -DnewVersion=${releaseVersion}`."

### Step 6: Clean Working Tree
**Agent:** `devops-engineer`
```bash
git --no-pager status --short
```
**Pass condition:** Empty output (no uncommitted changes)
**Fail behavior:** BLOCK — "Uncommitted changes on main. Commit or stash before release."

### Step 7: No Existing Tag Conflict
**Agent:** `devops-engineer`
```bash
git --no-pager tag -l "v${releaseVersion}"
```
**Pass condition:** Empty output (tag does not exist)
**Fail behavior:** BLOCK — "Tag v${releaseVersion} already exists. Choose a different version."

## Fail Behavior
- ALL steps are **auto-blocking** — release tag MUST NOT be created if any step fails
- Emit structured summary comment on the release commit
- Release process resumes from failed step after remediation

## Output (Summary Comment Structure)
```markdown
## Pre-Release Gate — v${releaseVersion}

| Gate | Status | Details |
|---|---|---|
| Build + PMD + Coverage | ✅ PASS / ❌ FAIL | BUILD SUCCESS / failure details |
| SonarCloud Quality Gate | ✅ PASS / ❌ FAIL | PASSED / FAILED |
| Security Audit | ✅ APPROVE / ❌ BLOCK | Verdict + findings summary |
| CHANGELOG | ✅ PRESENT / ❌ MISSING | v${releaseVersion} entry found / missing |
| pom.xml Version | ✅ MATCH / ❌ MISMATCH | ${releaseVersion} / actual version |
| Working Tree | ✅ CLEAN / ❌ DIRTY | Clean / N uncommitted files |
| Tag Conflict | ✅ NONE / ❌ EXISTS | No conflict / tag exists |

**Release Gate: PASSED — PROCEED WITH TAG / BLOCKED — DO NOT TAG**

### Blocking Issues (resolve before re-running gate)
- [itemized list with remediation steps]

### Next Steps (if PASSED)
1. `git tag -a v${releaseVersion} -m "Release v${releaseVersion}"`
2. `git push origin v${releaseVersion}`
3. `gh release create v${releaseVersion} --title "Mercury v${releaseVersion}" --notes-file CHANGELOG --latest`
```
