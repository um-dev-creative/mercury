---
name: review-code
description: Reviews a Mercury diff or PR for correctness, PMD compliance, and convention adherence
mode: ask
agent: code-reviewer
tools: [Read, Bash]
---

## Input Variables

- `${prNumber}` — GitHub PR number, or `DIFF` to review current working tree changes
- `${reviewDepth}` — `quick` (blocking issues only) or `full` (all severity levels)

## Steps

1. **Get the diff to review**:
   - For PR: `gh pr diff ${prNumber}`
   - For working tree: `git diff HEAD`

2. **Check PMD compliance** by running:
   ```bash
   mvn pmd:check
   ```
   List any violations found.

3. **Check layering conventions**:
   - Scan for `@Operation` or `@ApiResponse` on `*Controller` classes (not allowed)
   - Scan for `fixedRate = ` numeric literals in `@Scheduled` (must use `fixedRateString = "${prx.scheduler.*}"`)
   - Scan for `new CampaignMapper()` or `new <Any>Mapper()` (must be Spring beans)
   - Scan for `System.out` or `System.err` (must use SLF4J)

4. **Check exception handler coverage**:
   - For every new exception class introduced, verify it is registered in `GlobalExceptionHandler`
   - For every `throw new` statement, verify the exception type matches the intended HTTP status

5. **Check test coverage**:
   - For every new public method added, verify a corresponding test exists or is in the same PR
   - For every new `if/else` branch, verify both paths have test coverage

6. **Check async patterns**:
   - `CompletableFuture` chains must not silently swallow exceptions
   - `exceptionally()` handlers must log and re-throw or wrap properly

7. **Check logger usage**:
   - `logger.warn()` for 4xx client errors
   - `logger.error(msg, exception)` for 5xx server errors — stack trace must be passed
   - No `logger.error(exception.getMessage())` without the exception object

## Constraints

- BLOCKER findings must be resolved before merge
- MAJOR findings should be resolved before merge
- MINOR findings are optional improvements
- Do not approve PRs that fail `mvn clean test`

## Output Format

Findings table:
| File | Line | Severity | Rule/Convention | Description | Suggested Fix |
|---|---|---|---|---|---|

Summary:
- PMD: PASS / FAIL (N violations)
- Layering: PASS / N violations
- Exception coverage: PASS / N gaps
- Async safety: PASS / N issues
- Logger usage: PASS / N issues

**Verdict: APPROVED / REQUEST_CHANGES**
