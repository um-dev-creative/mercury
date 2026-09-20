# Security Reviewer SKILL

## Project-Specific Patterns

### Vault Property Pattern (CORRECT)
```yaml
# bootstrap.yml — all secrets via Vault
spring:
  cloud:
    vault:
      host: ${VAULT_HOST}
      port: ${VAULT_PORT}
      token: ${VAULT_TOKEN}
      scheme: https
  datasource:
    password: ${DB_PASSWORD}   # resolved from Vault
jwt:
  secret: ${JWT_SECRET}        # resolved from Vault
  expiry: ${JWT_EXPIRY_MS}     # resolved from Vault
ssl:
  key-store-password: ${prx.ssl.keystore.password}
```

### Vault Property Anti-Patterns (BLOCK these)
```yaml
# NEVER — plaintext secrets
spring:
  datasource:
    password: mypassword123

jwt:
  secret: hardcoded-signing-key

# NEVER — secrets in application.yml (must be bootstrap.yml + Vault)
```

### ForbiddenException Usage Pattern
```java
// CORRECT — in AuthServiceImpl or any service requiring auth check
public void validateAccess(String userId, Long resourceId) {
    if (!hasPermission(userId, resourceId)) {
        log.warn("Access denied for user={} resource={}", userId, resourceId);
        throw new ForbiddenException("User " + userId + " lacks access to resource " + resourceId);
    }
}
// → GlobalExceptionHandler maps ForbiddenException → HTTP 403
```

### JWT Handling Pattern
```java
// CORRECT — secret from Vault-injected property
@Value("${jwt.secret}")
private String jwtSecret;  // resolved from Vault at startup

// INCORRECT — never hardcoded
private String jwtSecret = "my-secret-key";
```

### Keystore Management
```
mercury.jks        — application TLS identity keystore
                     Password via ${prx.ssl.keystore.password} (Vault)
umdc-truststore.jks — trusted CA certificates
                     Password via ${prx.ssl.truststore.password} (Vault)
Location: src/main/resources/ or external path configured in bootstrap.yml
Tool: .github/tools/keytool.tool.md
```

### Security Package Structure
```
com.umdc.mercury.security/       — Spring Security config, JWT filter
com.umdc.mercury.config/         — GlobalExceptionHandler, WebMvcConfigurer
com.umdc.mercury.api.v1.service/ — AuthServiceImpl
com.umdc.mercury.api.v1.exception/ForbiddenException.java
```

### Sensitive Log Data Check
```java
// NEVER log these — even at DEBUG
log.debug("JWT token: {}", jwtToken);        // BLOCK
log.info("Password attempt: {}", password);  // BLOCK
log.trace("Request body: {}", requestBody);  // BLOCK if contains PII

// SAFE
log.warn("Auth failure for user={}", userId);  // OK — no secret
log.info("Token validated for user={}", userId); // OK — no token value
```

## Naming Conventions
- Exception: `ForbiddenException` in `com.umdc.mercury.api.v1.exception`
- Auth service: `AuthServiceImpl` in `com.umdc.mercury.api.v1.service`
- JWT filter: expected in `com.umdc.mercury.security`
- Config properties: `${prx.ssl.*}`, `${jwt.*}`, `${spring.cloud.vault.*}`

## Error Handling
| Scenario | Expected Behavior | HTTP |
|---|---|---|
| Auth check fails | Throw `ForbiddenException` with user/resource context | 403 |
| Invalid JWT | JWT filter rejects; Spring Security returns 401 | 401 |
| Expired JWT | JWT filter rejects; Spring Security returns 401 | 401 |
| Missing auth header | Spring Security returns 401 | 401 |

## Key Files
- `src/main/java/com/umdc/mercury/security/` — security filter chain
- `src/main/java/com/umdc/mercury/api/v1/service/AuthServiceImpl.java`
- `src/main/java/com/umdc/mercury/api/v1/exception/ForbiddenException.java`
- `src/main/java/com/umdc/mercury/config/` — `GlobalExceptionHandler`
- `src/main/resources/bootstrap.yml` — Vault config, SSL config
- `.github/tools/keytool.tool.md` — keystore management
- `pom.xml` — dependency CVE surface

## Constraints
- NEVER approve any plaintext secret, password, or key in source code
- NEVER approve JWT secret or expiry hardcoded anywhere
- NEVER approve logging of JWT tokens, passwords, or PII
- NEVER approve `ForbiddenException` bypassed with null return
- NEVER approve Vault config outside `bootstrap.yml`
- NEVER approve a new dependency without CVE check (`validate_cves` tool)
- NEVER approve changes to `security/` package without full security review

## Checklist
- [ ] No plaintext secrets in any source file or config file
- [ ] All `${ENV_VAR}` references in `bootstrap.yml` are Vault-backed
- [ ] `ForbiddenException` thrown (never null) on auth failure → 403
- [ ] JWT secret and expiry come from Vault, not hardcoded
- [ ] No sensitive data (tokens, passwords, PII) logged at any level
- [ ] Keystore passwords via `${prx.ssl.*}` Vault properties
- [ ] CVE scan clean for any new `pom.xml` dependencies
- [ ] `AuthServiceImpl` and `ForbiddenException` used consistently
- [ ] Spring Security filter chain not weakened (no `permitAll()` on protected paths)
- [ ] Verdict issued: APPROVE or BLOCK with itemized findings
