# 🏛️ 02 · Architecture

🌐 [Leer esto en Español](../es/02-arquitectura.md) · ⬅️ [Back to index](README.md)

> Mercury is a **multi-channel messaging microservice**: it exposes a REST API, queues per-channel messages via Kafka, delivers emails over SMTP with FreeMarker templates, and persists data across PostgreSQL (durable) and MongoDB (transient).

---

## 🧭 Layer view

```mermaid
flowchart TB
    subgraph L1["🌐 Presentation"]
        direction LR
        C1["*Controller"]
        C2["*Api interface<br/>(OpenAPI annotations)"]
    end

    subgraph L2["🧠 Service"]
        direction LR
        S1["*ServiceImpl<br/>(business logic)"]
        S2["CompletableFuture<br/>(async operations)"]
    end

    subgraph L3["🔄 Integration"]
        direction LR
        K1["KafkaTemplate<br/>(producer)"]
        K2["@KafkaListener<br/>(consumers)"]
        K3["SMTP / FreeMarker"]
        K4["BackboneClient<br/>(Feign)"]
    end

    subgraph L4["🗄️ Persistence"]
        direction LR
        P1["JPA Repositories<br/>(PostgreSQL)"]
        P2["NoSQL Repositories<br/>(MongoDB)"]
        P3["MapStruct Mappers"]
    end

    subgraph L5["☁️ External config"]
        direction LR
        E1["Spring Cloud Config<br/>(Git-backed)"]
        E2["HashiCorp Vault<br/>(KV v2, per environment)"]
    end

    L1 --> L2 --> L3 --> L4
    L5 -.->|startup: application.yml| L1
```

| Layer | Responsibility | Real examples |
|---|---|---|
| 🌐 Presentation | Thin endpoints; OpenAPI annotations live on the `*Api` interface, not the controller | `CampaignController` implements `CampaignApi` |
| 🧠 Service | Business rules; async operations return `CompletableFuture` | `CampaignServiceImpl.createCampaign()` |
| 🔄 Integration | Kafka publish/consume, SMTP, external HTTP clients | `MessageChannelRouter`, `EmailMessageConsumerServiceImpl` |
| 🗄️ Persistence | Data access, Entity↔TO mapping | `CampaignRepository`, `CampaignMapper` |
| ☁️ External config | Everything arriving from Vault/Config Server before the main context starts | See [06 · Environment Configuration](06-environment-configuration.md) |

---

## 🔀 Message flow (the heart of Mercury)

```mermaid
flowchart TD
    REST["POST /api/v1/campaigns"] --> CS["CampaignServiceImpl.createCampaign()<br/>(async)"]
    CS --> Persist["Persists CampaignEntity +<br/>initial CampaignMetricsEntity"]
    Persist --> Publish["Publishes 1 Kafka message<br/>per recipient"]

    Publish --> TopicEmail(["topic: mercury-email-messages"])
    Publish --> TopicUnified(["topic: mercury-unified-message"])

    TopicEmail --> EmailListener["MercuryEmailListener"]
    EmailListener --> EmailConsumer["EmailMessageConsumerServiceImpl"]
    EmailConsumer --> MongoEmail[("MongoDB<br/>EmailMessageDocument")]

    TopicUnified --> MCListener["NotificationEventListener<br/>(routing.channel)"]
    MCListener --> Router["MessageChannelRouter<br/>(resolves the template via TemplateDefinedService)"]
    Router --> ChannelSvc["ChannelService&lt;T&gt;<br/>Sms / Telegram / WhatsApp / Push"]
    ChannelSvc --> MongoMulti[("MongoDB<br/>messages (polymorphic)")]

    Scheduler["SendEmailScheduler<br/>(fixed-rate)"] --> Processor["MessageProcessor"]
    Processor -->|"1. reads OPENED, sends via SMTP"| MongoEmail
    Processor -->|"2. reads SENT, writes MessageRecord,<br/>creates VerificationCode if applicable,<br/>deletes the Mongo doc"| Postgres[("PostgreSQL<br/>MessageRecord / VerificationCode")]
```

> [!IMPORTANT]
> Only **Email** has a complete lifecycle (Kafka → Mongo → SMTP → PostgreSQL → Mongo deletion). The other channels (SMS/Telegram/WhatsApp/Push) persist the message into Mongo's `messages` collection — via `ChannelService.send()` — but nothing today drives it to a terminal delivery status or migrates it to PostgreSQL. See [04 · Components § Known Gaps](04-components.md#-known-gaps).

---

## 🧩 The two persistence models

See the full detail (every table, every document, and why they're split this way) in [03 · Data Model](03-data-model.md).

- **PostgreSQL** — durable, relational: campaigns, campaign metrics, channel types, templates, message records, verification codes, users, applications.
- **MongoDB** — transient, document-based: in-flight messages. Two independent document families:
  - `EmailMessageDocument` — its own collection, full lifecycle with `MessageProcessor`.
  - `MessageDocument` (polymorphic base) with subtypes `Sms/Telegram/WhatsApp/PushNotificationMessageDocument` — all mapped to the **same** `messages` collection, written by the `ChannelService` implementations but not yet reconciled/purged.

---

## 🔐 API security

```mermaid
sequenceDiagram
    participant Client
    participant Mercury
    participant Backbone as Backbone (Feign)
    Client->>Mercury: Request + Bearer token
    Mercury->>Backbone: Validates session (OAuth2)
    Backbone-->>Mercury: OK / Rejected
    Mercury-->>Client: 200 / 401
```

- Client authentication over OAuth2 (`mercury-backend-client` registration, `authorization-grant-type: password`) against the `external` provider (Keycloak-style, `${AUTH_URI}`).
- `BackboneClient` (Feign) validates session tokens against the Backbone service.
- Custom JWT (`SessionJwtServiceImpl`) for session tokens — the secret is required and shared with backbone-rest (`APP_TOKEN_SECRET`, no default); `iss`/`aud`/`type`, a local `jti` denylist and backbone-rest's own deny-list are all enforced. Client applications use a Backbone opaque Bearer. Full detail: [Authentication & token configuration](06-environment-configuration.md#-authentication--token-configuration-essential) and `docs/architecture/session-token-authorization.md`.
- `spring.autoconfigure.exclude: ServletWebSecurityAutoConfiguration` — Spring Boot's standard HTTP security is excluded; Mercury implements its own scheme.

---

## 🌱 One topic for four channels

SMS, Telegram, WhatsApp and Push share `mercury-unified-message` because the Aiven plan limits the number of topics; email keeps `mercury-email-messages`. Campaigns publish a `NotificationEventTO` (schema: [`docs/topic-general-message.md`](../../topic-general-message.md)), `NotificationEventListener` consumes it, and `NotificationEventConsumerServiceImpl` splits by `routing.channel`, validates `target_identifier` for that channel and builds the channel's document.

- `payload.context` carries the template parameters and `payload.delivery_instructions` the channel options (`sender_id`, `parse_mode`, `message_type`, `platform`, `title`, `icon`, `click_action`, `priority`, `badge`, `sound`).
- `header.messageId` is the Mongo document id, so a Kafka redelivery does not create a second message.
- Undeliverable records are logged and discarded. A processing failure is retried 4 times, 2 seconds apart, then logged and skipped.
- `EMAIL` on the unified topic is logged and discarded: the schema has no sender, subject or copy recipients.

---

*Generated from an exhaustive read of the real source code (`kafka/`, `api/v1/service/`, `application.yml`) — not from prior documentation or assumptions.*
