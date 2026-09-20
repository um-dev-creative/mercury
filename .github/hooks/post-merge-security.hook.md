---
name: post-merge-security
description: Security scan triggered automatically after merge to main — catches any security regressions introduced
trigger: post-merge
agents:
  - security-reviewer
  - code-reviewer
auto-block: false
---
# Post-Merge Security Hook

## Trigger Conditions
- **Event:** Successful merge to `main` branch
- **File Filters:** Changes to:
  - `src/main/java/com/umdc/mercury/security/**`
  - `src/main/java/com/umdc/mercury/config/**`
  - `src/main/java/com/umdc/mercury/api/v1/service/AuthServiceImpl.java`
  - `src/main/resources/bootstrap.yml`
  - `pom.xml` (new dependency added)
- **Branch Filters:** `main` (post-merge only)

## Steps (Ordered)

### Step 1: Secrets Regression Scan
**Agent:** `security-reviewer`
**Prompt:** `.github/prompts/security-audit.prompt.md` (Step 1)

Scan entire `src/` for any hardcoded secrets introduced in the merge:
```bash
git --no-pager diff HEAD~1 HEAD -- src/ | \
  grep "^+" | \
  grep -iE 'password\s*=\s*"[^$\{]|secret\s*=\s*"[^$\{]|token\s*=\s*"[^$\{]'
```
**Pass condition:** Zero hits
**Fail behavior:** ALERT (critical) — immediately notify team. Create hotfix issue. Do NOT auto-revert (manual review required).

### Step 2: ForbiddenException Flow Check
**Agent:** `security-reviewer`
**Prompt:** `.github/prompts/security-audit.prompt.md` (Step 3)

If `AuthServiceImpl.java` was in merge diff:
```bash
grep -n "return null\|return Optional.empty" \
  src/main/java/com/umdc/mercury/api/v1/service/AuthServiceImpl.java
```
**Pass condition:** No `return null` on auth paths
**Fail behavior:** HIGH alert — emit finding. Create issue for hotfix.

### Step 3: Vault Config Integrity
**Agent:** `security-reviewer`
**Prompt:** `.github/prompts/security-audit.prompt.md` (Step 2)

If `bootstrap.yml` was in merge diff:
```bash
git --no-pager diff HEAD~1 HEAD -- src/main/resources/bootstrap.yml | \
  grep "^+" | grep -vE '\$\{|#|spring\.cloud\.vault'
```
Review new lines for plaintext values.
**Pass condition:** All new config lines use `${ENV_VAR}` pattern
**Fail behavior:** CRITICAL alert — create immediate hotfix issue.

### Step 4: CVE Scan on New Dependencies
**Agent:** `security-reviewer`
If `pom.xml` was in merge diff (new `<dependency>` added):
Use `validate_cves` tool for all new dependencies.
**Pass condition:** No CRITICAL or HIGH CVEs
**Fail behavior:** HIGH alert — emit CVE report. Create issue for dependency upgrade.

### Step 5: Security Package Review
**Agent:** `security-reviewer`
If any file under `com.umdc.mercury.security/` was in merge diff:
Run full security audit:
**Prompt:** `.github/prompts/security-audit.prompt.md` (full)
**Pass condition:** APPROVE verdict
**Fail behavior:** HIGH alert — emit findings. Create issue.

## Fail Behavior
- This hook is **informational + alerting** (post-merge, cannot auto-block)
- CRITICAL/HIGH findings → create GitHub issue tagged `security` `hotfix`
- MEDIUM findings → create GitHub issue tagged `security`
- LOW findings → advisory comment on merge commit

## Output (Summary Comment Structure)
```markdown
## Post-Merge Security Scan — [commit SHA]

| Check | Status | Severity | Details |
|---|---|---|---|
| Secrets Scan | ✅ CLEAN / 🚨 ALERT | CRITICAL | N hardcoded secrets found |
| ForbiddenException Flow | ✅ CLEAN / ⚠️ ALERT | HIGH | N auth paths return null |
| Vault Config | ✅ CLEAN / 🚨 ALERT | CRITICAL | N plaintext values found |
| CVE Scan | ✅ CLEAN / ⚠️ ALERT | HIGH | N CVEs: [list] |
| Security Package | ✅ PASS / ⚠️ REVIEW | — | APPROVE / findings |

**Overall: CLEAN / FINDINGS DETECTED**

### Issues Created
- [issue links for any findings]

### Immediate Actions Required (for CRITICAL findings)
- [action items with assignees]
```
