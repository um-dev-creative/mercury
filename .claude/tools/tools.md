---
title: Mercury Tools Index
---

## Tool Registry

| File | Tool | Used By | Key Commands |
|---|---|---|---|
| `maven.tool.md` | Maven | orchestrator, developer, test-writer, code-reviewer, devops-engineer | `mvn -U clean package -DskipTests`, `mvn clean test`, `mvn clean verify`, `mvn -Pcoverage clean test`, `mvn -Pbenchmark clean test` |
| `git.tool.md` | Git | orchestrator, developer, devops-engineer | `git checkout -b feature/<id>-<desc>`, `git commit -m "feat: ..."`, `git tag -a v<ver>` |
| `pmd.tool.md` | PMD | developer, code-reviewer, devops-engineer | `mvn pmd:check`, `mvn pmd:pmd` (runs at `test` phase automatically) |
| `jacoco.tool.md` | JaCoCo | test-writer, devops-engineer, code-reviewer | `mvn clean verify` (threshold gate), `mvn -Pcoverage clean test` (XML for SonarCloud) |
| `docker-build.tool.md` | Docker | devops-engineer | `docker build -t mercury:latest .`, `docker run --env-file default.env -p 8080:8080 mercury:latest` |
| `kafka-cli.tool.md` | Kafka CLI | devops-engineer, developer | `kafka-topics.sh --create`, `kafka-console-consumer.sh`, `kafka-consumer-groups.sh --describe` |

## Coverage Thresholds

| Metric | Counter | Threshold |
|---|---|---|
| Line | BUNDLE | 70% |
| Branch | PACKAGE | 50% |

## Build Gate Order

```
mvn -U clean package -DskipTests  → compile check (fast)
mvn clean test                     → PMD + unit tests
mvn clean verify                   → PMD + tests + JaCoCo thresholds
```
