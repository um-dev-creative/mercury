# 🗄️ 03 · Data Model

🌐 [Leer esto en Español](../es/03-modelo-datos.md) · ⬅️ [Back to index](README.md)

> Mercury persists across **two** engines with distinct purposes: PostgreSQL for everything durable/relational, MongoDB for transient in-flight messages. `hibernate.ddl-auto: none` — the PostgreSQL schema is managed outside the application; Mercury never migrates it on its own.

---

## 🐘 PostgreSQL — 14 tables, 2 schemas

```mermaid
erDiagram
    APPLICATION ||--o{ TEMPLATE : owns
    APPLICATION ||--o{ TEMPLATE_DEFINED : owns
    APPLICATION ||--o{ VERIFICATION_CODES : owns
    USER ||--o{ VERIFICATION_CODES : requests

    CHANNEL_TYPE ||--o{ CAMPAIGNS : "used by"
    CHANNEL_TYPE ||--o{ CHANNEL_CONFIG : "configured by"
    CHANNEL_TYPE ||--o{ MESSAGE_DELIVERY_LOG : "delivered via"

    TEMPLATE_TYPE ||--o{ TEMPLATE : classifies
    SEVERITY_TYPE ||--o{ TEMPLATE : classifies
    TEMPLATE ||--o{ TEMPLATE_DEFINED : "is base of"
    FREQUENCY_TYPE ||--o{ TEMPLATE_DEFINED : limits

    TEMPLATE_DEFINED ||--o{ CAMPAIGNS : "is content of"
    TEMPLATE_DEFINED ||--o{ MESSAGE_RECORD : "is content of"

    CAMPAIGNS ||--o{ CAMPAIGN_METRICS : measures
    CAMPAIGNS ||--o{ MESSAGE_DELIVERY_LOG : generates

    MESSAGE_STATUS_TYPE ||--o{ MESSAGE_RECORD : classifies

    CAMPAIGNS {
        uuid id PK
        string name
        uuid channel_type_id FK
        uuid template_defined_id FK
        datetime scheduled_at
        string status
        int total_recipients
        int batch_size
        uuid created_by
    }
    CAMPAIGN_METRICS {
        uuid id PK
        uuid campaign_id FK
        int total_sent
        int delivered
        int failed
        int opened
        int clicked
        int bounced
        int unsubscribed
    }
    CHANNEL_TYPE {
        uuid id PK
        string code
        string name
        string implementation_class
        bool enabled
        bool active
    }
    CHANNEL_CONFIG {
        uuid id PK
        uuid channel_type_id FK
        uuid application_id
    }
    MESSAGE_DELIVERY_LOG {
        uuid id PK
        string message_id
        uuid campaign_id FK
        uuid channel_type_id FK
        string recipient
        string status
        string provider_message_id
        instant sent_at
        instant delivered_at
        instant opened_at
        instant clicked_at
    }
    MESSAGE_RECORD {
        uuid id PK
        string sender
        string to
        string cc
        string subject
        string content
        uuid message_status_type_id FK
        uuid template_defined_id FK
    }
    MESSAGE_STATUS_TYPE {
        uuid id PK
        string name
        bool active
    }
    TEMPLATE {
        uuid id PK
        string description
        string location
        string file_format
        uuid template_type_id FK
        uuid application_id FK
        uuid severity_type_id FK
        bool active
    }
    TEMPLATE_DEFINED {
        uuid id PK
        uuid template_id FK
        uuid user_id
        uuid application_id FK
        uuid frequency_type FK
        datetime expired_at
        bool is_active
    }
    TEMPLATE_TYPE {
        uuid id PK
        string name
        bool active
    }
    FREQUENCY_TYPE {
        uuid id PK
        string name
        bool active
    }
    SEVERITY_TYPE {
        uuid id PK
        string name
        bool active
    }
    VERIFICATION_CODES {
        uuid id PK
        uuid user_id
        uuid application_id FK
        string verification_code
        datetime expires_at
        datetime verified_at
        bool is_verified
        int attempts
        int max_attempts
    }
    APPLICATION {
        uuid id PK
    }
    USER {
        uuid id PK
    }
```

| Schema | Tables | Purpose |
|---|---|---|
| `mercury` | `campaigns`, `campaign_metrics`, `channel_type`, `channel_config`, `message_delivery_log`, `message_record`, `message_status_type`, `templates`, `template_defined`, `template_types`, `frequency_type`, `severity_type`, `verification_codes` | Everything specific to the messaging domain |
| `general` | `application`, `user` | Entities shared with other services in the PRX/UMDC platform — Mercury only **references** them, it doesn't own them |

> [!TIP]
> `CampaignEntity` and `MessageRecordEntity` point at `TemplateDefinedEntity` — not `TemplateEntity` directly. `TemplateDefinedEntity` is the "instance" of a template for a specific application (with its own frequency and expiration); `TemplateEntity` is the reusable generic template.

---

## 🍃 MongoDB — two document families

```mermaid
classDiagram
    class MessageDocument {
        <<abstract>>
        +String id
        +UUID campaignId
        +DeliveryStatusType deliveryStatus
        +String message
        +Map~String,Object~ params
    }
    class SmsMessageDocument {
        +String phoneNumber
    }
    class TelegramMessageDocument {
        +Long chatId
    }
    class WhatsAppMessageDocument {
        +String phoneNumber
        +String content
    }
    class PushNotificationMessageDocument {
        +String deviceToken
        +String platform
    }
    class EmailMessageDocument {
        +String to
        +String subject
        +String body
        +DeliveryStatusType deliveryStatus
    }

    MessageDocument <|-- SmsMessageDocument
    MessageDocument <|-- TelegramMessageDocument
    MessageDocument <|-- WhatsAppMessageDocument
    MessageDocument <|-- PushNotificationMessageDocument
```

| Collection | Document(s) | Repository | Lifecycle |
|---|---|---|---|
| `messages` | `MessageDocument` (polymorphic base) + `Sms/Telegram/WhatsApp/PushNotificationMessageDocument` | `MessageNSRepository` | Written by `ChannelService.send()` when consuming from the matching topic. **No reconciliation or purge yet** — see [04 · Components](04-components.md#-known-gaps). |
| own (email) | `EmailMessageDocument` | `EmailMessageNSRepository` | Written when consuming `mercury-email-messages`; `MessageProcessor` reads it (`OPENED`), sends it over SMTP, and **deletes it** after persisting the `MessageRecord` to PostgreSQL. The only document with a complete lifecycle. |

`EmailMessageDocument` is deliberately **independent** from the `MessageDocument` hierarchy — it shares neither base type nor collection with the other channels.

### Delivery states (`DeliveryStatusType`)

```
OPENED → PENDING → IN_PROGRESS → SENT → DELIVERED
                                      ↘ REJECTED / CANCELED / ABORTED / FAILED
```

---

## 🔑 Conventions

- **Every primary key is a `UUID`** — generated by the application or the database, never `SERIAL`/autoincrement.
- Timestamps: a mix of `LocalDateTime` (most of `mercury`) and `Instant` (`MessageDeliveryLogEntity`, `FrequencyTypeEntity`, `SeverityTypeEntity`) — not yet unified.
- `active`/`enabled` as `Boolean` (not the primitive `boolean`) on catalog tables (`channel_type`, `template_types`, `frequency_type`, `severity_type`, `message_status_type`) — allows `NULL` to mean "undefined" instead of forcing `false`.

---

*Generated from an exhaustive read of `jpa/sql/entity/` and `jpa/nosql/document/` — not from prior documentation or assumptions.*
