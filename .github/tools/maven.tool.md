# Tool: Maven

**Purpose:** Build, test, verify, and release Mercury via Apache Maven.

## Available Commands

### Fast Compile (skip tests)
```bash
mvn -U clean package -DskipTests
```
Use when: verifying compilation only, before running tests separately.

### Run All Tests
```bash
mvn clean test
```
Runs: PMD static analysis + JUnit 5 tests via maven-surefire-plugin.

### Full Verify (CI gate)
```bash
mvn -B -V -e clean verify
```
Runs: PMD + JUnit 5 + JaCoCo coverage enforcement.
- `-B` batch mode (no interactive prompts)
- `-V` display Maven version info
- `-e` show full stack traces on error
Must pass before any PR merge.

### Coverage Profile (JaCoCo XML for SonarCloud)
```bash
mvn -Pcoverage clean test
```
Generates: `target/site/jacoco/jacoco.xml` (consumed by SonarCloud scan).

### Single Test Class
```bash
mvn -Dtest=com.umdc.mercury.api.v1.service.CampaignServiceImplTest surefire:test
```
Use the fully qualified class name. Useful for targeted debugging.

### Update Version
```bash
mvn versions:set -DnewVersion=1.4.0
mvn versions:commit
```
Use during release process. Modifies `pom.xml` only.

### Dependency Tree
```bash
mvn dependency:tree
```
Use to diagnose classpath conflicts or CVE surface.

### Update Snapshots
```bash
mvn -U clean package -DskipTests
```
`-U` forces snapshot updates from remote repositories.

## Key Files
- `pom.xml` — project definition, plugin config, profiles
- `ruleset.xml` — PMD ruleset referenced by maven-pmd-plugin
- `target/` — build output (not committed)
- `target/site/jacoco/` — JaCoCo reports
- `target/surefire-reports/` — test reports

## JaCoCo Thresholds (enforced by `verify`)
| Metric | Threshold | Failure |
|---|---|---|
| Line coverage | ≥ 70% | BUILD FAILURE |
| Branch coverage | ≥ 50% | BUILD FAILURE |

## PMD
- Runs automatically during `mvn clean test` and `mvn clean verify`
- Ruleset: `ruleset.xml` at repo root
- Failures cause `BUILD FAILURE`

## Profiles
| Profile | Command | Purpose |
|---|---|---|
| default | `mvn clean test` | PMD + tests |
| coverage | `mvn -Pcoverage clean test` | JaCoCo XML for Sonar |
