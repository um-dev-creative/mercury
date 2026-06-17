---
name: security-audit
description: Perform a security-focused audit of Mercury changes touching auth, Vault, JWT, or secrets
mode: agent
agent: security-reviewer
tools:
  - run_in_terminal
  - read_file
  - grep_search
  - file_search
  - get_errors
  - validate_cves
---
# Security Audit

## Input Variables
- `${auditScope}` — `full` | `pr-diff` | specific package (e.g., `com.umdc.mercury.security`)
- `${prBranch}` — branch to audit (for `pr-diff` scope)
- `${newDependencies}` — comma-separated new Maven dependencies added (or `none`)

## Step 1: Secrets Scan
Search entire source for hardcoded secrets:
```bash
# Passwords / secrets / tokens in source
grep -rn --include="*.java" \
  'password\s*=\s*"[^$\{]\|secret\s*=\s*"[^$\{]\|token\s*=\s*"[^$\{]\|apiKey\s*=\s*"' \
  src/main/java/com/umdc/mercury/

# Plaintext values in config files
grep -rn --include="*.yml" --include="*.yaml" --include="*.properties" \
  'password:\s*[^$\{]\|secret:\s*[^$\{]\|token:\s*[^$\{]' \
  src/main/resources/
```
Any hit = BLOCK (CRITICAL severity).

## Step 2: Vault Configuration Audit
Check `src/main/resources/bootstrap.yml`:
- All secrets use `${ENV_VAR}` pattern (Vault-backed)
- `spring.cloud.vault.*` properties present and use env vars
- `spring.jpa.password`, `spring.datasource.password` use `${DB_PASSWORD}` or similar
- JWT config uses `${jwt.secret}`, `${jwt.expiry}` — not literals

```bash
grep -n "password\|secret\|token\|key" src/main/resources/bootstrap.yml
```
Review each line: must be `${...}` pattern.

## Step 3: ForbiddenException Flow Audit
Verify auth failures throw `ForbiddenException` → 403:
```bash
# Find all auth checks
grep -rn "hasPermission\|isAuthorized\|checkAccess\|validateUser" \
  src/main/java/com/umdc/mercury/

# Verify ForbiddenException thrown (not null return)
grep -rn "return null\|return Optional.empty" \
  src/main/java/com/umdc/mercury/api/v1/service/AuthServiceImpl.java
```
`return null` on auth method = BLOCK.

Check `GlobalExceptionHandler` maps `ForbiddenException` → 403:
```bash
grep -n "ForbiddenException" src/main/java/com/umdc/mercury/config/
```

## Step 4: JWT Handling Audit
```bash
# JWT secret source
grep -rn "jwtSecret\|JWT_SECRET\|signing.key" src/main/java/com/umdc/mercury/security/

# Verify no hardcoded JWT secrets
grep -rn '"[A-Za-z0-9+/=]\{32,\}"' src/main/java/com/umdc/mercury/security/
```

Check:
- JWT secret from `@Value("${jwt.secret}")` — backed by Vault
- Token expiry from `@Value("${jwt.expiry}")` — backed by Vault
- No token values logged

## Step 5: Sensitive Data Logging Audit
```bash
grep -rn 'log\.\(debug\|info\|warn\|error\|trace\).*\(token\|password\|secret\|jwt\)' \
  src/main/java/com/umdc/mercury/
```
Any log statement emitting token/password values = HIGH severity finding.

## Step 6: Keystore Audit
```bash
# Keystore passwords must be ${prx.ssl.*}
grep -n "key-store-password\|trust-store-password" src/main/resources/bootstrap.yml
```
Must show `${prx.ssl.keystore.password}` / `${prx.ssl.truststore.password}`.

## Step 7: CVE Scan (if new dependencies)
For each dependency in `${newDependencies}`:
Use `validate_cves` tool to check for known CVEs.
Any CRITICAL or HIGH CVE = BLOCK.

## Step 8: Spring Security Configuration Review
```bash
cat src/main/java/com/umdc/mercury/security/*.java
```
Verify:
- No `permitAll()` on protected paths
- JWT filter registered in security filter chain
- CSRF disabled intentionally (document if so)
- CORS configured appropriately

## Compile Verdict

### APPROVE conditions:
- Zero secrets in source
- All Vault-backed `${ENV_VAR}` patterns in bootstrap.yml
- `ForbiddenException` thrown (never null) on auth failure
- JWT secret/expiry from Vault
- No sensitive data in logs
- Keystore passwords from `${prx.ssl.*}`
- No CRITICAL/HIGH CVEs in new dependencies

### BLOCK conditions (any one):
- Plaintext secret/password/token in source or config
- `return null` on auth path instead of `ForbiddenException`
- JWT secret hardcoded
- CVE CRITICAL or HIGH in new dependency
- Sensitive data (token, password) emitted in logs

## Output Format
1. Secrets scan: PASS / FAIL (files + lines if failed)
2. Vault config audit: PASS / FAIL
3. ForbiddenException flow: PASS / FAIL
4. JWT handling: PASS / FAIL
5. Logging audit: PASS / FAIL
6. Keystore config: PASS / FAIL
7. CVE scan summary (if applicable)
8. Findings table: File | Line | Severity | Description | Remediation
9. **Verdict: APPROVE / BLOCK**
