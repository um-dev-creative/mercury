# Tool: PMD Static Analysis

**Purpose:** Run PMD static analysis on Mercury Java source code using the project ruleset.

## Ruleset Location
```
ruleset.xml — at repository root
/Users/lmata/projects/GitHub/mercury/ruleset.xml
```

## Running PMD via Maven

### Run PMD as Part of Test Phase
```bash
mvn clean test
```
PMD runs automatically during `test` phase. Violations cause `BUILD FAILURE`.

### Run PMD as Part of Full Verify
```bash
mvn -B -V -e clean verify
```
Recommended for pre-commit and CI gate.

### Run PMD Check Only (fast)
```bash
mvn pmd:check
```
Runs only PMD without compiling or testing. Fast iteration on fixes.

### Generate PMD HTML Report
```bash
mvn pmd:pmd
# Report at: target/site/pmd.html
```

## PMD Report Locations
```
target/pmd.xml           — machine-readable violations
target/site/pmd.html     — human-readable report (after mvn pmd:pmd)
```

## Key Rules Enforced (from ruleset.xml)

| Rule | Description | Common Trigger |
|---|---|---|
| `AtLeastOneConstructor` | Every class must have an explicit constructor | Missing no-arg ctor |
| `SystemPrintln` | No `System.out.println()` | Use SLF4J `log.info()` |
| `AvoidPrintStackTrace` | No `e.printStackTrace()` | Use `log.error(msg, e)` |
| `UnusedImports` | No unused import statements | IDE cleanup needed |
| `UseProperClassLoader` | Avoid direct classloader access | Spring beans only |

## Fix Patterns

### AtLeastOneConstructor
```java
// WRONG — no explicit constructor
public class CampaignServiceImpl { }

// CORRECT — explicit constructor
public class CampaignServiceImpl {
    public CampaignServiceImpl() { }
    // OR — dependency-injection constructor
    public CampaignServiceImpl(CampaignRepository repo) {
        this.repo = repo;
    }
}
```

### SystemPrintln / AvoidPrintStackTrace
```java
// WRONG
System.out.println("Processing " + id);
e.printStackTrace();

// CORRECT
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.info("Processing id={}", id);
log.error("Failed to process", e);
```

## Suppression (use sparingly, requires justification comment)
```java
@SuppressWarnings("PMD.AtLeastOneConstructor")
// Justified: Spring Data repository interface — no constructor needed
public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> { }
```

## Zero Tolerance
PMD violations must be 0 before any PR merge. The CI gate (`mvn clean verify`) enforces this.
