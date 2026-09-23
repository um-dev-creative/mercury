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
    L5 -.->|startup: bootstrap.yml| L1
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
    Publish --> TopicSms(["topic: mercury-sms-messages"])
    Publish --> TopicTelegram(["topic: mercury-telegram-messages"])
    Publish --> TopicWhatsapp(["topic: mercury-whatsapp-messages"])
    Publish --> TopicPush(["topic: mercury-push-messages"])

    TopicEmail --> EmailListener["MercuryEmailListener"]
    EmailListener --> EmailConsumer["EmailMessageConsumerServiceImpl"]
    EmailConsumer --> MongoEmail[("MongoDB<br/>EmailMessageDocument")]

    TopicSms & TopicTelegram & TopicWhatsapp & TopicPush --> MCListener["MultiChannelListener"]
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
- Custom JWT (`SessionJwtServiceImpl`) for application session tokens — configurable secret and expiration (`APP_TOKEN_SECRET`, `APP_TOKEN_EXPIRATION`).
- `spring.autoconfigure.exclude: ServletWebSecurityAutoConfiguration` — Spring Boot's standard HTTP security is excluded; Mercury implements its own scheme.

---

## 🌱 The two messaging designs that coexist

Mercury has **two** message-ingestion paths in the codebase, at different levels of maturity:

1. **The production path** (documented above): one topic per channel, one `ChannelService` per channel, `MessageChannelRouter` as dispatcher.
2. **"Nexus" (experimental)**: `NotificationEventListener` consumes a unified topic (`${umdc.consumer.topics.notification}`) via `NotificationEventConsumerServiceImpl` — but **nothing** in the campaign-creation flow publishes to it yet, and its per-channel handlers only `log`, with no persistence or real sending.

> [!NOTE]
> Treat "Nexus" as experimental/unwired until an explicit decision is made to migrate the producer side to that design.

---

*Generated from an exhaustive read of the real source code (`kafka/`, `api/v1/service/`, `bootstrap.yml`) — not from prior documentation or assumptions.*
