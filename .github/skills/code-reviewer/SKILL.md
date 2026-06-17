# Code Reviewer SKILL

## Project-Specific Patterns

### PMD Violation Categories to Check
```
AtLeastOneConstructor    — every class must have an explicit constructor
UseProperClassLoader     — never use Thread.currentThread().getContextClassLoader() directly
SystemPrintln            — never System.out.println(); use SLF4J
AvoidPrintStackTrace     — never e.printStackTrace(); use log.error(msg, e)
UnusedImports            — no unused import statements
UnnecessaryConstructor   — default constructor with no body only if PMD requires it
```

### Architecture Violation Checklist
```
✗ @Tag / @Operation / @ApiResponse on *Controller class
✗ Hardcoded Kafka topic: "campaign-notifications" — must be ${prx.consumer.topics.*}
✗ Hardcoded scheduler rate: fixedRate = 5000L — must be ${prx.scheduler.*}
✗ new CampaignMapperImpl() — must inject as @Autowired / constructor
✗ System.out.println — must use log.info / log.warn / log.error
✗ e.printStackTrace() — must use log.error("msg", e)
✗ Missing explicit constructor
✗ Secrets in source: password = "secret" — must be ${ENV_VAR} backed by Vault
✗ DDL in entity: @Column(columnDefinition = "...") for schema changes — use Flyway
```

### Correct vs. Incorrect Patterns

**Logging:**
```java
// CORRECT
private static final Logger log = LoggerFactory.getLogger(CampaignServiceImpl.class);
log.info("Processing campaign id={}", id);
log.error("Failed to process campaign", e);

// INCORRECT
System.out.println("Processing campaign " + id);
e.printStackTrace();
```

**Async:**
```java
// CORRECT — write returns CompletableFuture
public CompletableFuture<CampaignResponse> createCampaign(CampaignRequest req) { ... }

// INCORRECT — write should not return void or T synchronously
public CampaignResponse createCampaign(CampaignRequest req) { ... }
```

**Exception throwing:**
```java
// CORRECT
throw new CampaignNotFoundException("Campaign not found: " + id);
throw new ForbiddenException("Access denied for user: " + userId);

// INCORRECT — silent return on not-found
return null;
return Optional.empty(); // without surfacing error
```

### Exception → HTTP Mapping Verification
| Exception | Expected HTTP | Verified By |
|---|---|---|
| `CampaignNotFoundException` | 404 | `GlobalExceptionHandler` |
| `ForbiddenException` | 403 | `GlobalExceptionHandler` |
| `IllegalStateException` | 422 | `GlobalExceptionHandler` |
| `IllegalArgumentException` | 400 | `GlobalExceptionHandler` |
| `MethodArgumentNotValidException` | 400 | Spring MVC |

## Naming Conventions
Verify all new files follow Mercury naming conventions:
- `*Api` → REST interface with OpenAPI annotations
- `*Controller` → implements `*Api`, zero OpenAPI annotations
- `*ServiceImpl` → service implementation
- `*Entity` → JPA entity in `jpa/sql/entity/`
- `*Repository` → Spring Data repository in `jpa/sql/repository/` or `jpa/nosql/repository/`
- `*Document` → MongoDB document in `jpa/nosql/document/`
- `*Mapper` → MapStruct mapper in `mapper/`
- `*Request` / `*Response` / `*TO` → records in `api/v1/to/`

## Error Handling
When violations are found:
- **BLOCK** (must fix before merge): PMD violations, missing constructors, hardcoded topics/secrets, wrong HTTP codes, OpenAPI on Controller
- **ADVISORY** (should fix, can merge with note): minor naming inconsistency, missing edge-case test, log message formatting

## Key Files
- `ruleset.xml` — PMD ruleset at repo root
- `src/main/java/com/umdc/mercury/api/v1/exception/` — exception classes
- `src/main/java/com/umdc/mercury/config/` — `GlobalExceptionHandler` location
- `target/pmd/` — PMD reports after `mvn clean test`
- `target/site/jacoco/` — JaCoCo reports
- `.github/tools/pmd.tool.md` — PMD run commands
- `.github/tools/jacoco.tool.md` — coverage commands

## Constraints
- NEVER approve a PR with PMD violations > 0
- NEVER approve a PR that places OpenAPI annotations on `*Controller`
- NEVER approve a PR with hardcoded Kafka topics or scheduler rates
- NEVER approve a PR with secrets in source code
- NEVER approve a PR where coverage drops below 70% line / 50% branch
- NEVER approve a PR that uses `System.out.println` or `e.printStackTrace()`

## Checklist
- [ ] PMD violations = 0 (`mvn clean test` produces no PMD output)
- [ ] All new classes have explicit constructors
- [ ] No OpenAPI annotations on `*Controller` classes
- [ ] No hardcoded Kafka topic strings
- [ ] No hardcoded scheduler rate values
- [ ] No `new *MapperImpl()` instantiations
- [ ] SLF4J logger used everywhere (no `System.out.println`)
- [ ] No secrets in source or config
- [ ] Exception → HTTP mapping correct per table
- [ ] Write operations return `CompletableFuture<T>`
- [ ] Line coverage ≥ 70%, branch coverage ≥ 50%
- [ ] Flyway migration script provided for schema changes
- [ ] Verdict issued: APPROVE or REQUEST_CHANGES with itemized list
