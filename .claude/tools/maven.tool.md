---
name: Maven
description: Tool for building, testing, and verifying the Mercury Spring Boot service
type: terminal
command-prefix: mvn
used-by: [orchestrator, developer, test-writer, code-reviewer, devops-engineer]
---

## Purpose

Maven is the build tool for Mercury. It manages compilation, dependency resolution, PMD static analysis, JUnit test execution, JaCoCo coverage enforcement, and release packaging.

## Available Commands

### Compile only (skip tests)
```bash
mvn -U clean package -DskipTests
```
Use when verifying that code compiles without running PMD or tests.

### Run all tests
```bash
mvn clean test
```
Compiles, runs PMD (fails on violations), and executes all JUnit 5 tests via Surefire.

### Run a single test class
```bash
mvn -Dtest=com.prx.mercury.api.v1.service.CampaignServiceImplTest surefire:test
```

### Run a single test method
```bash
mvn -Dtest=com.prx.mercury.api.v1.service.CampaignServiceImplTest#methodName surefire:test
```

### Full build with tests and JaCoCo coverage report
```bash
mvn -B -V -e clean verify
```
Runs compile + PMD + tests + JaCoCo threshold enforcement (70% line / 50% branch). Fails if thresholds are not met.

### Coverage profile (generates XML for SonarCloud)
```bash
mvn -Pcoverage clean test
```
Generates `target/site/jacoco/jacoco.xml` for SonarCloud integration.

### Run JMH benchmarks
```bash
mvn -Pbenchmark clean test
```
Runs JMH benchmark classes in `src/test/java/com/prx/mercury/benchmark/`.

## Output Locations

- Compiled classes: `target/classes/`
- Test classes: `target/test-classes/`
- JAR artifact: `target/mercury-0.0.1.jar`
- Surefire test reports: `target/surefire-reports/`
- JaCoCo HTML report: `target/site/jacoco/index.html`
- JaCoCo XML (SonarCloud): `target/site/jacoco/jacoco.xml`
- PMD reports: `target/pmd.xml`

## Notes

- Private PRX dependencies (`prx-commons`, `commons-services`, `security-oauth`) are hosted at `https://repo.repsy.io/mvn/lmata/prx` — requires credentials in `~/.m2/settings.xml`
- PMD runs at the `test` phase automatically — violations fail the build before tests execute
- JaCoCo thresholds are enforced at the `verify` phase only — `mvn clean test` does not check coverage
- The `-U` flag forces snapshot dependency update checks
