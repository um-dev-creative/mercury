---
name: JaCoCo
description: Tool for code coverage measurement and enforcement in Mercury — enforces 70% line / 50% branch at mvn verify phase
type: terminal
command-prefix: mvn
used-by: [test-writer, devops-engineer, code-reviewer]
---

## Purpose

JaCoCo measures code coverage and enforces minimum thresholds. Thresholds are checked at the Maven `verify` phase only. Coverage reports are generated at the `test` phase.

## Available Commands

### Run with coverage enforcement (fails if below threshold)
```bash
mvn -B -V -e clean verify
```
Runs all tests, generates coverage data, then checks thresholds. Fails with `BUILD FAILURE` if below minimums.

### Generate coverage report only (no threshold check)
```bash
mvn clean test
```
Generates `target/site/jacoco/index.html` but does NOT enforce thresholds.

### Coverage profile for SonarCloud XML
```bash
mvn -Pcoverage clean test
```
Activates the `coverage` profile which generates `target/site/jacoco/jacoco.xml` for upload to SonarCloud.

## Thresholds

| Counter | Element | Minimum |
|---|---|---|
| LINE | BUNDLE | 70% |
| BRANCH | PACKAGE | 50% |

## Output Locations

- HTML report: `target/site/jacoco/index.html`
- XML report: `target/site/jacoco/jacoco.xml`
- Raw exec data: `target/jacoco.exec`

## Reading the Report

Open `target/site/jacoco/index.html` in a browser. Navigate to:
- `com.prx.mercury.api.v1.service` — service layer coverage (most important)
- `com.prx.mercury.processor` — `MessageProcessor` coverage
- `com.prx.mercury.mapper` — MapStruct mapper coverage
- `com.prx.mercury.kafka` — Kafka listener/router coverage

## Notes

- JaCoCo thresholds are enforced at `verify` — `mvn clean test` does NOT enforce them
- The `benchmark` profile excludes JMH benchmarks from coverage counting
- MapStruct-generated mapper implementations are instrumented — add tests for mapper edge cases to boost branch coverage
- Async `CompletableFuture` branches count toward branch coverage — test both completion and exception paths
