---
name: Fix PMD
description: Fix all PMD violations so mvn clean test passes without touching business logic
---

Fix all PMD violations in the current changes.

## Instructions

1. Run PMD to see violations:
   ```bash
   mvn clean test 2>&1 | grep -A 5 "PMD\|Rule violation"
   ```

2. Apply these fixes per rule:

   | Rule | Fix |
   |---|---|
   | `AtLeastOneConstructor` | Add `public ClassName() {}` or all-args constructor |
   | `UnusedImports` | Remove the import statement |
   | `EmptyCatchBlock` | Add a comment explaining why exception is intentionally ignored |
   | `AvoidDuplicateLiterals` | Extract to `private static final String CONSTANT_NAME` |
   | `UseUtilityClass` | Add `private ClassName() { throw new UnsupportedOperationException(); }` |
   | `LongVariable` / `ShortVariable` | Rename to a clear, appropriately-sized identifier |

3. Never use `@SuppressWarnings("PMD.*")` unless it is a genuine false positive with a comment explaining why.

4. Verify:
   ```bash
   mvn clean test        # Must reach Surefire (tests run, not just PMD)
   mvn -B -V -e clean verify  # Full gate including JaCoCo
   ```

Output: list of fixes applied (rule → file:line → change), final build status.
