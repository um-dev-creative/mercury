# DevOps Engineer SKILL

## Project-Specific Patterns

### bootstrap.yml Structure Pattern
```yaml
spring:
  application:
    name: mercury
  cloud:
    vault:
      host: ${VAULT_HOST}
      port: ${VAULT_PORT}
      token: ${VAULT_TOKEN}
      scheme: https
      kv:
        enabled: true
        backend: secret
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

prx:
  consumer:
    topics:
      notifications: ${KAFKA_TOPIC_NOTIFICATIONS}
      campaigns: ${KAFKA_TOPIC_CAMPAIGNS}
      # Add new topics here — never in application code
  scheduler:
    campaign:
      rate: ${SCHEDULER_CAMPAIGN_RATE_MS}
      # Add new scheduler rates here — never hardcoded in @Scheduled
  ssl:
    keystore:
      password: ${SSL_KEYSTORE_PASSWORD}
    truststore:
      password: ${SSL_TRUSTSTORE_PASSWORD}
```

### Docker Build Pattern
```dockerfile
# Dockerfile at repo root — multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/mercury-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### GitHub Actions Pattern
```yaml
# .github/workflows/ci.yml — must keep JaCoCo gates
- name: Run tests and coverage
  run: mvn -B -V -e clean verify -Pcoverage

- name: SonarCloud scan
  run: mvn sonar:sonar -Dsonar.token=${{ secrets.SONAR_TOKEN }}
```

### Release Tag Pattern
```bash
# Semantic versioning: v<MAJOR>.<MINOR>.<PATCH>
mvn versions:set -DnewVersion=1.2.3
git add pom.xml
git commit -m "chore(release): bump version to 1.2.3"
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin main --tags
```

### Flyway Migration Placement
```
src/main/resources/db/migration/
├── V20250101__initial_schema.sql
├── V20250102__add_campaign_status.sql
└── V20250201__add_verification_code_table.sql

Naming: V<YYYYMMDD><seq>__description_with_underscores.sql
Example: V20250315001__add_message_record_index.sql
```

### Kafka Topic Governance
- New topics MUST be added to `bootstrap.yml` under `prx.consumer.topics.*`
- Topic names resolved from environment variables backed by Vault
- `@KafkaListener(topics = "${prx.consumer.topics.<name>}")` — no literals
- Consumer group IDs also from `bootstrap.yml`: `${prx.consumer.group.*}`

## Naming Conventions
- Release tags: `v<MAJOR>.<MINOR>.<PATCH>` (e.g., `v1.4.0`)
- Docker image: `mercury:<tag>` where `<tag>` matches release tag
- Migration files: `V<YYYYMMDD><seq>__<description>.sql`
- Kafka topic properties: `prx.consumer.topics.<feature-name>`
- Scheduler rate properties: `prx.scheduler.<feature-name>.rate`
- Workflow files: `.github/workflows/<purpose>.yml`

## Error Handling
- CI build failure → check `mvn -B -V -e clean verify` output for PMD / test / JaCoCo gate
- Docker build failure → check `FROM` image availability and `mvn` build step
- Vault connection failure → verify `${VAULT_HOST}`, `${VAULT_PORT}`, `${VAULT_TOKEN}` env vars
- Kafka connection failure → verify `${KAFKA_BOOTSTRAP_SERVERS}` and topic existence
- Flyway migration failure → check `V<version>__` filename collision or SQL syntax error

## Key Files
- `src/main/resources/bootstrap.yml` — all external config, Vault, Kafka topics, scheduler rates
- `Dockerfile` — multi-stage Docker build at repo root
- `docker-compose.yml` — local development stack (PostgreSQL, MongoDB, Kafka, Vault)
- `.github/workflows/build.yml` — SonarQube workflow
- `.github/workflows/ci.yml` — main CI: compile → test → JaCoCo → SonarCloud
- `.github/workflows/qodana_code_quality.yml` — Qodana quality gate
- `.github/dependabot.yml` — dependency update automation (Maven + Actions)
- `src/main/resources/db/migration/` — Flyway SQL scripts
- `pom.xml` — Maven POM, plugin versions, profiles
- `default.env` — local development environment variable defaults

## Constraints
- NEVER add Kafka topic strings directly to Java source — only `bootstrap.yml`
- NEVER add scheduler rates as literals — only `bootstrap.yml`
- NEVER remove JaCoCo 70% line / 50% branch gates from CI workflows
- NEVER modify existing Flyway migration files after they have been applied
- NEVER push secrets to `bootstrap.yml` or `application.yml` — Vault only
- NEVER change release tag format from `v<MAJOR>.<MINOR>.<PATCH>`
- NEVER skip `mvn -B -V -e clean verify` in the CI pipeline

## Checklist
- [ ] New Kafka topics added to `bootstrap.yml` under `prx.consumer.topics.*`
- [ ] New scheduler rates added to `bootstrap.yml` under `prx.scheduler.*`
- [ ] `bootstrap.yml` uses `${ENV_VAR}` for all secrets (no literals)
- [ ] Flyway migration file named `V<YYYYMMDD><seq>__<description>.sql`
- [ ] `Dockerfile` builds successfully with `docker build -t mercury:test .`
- [ ] CI workflow retains JaCoCo line ≥ 70% / branch ≥ 50% gates
- [ ] Release tag follows `v<MAJOR>.<MINOR>.<PATCH>` format
- [ ] `dependabot.yml` covers both `maven` and `github-actions` ecosystems
- [ ] `CHANGELOG` updated for releases
