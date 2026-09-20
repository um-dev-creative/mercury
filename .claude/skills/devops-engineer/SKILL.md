---
agent: devops-engineer
version: 1.0
---

## 1. Project-Specific Patterns

### Build lifecycle
```
mvn -U clean package -DskipTests   # fast compile check
mvn clean test                      # compile + PMD + unit tests
mvn clean verify                    # compile + PMD + tests + JaCoCo thresholds
mvn -Pcoverage clean test           # generates jacoco.xml for SonarCloud
mvn -Pbenchmark clean test          # runs JMH benchmarks
```

### Kafka local dev
`PRX_KAFKA_AUTO_STARTUP=false` is the default in `bootstrap.yml`. This prevents listener containers from crashing when no Kafka broker is available locally. Set `PRX_KAFKA_AUTO_STARTUP=true` in deployed environments.

Topics defined in `bootstrap.yml`:
- `email-topic` → `EmailChannelService`
- `sms-topic` → `SmsChannelService`
- `telegram-topic` → `TelegramChannelService`

### Schema migrations
DDL strategy is `none`. Schema changes require SQL scripts in `src/main/resources/db/`.

### Docker
`Dockerfile` is present at the project root. Builds the Spring Boot fat jar.

## 2. Naming Conventions

- Environment variables: `UPPER_SNAKE_CASE` (e.g., `PRX_KAFKA_AUTO_STARTUP`, `APP_TOKEN_SECRET`)
- Scheduler properties: `prx.scheduler.<task>.fixed-rate` in `bootstrap.yml`
- SQL migration files: `src/main/resources/db/<version>__<description>.sql`

## 3. Error Handling

- Build failures from missing `~/.m2/settings.xml` credentials → add `https://repo.repsy.io/mvn/lmata/prx` repo credentials
- PMD failure at test phase → fix violations in `ruleset.xml` active rules
- JaCoCo failure at verify → add tests to reach 70% line / 50% branch
- Kafka listener startup failure → set `PRX_KAFKA_AUTO_STARTUP=false` for local dev

## 4. Key Files

- `pom.xml` — version `0.0.1`, parent `spring-boot-starter-parent:3.5.8`
- `src/main/resources/bootstrap.yml` — all runtime config and Kafka toggles
- `Dockerfile` — project root, Spring Boot fat jar build
- `default.env` — local environment variable defaults
- `environment_variables.md` — documentation of all required env vars
- `CHANGELOG` — release history
- `ruleset.xml` — PMD rules, fails build on violation
- `src/main/resources/db/` — SQL migration scripts

## 5. Constraints

- Private PRX dependencies (`prx-commons`, `commons-services`, `security-oauth`) require `~/.m2/settings.xml` with `https://repo.repsy.io/mvn/lmata/prx` credentials
- `PRX_KAFKA_AUTO_STARTUP` must remain `false` as default in `bootstrap.yml`
- Version bumps in `pom.xml` require `CHANGELOG` update
- New env vars must be added to both `environment_variables.md` and `default.env`

## 6. Checklist

- [ ] `mvn -U clean package -DskipTests` succeeds
- [ ] `mvn clean verify` passes (PMD + JaCoCo)
- [ ] New env vars in `environment_variables.md` and `default.env`
- [ ] Kafka topics referenced in `bootstrap.yml`, not hardcoded
- [ ] `PRX_KAFKA_AUTO_STARTUP=false` default preserved in `bootstrap.yml`
- [ ] SQL migration script created for schema changes
- [ ] `CHANGELOG` updated for release
- [ ] Docker image builds: `docker build -t mercury:latest .`
