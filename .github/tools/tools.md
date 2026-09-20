# Mercury Agent Tools — Index

All tool reference files available to Mercury agents.

| Tool | File | Used By | Purpose |
|---|---|---|---|
| Maven | `maven.tool.md` | developer, test-writer, code-reviewer, devops-engineer, database-architect | Build, test, verify, release |
| Docker Build | `docker-build.tool.md` | devops-engineer | Build and run Mercury Docker image |
| Git | `git.tool.md` | developer, devops-engineer, orchestrator | Branching, commits, tagging |
| GitHub CLI | `github-cli.tool.md` | devops-engineer, orchestrator | PRs, releases, workflow status |
| PMD | `pmd.tool.md` | code-reviewer, developer | Static analysis via ruleset.xml |
| JaCoCo | `jacoco.tool.md` | test-writer, code-reviewer | Coverage measurement and enforcement |
| Keytool | `keytool.tool.md` | security-reviewer, devops-engineer | mercury.jks / umdc-truststore.jks management |
| SonarCloud | `sonar.tool.md` | devops-engineer, code-reviewer | Code quality analysis |
| Flyway | `flyway.tool.md` | database-architect, devops-engineer | DB migration naming, placement, validation |

## Quick Reference

### Build Commands
```bash
mvn -U clean package -DskipTests       # fast compile
mvn clean test                          # PMD + JUnit 5
mvn -B -V -e clean verify              # full CI gate
mvn -Pcoverage clean test              # JaCoCo XML for SonarCloud
```

### Test Specific Class
```bash
mvn -Dtest=com.umdc.mercury.api.v1.service.CampaignServiceImplTest surefire:test
```

### Docker
```bash
docker build -t mercury:latest .
docker compose up -d
```

### Sonar
```bash
mvn -Pcoverage clean test && mvn sonar:sonar -Dsonar.token="${SONAR_TOKEN}"
```

### Flyway
```bash
# Migration file: src/main/resources/db/migration/V<YYYYMMDD><seq>__<desc>.sql
mvn flyway:validate
mvn flyway:info
```
