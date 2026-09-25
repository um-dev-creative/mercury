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

Mercury is a **multi-channel messaging microservice** built on Spring Boot 4.1.0 / Java 25 (LTS). It exposes a REST API, queues messages via Kafka, sends emails via SMTP with FreeMarker templates, and persists data across PostgreSQL and MongoDB.

### Layered Request Flow

```
REST Controller (*Controller + *Api interface)
  └─> Service (*ServiceImpl)
        ├─> JPA Repositories (PostgreSQL)  ─> MapStruct Mappers ─> *TO records
        ├─> NoSQL Repositories (MongoDB)
        └─> KafkaTemplate (publish per-recipient messages to channel topics)

Kafka Listeners (MultiChannelListener: SMS / Telegram / WhatsApp / Push)
  └─> MessageChannelRouter (resolves the template, then dispatches)
        └─> ChannelService implementations (Sms / Telegram / WhatsApp / Push)
              └─> MessageNSRepository (MongoDB, polymorphic "messages" collection)

SendEmailScheduler (fixed-rate)
  └─> MessageProcessor
        ├─> processMessage()  — reads OPENED docs from MongoDB, sends via SMTP, updates status
        └─> updateMessageStatus() — reads SENT docs, writes MessageRecord to PostgreSQL, creates VerificationCode if applicable, deletes MongoDB doc
```

> **Known gaps (as of the Spring Boot 4 upgrade branch):**
> - A second, unfinished messaging design ("Nexus") exists in parallel: `NotificationEventListener` actively consumes a unified `${umdc.consumer.topics.notification}` topic via `NotificationEventConsumerServiceImpl`, but nothing in the campaign-creation flow publishes to it yet, and its per-channel handlers only log (no persistence or send logic). Treat it as experimental/unwired until a decision is made to migrate the producer side to it.
> - None of the `ChannelService` implementations (Email/SMS/Telegram/WhatsApp/Push) call a real provider yet — they persist the message so it's ready once an SMTP/Twilio/Telegram Bot API/WhatsApp Cloud API/FCM-APNs integration is added.

### Key Package Map

| Package | Purpose |
|---|---|
| `api/v1/controller` | Thin controllers implementing `*Api` interfaces (OpenAPI annotations on the interface) |
| `api/v1/service` | Business logic; async ops return `CompletableFuture` |
| `api/v1/to` | Immutable Java records: `*Request`, `*Response`, `*TO` suffixes |
| `jpa/sql/entity` + `jpa/sql/repository` | JPA entities and Spring Data repositories (PostgreSQL) |
| `jpa/nosql/document` + `jpa/nosql/repository` | MongoDB documents for in-flight messages (`MessageDocument` is the polymorphic base for Sms/Telegram/WhatsApp/Push; `EmailMessageDocument` is separate) |
| `mapper` | MapStruct mappers bridging entities ↔ TOs |
| `kafka/listener` | `@KafkaListener` entry points — `MultiChannelListener` (SMS/Telegram/WhatsApp/Push), `MercuryEmailListener` (email), `NotificationEventListener` (experimental unified "Nexus" topic, see Known gaps above) |
| `kafka/consumer/service` | Per-channel service implementations (`ChannelService` contract); email is handled separately by `EmailMessageConsumerService`, not by a `ChannelService` |
| `kafka/router` | `MessageChannelRouter` resolves the template via `TemplateDefinedService` and dispatches to the correct `ChannelService` (SMS/Telegram/WhatsApp/Push) |
| `scheduler` | `SendEmailScheduler` drives `MessageProcessor` on configurable fixed rates |
| `processor` | `MessageProcessor` — orchestrates the email send-and-persist lifecycle |
| `client` | `BackboneClient` — Feign client for auth/session validation against the Backbone service |
| `constant` | `ChannelType` enum (EMAIL, SMS, TELEGRAM, WHATSAPP, PUSH) and delivery status types |
| `config` | Spring `@Configuration` classes for Mail, FreeMarker, Database, Kafka |
| `security` | JWT session token generation (`SessionJwtServiceImpl`) |

### Persistence Model

- **PostgreSQL** (via JPA/Hibernate, DDL `none`): campaigns, campaign metrics, channel types, templates, message records, verification codes, users, applications.
- **MongoDB**: transient `EmailMessageDocument` records (own `messages` collection lifecycle: written on Kafka receipt, processed by `MessageProcessor`, deleted after being moved to PostgreSQL) plus a separate polymorphic `messages` collection (`MessageDocument` and its `SmsMessageDocument`/`TelegramMessageDocument`/`WhatsAppMessageDocument` subtypes) written by the `ChannelService` implementations — these are persisted but not yet reconciled/deleted by any scheduler.
- **Spring Cloud Config + HashiCorp Vault**: all credentials and environment-specific config come from Vault at startup via `bootstrap.yml`.

### Campaign Flow

1. `POST /api/v1/campaigns` → `CampaignServiceImpl.createCampaign()` (async)
2. Validates channel type (enabled), resolves template, persists `CampaignEntity` + initial `CampaignMetricsEntity`
3. Publishes one Kafka message per recipient to the channel topic (e.g., `email-topic`)
4. Kafka consumer picks up messages; `SendEmailScheduler` drives `MessageProcessor` to send and reconcile

## Key Conventions

- **Controller pattern**: every controller implements a `*Api` interface. OpenAPI (`@Operation`, `@ApiResponse`) annotations belong on the interface, not the controller class.
- **DTO location**: all transfer objects go in `api/v1/to` as Java records with Bean Validation annotations.
- **Scheduler rates**: use `${umdc.scheduler.*}` property placeholders, not hardcoded `fixedRate` values. Note the whole config namespace is `umdc.*` since the `com.prx` → `com.umdc` migration — `${prx.*}` placeholders no longer resolve.
- **Channel services**: new channel implementations must implement `send`, `updateStatus`, and `findByDeliveryStatus`.
- **Logging**: SLF4J `LoggerFactory` only — no other logging frameworks. Async methods use `CompletableFuture`.
- **MapStruct mappers**: place in `mapper/` package; processor is on the annotation processing path, so no manual `new` instantiation of mappers.
- **Kafka**: `UMDC_KAFKA_AUTO_STARTUP=false` by default (set in `bootstrap.yml`) so listeners don't crash local dev without a running broker. Set to `true` in deployed environments.

## External Dependencies

- **prx-commons**, **commons-services**, **security-oauth** — private PRX libraries hosted at `https://repo.repsy.io/mvn/lmata/prx`. Required for compilation; credentials must be in `~/.m2/settings.xml`.
- **Backbone service**: Feign client at `https://prx-qa.backbone.tst/backbone` for session token validation (`BackboneClient`).