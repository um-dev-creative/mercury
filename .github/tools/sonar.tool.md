# Tool: SonarCloud / SonarQube

**Purpose:** Run SonarCloud and SonarQube code quality analysis for Mercury.

## Mercury SonarCloud Integration

SonarCloud is configured in `.github/workflows/ci.yml` and `.github/workflows/build.yml`.
Analysis runs automatically on every push to `main` and on pull requests.

## Prerequisites
```
SONAR_TOKEN — GitHub Actions secret; must be set in repository secrets
```

## Manual Scan Commands

### SonarCloud Scan (with coverage)
```bash
# 1. Generate JaCoCo XML first
mvn -Pcoverage clean test

# 2. Run SonarCloud analysis
mvn sonar:sonar \
  -Dsonar.token="${SONAR_TOKEN}" \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=um-dev-creative \
  -Dsonar.projectKey=um-dev-creative_mercury
```

### SonarQube Scan (self-hosted)
```bash
mvn sonar:sonar \
  -Dsonar.token="${SONAR_TOKEN}" \
  -Dsonar.host.url="${SONAR_HOST_URL}"
```

### Combined: Coverage + Sonar in One Command
```bash
mvn -B -V -e clean verify sonar:sonar \
  -Dsonar.token="${SONAR_TOKEN}" \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=um-dev-creative \
  -Dsonar.projectKey=um-dev-creative_mercury
```

## GitHub Actions Workflow Reference

### ci.yml (SonarCloud)
```yaml
- name: Build and analyze
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: |
    mvn -B -V -e clean verify -Pcoverage
    mvn sonar:sonar -Dsonar.token=${{ secrets.SONAR_TOKEN }}
```

### build.yml (SonarQube)
Configured separately for self-hosted SonarQube instance.

## Quality Gate Requirements
Mercury must pass SonarCloud Quality Gate before PR merge:
- Coverage ≥ 70% (line) — matches JaCoCo local gate
- Duplications < 3%
- Maintainability rating: A
- Reliability rating: A
- Security rating: A
- No new blocker/critical issues

## Qodana
Separate quality analysis via `.github/workflows/qodana_code_quality.yml`.
Config at `qodana.yaml` in repo root.

## Report Locations
- SonarCloud dashboard: https://sonarcloud.io/project/overview?id=um-dev-creative_mercury
- Local JaCoCo (input to Sonar): `target/site/jacoco/jacoco.xml`

## Notes
- Never hardcode `SONAR_TOKEN` — always use GitHub Actions secret or env var
- SonarCloud scans are incremental on PRs (new code only)
- Full analysis runs on pushes to `main`
- `sonar-project.properties` or `pom.xml` `<sonar.*>` properties define project config
