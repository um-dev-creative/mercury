---
name: Security Audit
description: >
  Run a full Mercury security audit: secrets scan, CVE check, auth flow review,
  Kafka/MongoDB exposure assessment. Returns PASS or FAIL with findings.
mode: agent
agent: security-reviewer
tools: [read_file, grep_search, run_in_terminal, codebase_search]
---

## Input Variables

- `${scope}` — `full` | `pre-release` | `deps-only` | `auth-only`
- `${triggerReason}` — why this audit is running (e.g., "dependency update", "new auth endpoint", "pre-release")

## Steps

1. **Secrets scan**:
   ```bash
   grep -rn "VAULT_TOKEN\|password\|secret\|token" \
     src/ default.env --include="*.java" --include="*.yml" --include="*.env"
   ```
   Any real value (not `${VAR}` or `<placeholder>`) is CRITICAL.

2. **Dependency CVE check**:
   ```bash
   mvn dependency:tree -DoutputFile=dependency-tree.txt
   ```
   Cross-reference Spring Boot 3.5.8, Kafka clients, Feign, Telegram bots against NVD.

3. **Auth flow review** (`src/main/java/com/prx/mercury/client/BackboneClient.java`):
   - Feign client must use `https://` URL
   - Token not logged in `SessionJwtServiceImpl`
   - Every secured endpoint throws `ForbiddenException` → 403

4. **Kafka exposure** (`bootstrap.yml`):
   - `PRX_KAFKA_AUTO_STARTUP=false` is the default
   - No broker credentials in source

5. **MongoDB lifecycle** (`MessageProcessor`):
   - `EmailMessageDocument` is deleted after PostgreSQL write
   - Connection string uses TLS in non-dev environments

## Output

```
Security Audit — Mercury (${scope})
Trigger: ${triggerReason}

CRITICAL: N findings
HIGH: N findings
MEDIUM: N findings

Secrets Scan: CLEAN / N exposures
CVE Summary: N applicable CVEs found
Auth Flow: PASS / ISSUES
Kafka Config: PASS / ISSUES
MongoDB Lifecycle: PASS / ISSUES

Overall: PASS / FAIL
```
