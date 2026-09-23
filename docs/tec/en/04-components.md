# 📦 04 · Component Catalog

🌐 [Leer esto en Español](../es/04-componentes.md) · ⬅️ [Back to index](README.md)

---

## 🗺️ Package map

| Package | Purpose |
|---|---|
| `api/v1/controller` | Thin controllers implementing `*Api` interfaces (OpenAPI annotations live on the interface) |
| `api/v1/service` | Business logic; async operations return `CompletableFuture` |
| `api/v1/to` | Immutable Java records: `*Request`, `*Response`, `*TO` suffixes |
| `api/v1/exception` | Domain exceptions + `GlobalExceptionHandler` |
| `jpa/sql/entity` + `jpa/sql/repository` | JPA entities and Spring Data repositories (PostgreSQL) |
| `jpa/nosql/document` + `jpa/nosql/repository` | MongoDB documents (polymorphic `MessageDocument` for Sms/Telegram/WhatsApp/Push; `EmailMessageDocument` separate) |
| `mapper` | MapStruct mappers (Entity ↔ TO) |
| `kafka/listener` | `@KafkaListener` entry points — `MultiChannelListener`, `MercuryEmailListener`, `NotificationEventListener` (experimental) |
| `kafka/consumer/service` | Per-channel implementations (`ChannelService`); Email is handled separately, via `EmailMessageConsumerService` |
| `kafka/router` | `MessageChannelRouter` — resolves the template and dispatches to the right `ChannelService` |
| `kafka/config` | `ConsumerConfig`, `ProducerConfig`, `KafkaSslProps` |
| `kafka/to` | Kafka message DTOs |
| `scheduler` | `SendEmailScheduler` — triggers `MessageProcessor` at a configurable interval |
| `processor` | `MessageProcessor` — orchestrates the email send-and-persist cycle |
| `client` | `BackboneClient` — Feign client for session validation against Backbone |
| `constant` | `ChannelType`, `DeliveryStatusType` |
| `config` | Mail, FreeMarker, and Database configuration |
| `security` | Session JWT generation (`SessionJwtServiceImpl`) |

---

## 🌐 REST API — real endpoints

| Resource | Operation | Method + path |
|---|---|---|
| Campaigns | `createCampaign` | `POST /api/v1/campaigns` |
| | `getCampaignById` | `GET /api/v1/campaigns/{id}` |
| | `updateCampaign` | `PUT /api/v1/campaigns/{id}` |
| | `getCampaignsByApplication` | `GET /api/v1/campaigns/application/{applicationId}` |
| | `toggleCampaign` | `PATCH /api/v1/campaigns/{id}/toggle` |
| Channel types | `getAllChannelTypes` | `GET /api/v1/channel-types` |
| | `getEnabledChannelTypes` | `GET /api/v1/channel-types/enabled` |
| | `createChannelType` | `POST /api/v1/channel-types` |
| | `getChannelTypeById` | `GET /api/v1/channel-types/{id}` |
| | `getChannelTypeByCode` | `GET /api/v1/channel-types/code/{code}` |
| | `updateChannelType` | `PUT /api/v1/channel-types/{id}` |
| | `toggleChannelType` | `PATCH /api/v1/channel-types/{id}/toggle` |
| Email | `sendEmail` | `POST /api/v1/mail` |
| Verification | `sendVerificationCode` | `POST /api/v1/verification-code` |
| | `getLatestIsVerifiedStatus` | `GET /api/v1/verification-code/latest-status` |

📖 Full contract: `src/main/resources/api/openapi.yaml` — served at `/v3/api-docs` and Swagger UI at runtime.

---

## 📡 Messaging channels — real status per channel

| Channel | `ChannelService` | Real delivery | Notes |
|---|---|---|---|
| 📧 Email | *(outside `ChannelService`)* `EmailMessageConsumerServiceImpl` → `MessageProcessor` | ✅ Real SMTP | The only channel with a complete lifecycle (see [02 · Architecture](02-architecture.md)) |
| 💬 SMS | `SmsChannelService` | ❌ Persists to Mongo only | No provider (Twilio, etc.) integrated |
| ✈️ Telegram | `TelegramChannelService` | ❌ Persists to Mongo only | `telegrambots-*` dependencies present in `pom.xml` but unused on this branch |
| 🟢 WhatsApp | `WhatsAppChannelService` | ❌ Persists to Mongo only | No WhatsApp Cloud API integration |
| 🔔 Push | `PushChannelService` | ❌ Persists to Mongo only | No FCM/APNs integration |

Common contract (`ChannelService<T extends MessageDocument>`):

```java
T send(T message, TemplateDefinedTO template);
void updateStatus(T message);
List<T> findByDeliveryStatus(DeliveryStatusType status);
```

> [!NOTE]
> Any new channel must implement these three methods. `MessageChannelRouter` resolves the template via `TemplateDefinedService` before dispatching — the "which channel is this" logic never lives inside the `ChannelService` itself.

---

## 🏭 Campaign flow — step by step

1. `POST /api/v1/campaigns` → `CampaignServiceImpl.createCampaign()` (async)
2. Validates the channel type (enabled), resolves the template, persists `CampaignEntity` + an initial `CampaignMetricsEntity`
3. Publishes one Kafka message per recipient to the channel's topic (e.g. `mercury-email-messages`)
4. The matching Kafka consumer picks up the messages; for email, `SendEmailScheduler` drives `MessageProcessor` to send and reconcile

---

## ⚠️ Known Gaps

> [!IMPORTANT]
> These are the real functional gaps as of this documentation's writing — not aspirations, facts verified against the code.

- **No channel besides Email reaches a real provider.** SMS/Telegram/WhatsApp/Push persist the message and stop there — ready for whenever Twilio/Telegram Bot API/WhatsApp Cloud API/FCM-APNs gets wired in.
- **"Nexus" (`NotificationEventListener`) is experimental.** It consumes a unified topic, but nothing publishes to it yet; its per-channel handlers only `log`.
- **Non-email messages aren't migrated to PostgreSQL nor purged from Mongo.** `ChannelService.updateStatus()` exists but has no real caller anywhere in the flow — there's no terminal-status reconciliation for SMS/Telegram/WhatsApp/Push.
- **`EmailMessageDocument`/`EmailMessageTO` carry no `campaignId`.** The campaign↔email link doesn't exist while the message is in flight today; it's only reconstructed once it reaches `MessageRecord` in PostgreSQL.
- **`ChannelConfigEntity` is defined but not wired** into any active read/write flow.
- **No delivery reconciliation** (`MessageDeliveryLogEntity` exists in the schema but nothing writes to it in the explored code).

---

*Generated from an exhaustive read of `api/`, `kafka/`, `scheduler/` and `processor/` — not from prior documentation or assumptions.*
