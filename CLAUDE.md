# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Compile only (skip tests)
mvn -U clean package -DskipTests

# Run all tests
mvn clean test

# Run a single test class
mvn -Dtest=com.prx.mercury.api.v1.service.CampaignServiceImplTest surefire:test

# Run a single test method
mvn -Dtest=com.prx.mercury.api.v1.service.CampaignServiceImplTest#methodName surefire:test

# Full build with tests and JaCoCo coverage report
mvn -B -V -e clean verify

# Coverage profile (generates XML for SonarCloud)
mvn -Pcoverage clean test

# Run JMH benchmarks
mvn -Pbenchmark clean test
```

PMD static analysis runs automatically at the `test` phase and fails the build on violations — `ruleset.xml` at the project root defines the active rules.

JaCoCo enforces **70% line coverage** (BUNDLE) and **50% branch coverage** (PACKAGE) at the `verify` phase.

## Architecture Overview

Mercury is a **multi-channel messaging microservice** built on Spring Boot 3.5.8 / Java 21. It exposes a REST API, queues messages via Kafka, sends emails via SMTP with FreeMarker templates, and persists data across PostgreSQL and MongoDB.

### Layered Request Flow

```
REST Controller (*Controller + *Api interface)
  └─> Service (*ServiceImpl)
        ├─> JPA Repositories (PostgreSQL)  ─> MapStruct Mappers ─> *TO records
        ├─> NoSQL Repositories (MongoDB)
        └─> KafkaTemplate (publish per-recipient messages to channel topics)

Kafka Listeners (MultiChannelListener)
  └─> MessageChannelRouter
        └─> ChannelService implementations (Email / SMS / Telegram / WhatsApp)

SendEmailScheduler (fixed-rate)
  └─> MessageProcessor
        ├─> processMessage()  — reads OPENED docs from MongoDB, sends via SMTP, updates status
        └─> updateMessageStatus() — reads SENT docs, writes MessageRecord to PostgreSQL, creates VerificationCode if applicable, deletes MongoDB doc
```

### Key Package Map

| Package | Purpose |
|---|---|
| `api/v1/controller` | Thin controllers implementing `*Api` interfaces (OpenAPI annotations on the interface) |
| `api/v1/service` | Business logic; async ops return `CompletableFuture` |
| `api/v1/to` | Immutable Java records: `*Request`, `*Response`, `*TO` suffixes |
| `jpa/sql/entity` + `jpa/sql/repository` | JPA entities and Spring Data repositories (PostgreSQL) |
| `jpa/nosql/document` + `jpa/nosql/repository` | MongoDB documents for in-flight email messages |
| `mapper` | MapStruct mappers bridging entities ↔ TOs |
| `kafka/listener` | `@KafkaListener` entry points |
| `kafka/consumer/service` | Per-channel service implementations (`ChannelService` contract) |
| `kafka/router` | `MessageChannelRouter` dispatches to the correct channel service |
| `scheduler` | `SendEmailScheduler` drives `MessageProcessor` on configurable fixed rates |
| `processor` | `MessageProcessor` — orchestrates the email send-and-persist lifecycle |
| `client` | `BackboneClient` — Feign client for auth/session validation against the Backbone service |
| `constant` | `ChannelType` enum (EMAIL, SMS, TELEGRAM, WHATSAPP, PUSH) and delivery status types |
| `config` | Spring `@Configuration` classes for Mail, FreeMarker, Database, Kafka |
| `security` | JWT session token generation (`SessionJwtServiceImpl`) |

### Persistence Model

- **PostgreSQL** (via JPA/Hibernate, DDL `none`): campaigns, campaign metrics, channel types, templates, message records, verification codes, users, applications.
- **MongoDB**: transient `EmailMessageDocument` records. Messages are written here when received from Kafka, processed by `MessageProcessor`, then deleted after being moved to PostgreSQL.
- **Spring Cloud Config + HashiCorp Vault**: all credentials and environment-specific config come from Vault at startup via `bootstrap.yml`.

### Campaign Flow

1. `POST /api/v1/campaigns` → `CampaignServiceImpl.createCampaign()` (async)
2. Validates channel type (enabled), resolves template, persists `CampaignEntity` + initial `CampaignMetricsEntity`
3. Publishes one Kafka message per recipient to the channel topic (e.g., `email-topic`)
4. Kafka consumer picks up messages; `SendEmailScheduler` drives `MessageProcessor` to send and reconcile

## Key Conventions

- **Controller pattern**: every controller implements a `*Api` interface. OpenAPI (`@Operation`, `@ApiResponse`) annotations belong on the interface, not the controller class.
- **DTO location**: all transfer objects go in `api/v1/to` as Java records with Bean Validation annotations.
- **Scheduler rates**: use `${prx.scheduler.*}` property placeholders, not hardcoded `fixedRate` values.
- **Channel services**: new channel implementations must implement `send`, `updateStatus`, and `findByDeliveryStatus`.
- **Logging**: SLF4J `LoggerFactory` only — no other logging frameworks. Async methods use `CompletableFuture`.
- **MapStruct mappers**: place in `mapper/` package; processor is on the annotation processing path, so no manual `new` instantiation of mappers.
- **Kafka**: `PRX_KAFKA_AUTO_STARTUP=false` by default (set in `bootstrap.yml`) so listeners don't crash local dev without a running broker. Set to `true` in deployed environments.

## External Dependencies

- **prx-commons**, **commons-services**, **security-oauth** — private PRX libraries hosted at `https://repo.repsy.io/mvn/lmata/prx`. Required for compilation; credentials must be in `~/.m2/settings.xml`.
- **Backbone service**: Feign client at `https://prx-qa.backbone.tst/backbone` for session token validation (`BackboneClient`).