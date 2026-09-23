# 🧱 01 · Technology Stack

🌐 [Leer esto en Español](../es/01-stack-tecnologico.md) · ⬅️ [Back to index](README.md)

> Every piece of Mercury's stack, with the exact version pinned in `pom.xml`, and why it's there.

---

## ⚙️ Runtime

| Component | Version | Notes |
|---|---|---|
| ☕ Java | **21** (LTS) | `<java.version>21</java.version>` in `pom.xml` |
| 🍃 Spring Boot | **4.1.0** | `spring-boot-starter-parent` |
| ☁️ Spring Cloud | **2025.1.2** | BOM (`spring-cloud-dependencies`) — pins Config/Vault/OpenFeign versions |
| 📦 Packaging | Executable jar (`spring-boot-maven-plugin` repackage) | `BOOT-INF/classes` + `BOOT-INF/lib` |
| 🐳 Base image (Docker) | `amazoncorretto:21.0.11-alpine3.23` | Alpine — minimal image, non-root user (`jvapps`) |

---

## 🌐 Web / API layer

| Dependency | Use |
|---|---|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat |
| `springdoc-openapi-starter-webmvc-ui` | Serves Swagger UI + `/v3/api-docs` from the contract at `src/main/resources/api/openapi.yaml` |
| `springdoc-openapi-maven-plugin` | Generates `*Api` interfaces from the contract at build time (contract-first) |
| `spring-boot-starter-validation` | Bean Validation (`@Valid`, `@NotNull`, etc.) on the records in `api/v1/to` |
| `spring-boot-starter-actuator` | `/actuator/health` and other management endpoints |

> [!NOTE]
> The OpenAPI contract is the **source of truth**: controllers implement `*Api` interfaces generated/annotated from `openapi.yaml`, not the other way around. See [04 · Component Catalog](04-components.md).

---

## 🗄️ Persistence

| Dependency | Engine | Use in Mercury |
|---|---|---|
| `spring-boot-starter-data-jpa` + `postgresql` | PostgreSQL | Campaigns, metrics, templates, message records, users, verification codes — **durable** data |
| `spring-boot-starter-data-mongodb` | MongoDB | **Transient** message documents (`messages` — polymorphic Sms/Telegram/WhatsApp/Push — and the email queue) |
| `mapstruct` + `mapstruct-processor` | — | Entity ↔ TO mapping at compile time (no runtime reflection) |

DDL runs in `none` mode (`hibernate.ddl-auto: none`) — the PostgreSQL schema is managed outside Hibernate; Mercury never migrates it on its own.

---

## 📨 Messaging

| Dependency | Use |
|---|---|
| `spring-kafka` | Producer (publishes one message per recipient to the channel's topic) and consumers (`@KafkaListener`) |
| `spring-boot-starter-mail` (Jakarta Mail / SMTP) | The only channel with real delivery today: Email over SMTP |
| `telegrambots-client`, `telegrambots-longpolling`, `telegrambots-webhook`, `telegrambots-extensions` | Telegram Bot API dependencies — **present in `pom.xml` but not yet used by any `ChannelService`** on this branch (see [04 · Components](04-components.md#-known-gaps)) |
| `spring-boot-starter-freemarker` | Template engine for email bodies |

### Kafka serialization

- Producer: `JacksonJsonSerializer` (Spring Kafka 4.x) — **not** the deprecated `JsonSerializer`.
- Consumer: `ErrorHandlingDeserializer` wrapping `JacksonJsonDeserializer` — a malformed message doesn't take the listener down.

> [!IMPORTANT]
> Jackson 3 (`tools.jackson.*`, used by `JacksonJsonSerializer`) and Jackson 2 (`com.fasterxml.jackson.*`, used by the rest of Spring) **coexist** in this project. They are not interchangeable — watch your imports.

---

## ☁️ External configuration and secrets

| Dependency | Use |
|---|---|
| `spring-cloud-starter-bootstrap` | Enables the classic *bootstrap context* (`bootstrap.yml` is processed before `application.yml`) |
| `spring-cloud-starter-config` | Spring Cloud Config client — fetches `mercury-{profile}.yml` from a Git repo through the Config Server |
| `spring-cloud-starter-vault-config` | HashiCorp Vault client — fetches per-environment secrets (`KV v2`) |

See [06 · Environment Configuration](06-environment-configuration.md) for the full startup flow (Vault → Config Server → application).

---

## 🔐 Security

| Dependency | Use |
|---|---|
| `spring-cloud-starter-openfeign` | `BackboneClient` — validates session tokens against the Backbone service (OAuth2) |
| `security-oauth` (private PRX library) | Shared OAuth2 utilities |
| Custom JWT (`com.umdc.mercury.security`) | Application session token generation (`SessionJwtServiceImpl`) |

---

## 🧪 Quality and build

| Tool | Threshold / configuration |
|---|---|
| `maven-pmd-plugin` | Runs at the `test` phase; **0 violations** breaks the build (`ruleset.xml` at the repo root) |
| `jacoco-maven-plugin` | **70% line** (BUNDLE) and **50% branch** (PACKAGE), enforced at the `verify` phase |
| `junit-jupiter` + `mockito-core` | Unit tests |
| `spring-restdocs-mockmvc` | API documentation generated from integration tests |
| `jmh-core` / `jmh-generator-annprocess` | Microbenchmarks (`benchmark` profile) |
| `rewrite-maven-plugin` + `rewrite-spring` | Automated refactors (used for the Spring Boot 4 migrations) |

```bash
mvn -B -V -e clean verify          # full build with tests + coverage gate
mvn -Pcoverage clean test          # generates coverage XML for SonarCloud
mvn -Pbenchmark clean test         # runs the JMH benchmarks
```

---

## 🏢 Private PRX dependencies

| Dependency | Repository |
|---|---|
| `prx-commons`, `commons-services`, `security-oauth` | `https://repo.repsy.io/mvn/lmata/prx` — requires credentials in `~/.m2/settings.xml` |

---

## 🐳 Container infrastructure

| Tool | Role |
|---|---|
| Docker (no multi-stage — single image on top of a pre-built jar) | Packages the jar + baked-in certificates (`certs/mercury/`) |
| `docker-entrypoint.sh` | Imports `*.crt` files into the JVM's trust store at container start |
| Docker Compose | Orchestrates `mercury` (and, in the full development compose, `config-server`/`vault-server`) |

See [06 · Environment Configuration](06-environment-configuration.md) for the full build/deploy details.

---

*Generated from an exhaustive read of `pom.xml`, `bootstrap.yml` and the real source code — not from prior documentation or assumptions.*
