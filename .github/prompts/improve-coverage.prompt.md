---
name: improve-coverage
description: Identify and close coverage gaps across Mercury to meet JaCoCo thresholds
mode: agent
agent: test-writer
tools:
  - run_in_terminal
  - read_file
  - grep_search
  - file_search
  - insert_edit_into_file
  - replace_string_in_file
  - create_file
  - get_errors
---
# Improve Coverage

## Input Variables
- `${targetPackage}` — package to improve (e.g., `com.umdc.mercury.api.v1.service`) or `all`
- `${currentLineCoverage}` — current overall line coverage percentage (e.g., `62`)
- `${currentBranchCoverage}` — current overall branch coverage percentage (e.g., `45`)
- `${targetLineCoverage}` — target (default: `70`)
- `${targetBranchCoverage}` — target (default: `50`)

## Step 1: Generate Coverage Report
```bash
mvn -Pcoverage clean test
# Opens: target/site/jacoco/index.html
# XML:   target/site/jacoco/jacoco.xml
```

## Step 2: Identify Classes Below Threshold
Parse `target/site/jacoco/jacoco.xml`:
```bash
# Classes with missed lines > 0
grep -A3 'type="LINE"' target/site/jacoco/jacoco.xml | \
  grep -B2 'missed="[^0]' | grep 'class name'
```

Create priority list:
1. Classes with 0% coverage (no tests at all) — highest priority
2. Classes below 70% line with complex logic — high priority
3. Classes with many uncovered branches — medium priority

## Step 3: For Each Undercovered Class

### Read the class
Identify:
- Uncovered methods (no test exists)
- Uncovered exception branches (exception thrown but not tested)
- Uncovered conditional branches (if/else not fully exercised)

### Write targeted tests
Focus on branch coverage — these are the hardest to reach:

```java
// Branch: empty Optional
@Test
void shouldThrowNotFoundWhenEntityMissing() {
    when(repo.findById(anyLong())).thenReturn(Optional.empty());
    assertThrows(CampaignNotFoundException.class, () -> service.get(99L));
}

// Branch: null check
@Test
void shouldThrowIllegalArgumentWhenInputNull() {
    assertThrows(IllegalArgumentException.class, () -> service.create(null));
}

// Branch: channel disabled
@Test
void shouldThrowIllegalStateWhenChannelDisabled() {
    when(channel.isEnabled()).thenReturn(false);
    assertThrows(IllegalStateException.class, () -> service.send(msg));
}

// Branch: CompletableFuture exception path
@Test
void shouldHandleExceptionInAsyncOperation() {
    when(repo.save(any())).thenThrow(new RuntimeException("DB error"));
    CompletableFuture<Response> future = service.create(request);
    assertThrows(ExecutionException.class, future::get);
}
```

## Step 4: Prioritized Classes in `${targetPackage}`

Cover these Mercury service classes (highest business logic density):
- `CampaignServiceImpl` — campaign CRUD + progress
- `EmailServiceImpl` — email channel service
- `VerificationCodeServiceImpl` — code generation/validation
- `CampaignProgressServiceImpl` — progress calculation
- `MessageRecordServiceImpl` — message tracking
- `AuthServiceImpl` — auth checks

Cover these Kafka classes:
- `MultiChannelListener` — `consume()` routing
- `MessageChannelRouter` — routing logic

## Step 5: Iterate Until Threshold Met
After each batch of new tests:
```bash
mvn -Pcoverage clean test
# Check overall coverage in target/site/jacoco/index.html
```

Repeat until:
- Line coverage ≥ ${targetLineCoverage}%
- Branch coverage ≥ ${targetBranchCoverage}%

## Step 6: Final Verify
```bash
mvn -B -V -e clean verify
```
Must produce `BUILD SUCCESS` (coverage gates enforced).

## Constraints
- NEVER write tests that only exercise constructors/getters/setters for coverage inflation
- NEVER use `@SpringBootTest` for unit test coverage — use `MockitoExtension`
- NEVER write assertions that always pass regardless of logic (`assertThat(true).isTrue()`)
- Focus on branch coverage — it's harder to reach and more valuable

## Output Format
1. Coverage baseline: Line ${currentLineCoverage}% / Branch ${currentBranchCoverage}%
2. Top 10 classes by coverage gap (table: Class | Line% | Branch% | Gap)
3. Tests added per class
4. Coverage after fix: Line% / Branch% per class
5. Overall coverage: Line% / Branch% final
6. `mvn -B -V -e clean verify` result
