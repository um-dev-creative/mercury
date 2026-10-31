---
name: fix-lint-violations
description: Identify and fix all PMD static analysis violations in Mercury source code
mode: agent
agent: code-reviewer
tools:
  - run_in_terminal
  - read_file
  - grep_search
  - file_search
  - insert_edit_into_file
  - replace_string_in_file
  - get_errors
---
# Fix Lint Violations

## Input Variables
- `${targetScope}` — scope of fix: `all` | specific package (e.g., `com.umdc.mercury.api.v1.service`) | specific file path
- `${violationReport}` — optional: paste PMD violation output here if already available

## Step 1: Run PMD and Collect Violations
```bash
mvn pmd:check 2>&1 | tee /dev/stderr
# Also generate full report:
mvn pmd:pmd
# View: target/site/pmd.html
```

## Step 2: Parse Violations
For each violation, identify:
- File path
- Line number
- Rule name
- Description

Group by rule name for batch fixing efficiency.

## Step 3: Fix by Rule

### AtLeastOneConstructor
Every class must have an explicit constructor.
```java
// Add to any class missing one:
public ClassName() { }
// OR use the dependency-injection constructor if dependencies exist
public ClassName(Dependency dep) { this.dep = dep; }
```

### SystemPrintln
Replace all `System.out.println(...)` with SLF4J:
```java
// Remove:
System.out.println("message " + value);
// Add logger declaration if missing:
private static final Logger log = LoggerFactory.getLogger(ClassName.class);
// Replace with:
log.info("message {}", value);
```

### AvoidPrintStackTrace
Replace all `e.printStackTrace()` with SLF4J:
```java
// Remove:
e.printStackTrace();
// Replace with:
log.error("Operation failed", e);
```

### UnusedImports
Remove all unused import statements. Use IDE or:
```bash
grep -n "^import" src/main/java/path/to/File.java
# Cross-reference with actual usage in file body
```

### UseProperClassLoader
```java
// Avoid direct classloader access outside Spring context
// Use Spring's ResourceLoader or ClassPathResource instead
```

## Step 4: Verify Fix Iteration
After each batch of fixes:
```bash
mvn pmd:check
```
Repeat until 0 violations.

## Step 5: Final Full Build
```bash
mvn -B -V -e clean verify
```
Must produce `BUILD SUCCESS` with 0 PMD violations and all tests passing.

## Scope: ${targetScope}
If `all`: scan entire `src/main/java/` tree.
If specific package: scan only that package and sub-packages.
If specific file: fix only that file then verify no regressions.

## Constraints
- NEVER suppress PMD violations with `@SuppressWarnings("PMD.*")` without a justified comment
- NEVER add a no-op constructor to a Spring Data `@Repository` interface (PMD exemption required)
- NEVER change logic while fixing lint — structural changes only
- NEVER break existing test coverage while adding constructors

## Output Format
1. Violations found table: File | Line | Rule | Description
2. Fixes applied table: File | Rule Fixed | Change Made
3. `mvn pmd:check` final output (0 violations required)
4. `mvn clean test` output (all tests must still pass)
