---
name: Fix Lint Violations
description: >
  Fix all PMD static analysis violations in Mercury so that `mvn clean test` passes
  without touching test coverage or business logic.
mode: agent
agent: developer
tools: [read_file, grep_search, replace_string_in_file, run_in_terminal]
---

## Input Variables

- `${violationReport}` — paste the PMD output or `target/pmd.xml` content (optional; agent will run PMD if not provided)

## Steps

1. **Run PMD to get the current violation list**:
   ```bash
   mvn clean test 2>&1 | grep -A 3 "PMD Failure\|Rule violation"
   ```
   Or parse `target/pmd.xml` directly.

2. **For each violation, apply the canonical fix**:

   | PMD Rule | Fix |
   |---|---|
   | `AtLeastOneConstructor` | Add `public ClassName() {}` or an all-args constructor |
   | `UnusedImports` | Remove the import statement |
   | `EmptyCatchBlock` | Add a comment explaining why the exception is intentionally ignored |
   | `AvoidDuplicateLiterals` | Extract to a `private static final String CONSTANT` |
   | `TooManyMethods` | If over limit, extract a helper — but only if the split is natural |
   | `LongVariable` | Shorten variable name while preserving clarity |
   | `ShortVariable` | Rename to a meaningful identifier |
   | `UseUtilityClass` | Add `private ClassName() { throw new UnsupportedOperationException(); }` |

3. **Never suppress with `@SuppressWarnings("PMD.*")`** unless the rule is a false positive that cannot be resolved by code change. If suppression is the only option, document why in a comment above the annotation.

4. **After fixing all violations, verify**:
   ```bash
   mvn clean test
   ```
   The build must reach the Surefire phase (all tests run) without a PMD failure.

5. **Run full verify to confirm coverage is unaffected**:
   ```bash
   mvn -B -V -e clean verify
   ```

## Constraints

- Do not change method signatures or public APIs to fix PMD — find a PMD-compliant way within the existing contract
- Do not add, remove, or alter business logic
- Do not remove or weaken test assertions

## Output

For each fixed violation:
- Rule name
- File path and line
- Change applied (before → after)

Final status: `mvn clean test` → PASS / FAIL with remaining count.
