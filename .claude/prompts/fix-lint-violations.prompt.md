---
name: fix-lint-violations
description: Resolves all PMD static analysis violations in Mercury to make mvn clean test pass
mode: agent
agent: developer
tools: [Read, Edit, Bash]
---

## Input Variables

- `${violationList}` — paste the PMD violation output from the build log, or `ALL` to fix all current violations

## Steps

1. **Get current violations**:
   ```bash
   mvn pmd:check 2>&1 | grep -A 3 "PMD Failure"
   ```
   Or check `target/pmd.xml` after running `mvn pmd:pmd`.

2. **Group violations by rule** and address each:

   **`AtLeastOneConstructor`** — Add explicit no-arg constructor:
   ```java
   public MyClass() {
       // default constructor
   }
   ```

   **`UnusedPrivateField`** — Remove unused field or add usage; if it's a logger, check `LoggerFactory.getLogger()` pattern.

   **`UnusedPrivateMethod`** — Remove method or change visibility if used externally.

   **`UnusedLocalVariable`** — Remove assignment or use the variable.

   **`PreserveStackTrace`** — Change:
   ```java
   // WRONG
   throw new RuntimeException(e.getMessage());
   // RIGHT
   throw new RuntimeException(e.getMessage(), e);
   ```

   **`UseCollectionIsEmpty`** — Change:
   ```java
   // WRONG
   if (list.size() == 0)
   // RIGHT
   if (list.isEmpty())
   ```

   **`ConfusingTernary`** — Rewrite negated ternary:
   ```java
   // WRONG
   String result = !condition ? "a" : "b";
   // RIGHT
   String result = condition ? "b" : "a";
   ```

   **`OneDeclarationPerLine`** — Split multi-variable declarations onto separate lines.

   **`ConstantsInInterface`** — Move constants to an enum or a dedicated constants class.

3. **Re-run PMD after each batch of fixes**:
   ```bash
   mvn pmd:check
   ```

4. **Run full test suite** to confirm no regressions:
   ```bash
   mvn clean test
   ```

## Constraints

- Do not suppress PMD warnings with `@SuppressWarnings("PMD.*")` unless there is a genuine false positive
- Do not modify `ruleset.xml` to exclude rules
- Fixing `AtLeastOneConstructor` on `GlobalExceptionHandler` is correct — the empty constructor is intentional
- Do not change method signatures or API contracts while fixing lint

## Output Format

- Violations fixed: table with File | Rule | Fix Applied
- Remaining violations (if any): file, rule, reason not auto-fixable
- Final result: `mvn clean test` PASS / FAIL
