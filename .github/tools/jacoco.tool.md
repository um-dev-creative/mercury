# Tool: JaCoCo Coverage

**Purpose:** Measure and enforce code coverage thresholds for Mercury.

## Coverage Thresholds (CI Gate)
| Metric | Threshold | Enforced By |
|---|---|---|
| Line coverage | ≥ 70% | `mvn clean verify` |
| Branch coverage | ≥ 50% | `mvn clean verify` |

Failure to meet thresholds causes `BUILD FAILURE` during verify phase.

## Commands

### Run Tests and Generate Coverage
```bash
mvn clean test
# Coverage report: target/site/jacoco/index.html
# XML report:      target/site/jacoco/jacoco.xml
```

### Run Coverage with Profile (SonarCloud)
```bash
mvn -Pcoverage clean test
# Generates: target/site/jacoco/jacoco.xml (consumed by SonarCloud)
```

### Full Verify with Coverage Enforcement
```bash
mvn -B -V -e clean verify
# Fails build if line < 70% OR branch < 50%
```

### Single Class Coverage (after full test run)
```bash
# 1. Run all tests to generate jacoco.exec
mvn clean test

# 2. Parse XML for specific class
grep -A5 "com/umdc/mercury/api/v1/service/CampaignServiceImpl" \
  target/site/jacoco/jacoco.xml
```

## Report Locations
```
target/site/jacoco/index.html      — human-readable HTML report
target/site/jacoco/jacoco.xml      — XML report (used by SonarCloud)
target/jacoco.exec                 — binary execution data
```

## Reading JaCoCo XML Report
```xml
<!-- Example JaCoCo XML entry -->
<class name="com/umdc/mercury/api/v1/service/CampaignServiceImpl">
  <counter type="LINE" missed="5" covered="35"/>       <!-- 87.5% line -->
  <counter type="BRANCH" missed="2" covered="8"/>      <!-- 80% branch -->
</class>
```
Formula: `covered / (covered + missed) × 100`

## Coverage Gaps — How to Identify
```bash
# Open HTML report in browser
open target/site/jacoco/index.html

# Or grep XML for classes below threshold
grep -B2 'type="LINE"' target/site/jacoco/jacoco.xml | \
  awk '/missed/ {split($0,a,"\""); if (a[4]+0 > 0) print}'
```

## Excluded Classes (typical patterns)
The following are typically excluded from coverage enforcement:
- `*Application.java` (entry point)
- `*Config.java` (Spring config classes)
- `*Exception.java` (simple exception wrappers)
- DTO records in `api/v1/to/` (generated equals/hashCode)

Check `pom.xml` JaCoCo plugin configuration for current exclusions.

## Tips for Reaching Thresholds
1. Write `@Test` for each service method (happy path + exception branches)
2. Use `assertThrows` to cover exception paths (branches)
3. Use `@ParameterizedTest` for multiple input variations
4. Mock dependencies — do NOT skip testing due to infrastructure
5. Check HTML report's "Missed" column to find untested branches
