---
name: Improve Coverage
description: >
  Raise Mercury JaCoCo coverage above the 70% line / 50% branch gate by identifying
  the most impactful uncovered classes and adding targeted tests.
mode: agent
agent: test-writer
tools: [read_file, grep_search, run_in_terminal, create_file, insert_edit_into_file]
---

## Input Variables

- `${currentLinePercent}` — current LINE coverage percentage (from JaCoCo report or CI output)
- `${currentBranchPercent}` — current BRANCH coverage percentage
- `${failingModule}` — module or package with the largest gap (optional; agent will identify if not provided)

## Steps

1. **Generate the coverage report**:
   ```bash
   mvn -Pcoverage clean test
   ```
   Open `target/site/jacoco/index.html` or parse `target/site/jacoco/jacoco.xml`.

2. **Identify top uncovered classes** — sort by missed lines descending:
   ```bash
   # Extract from jacoco.xml — classes with most missed lines
   grep -o 'name="[^"]*" sourcefilename[^/]*/>' target/site/jacoco/jacoco.xml | head -20
   ```
   Prioritize: services → controllers → mappers → channel services.

3. **For each target class, add tests** following the `write-unit-tests.prompt.md` pattern:
   - Focus on uncovered branches first (highest branch-coverage payoff)
   - Exception paths are usually the cheapest coverage wins
   - `@ParameterizedTest` for boundary values

4. **Re-run after each class** to track incremental gain:
   ```bash
   mvn -Pcoverage clean test
   ```

5. **Stop when**:
   - LINE ≥ 75% (5% above the 70% gate for safety margin)
   - BRANCH ≥ 55% (5% above the 50% gate)

6. **Run full verify to confirm gates pass**:
   ```bash
   mvn -B -V -e clean verify
   ```

## Prioritization Guide

| Class type | Coverage payoff |
|---|---|
| `*ServiceImpl` | HIGH — complex logic, many branches |
| `*Controller` | MEDIUM — thin, but `@Valid` paths need testing |
| `*Mapper` | LOW — MapStruct-generated, but custom methods need coverage |
| `MessageProcessor` | HIGH — email lifecycle branches |
| `ChannelService` impls | HIGH — `send`/`updateStatus`/`findByDeliveryStatus` all need coverage |

## Constraints

- Do not lower `@JacocoIgnore` thresholds — raise coverage instead
- Do not add empty/trivial tests that inflate numbers without testing real behavior
- Do not modify production code for testability

## Output

Before/after table:
| Class | Before LINE% | After LINE% | Before BRANCH% | After BRANCH% |
|---|---|---|---|---|

Final: `mvn -B -V -e clean verify` → PASS / FAIL
