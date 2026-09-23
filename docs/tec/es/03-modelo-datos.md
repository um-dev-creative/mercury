# 🗄️ 03 · Modelo de Datos

🌐 [Read this in English](../en/03-data-model.md) · ⬅️ [Volver al índice](README.md)

> Mercury persiste en **dos** motores con propósitos distintos: PostgreSQL para todo lo durable/relacional, MongoDB para mensajes transitorios en vuelo. `hibernate.ddl-auto: none` — el esquema de PostgreSQL se gestiona fuera de la aplicación; Mercury nunca lo migra por su cuenta.

---

## 🐘 PostgreSQL — 14 tablas, 2 esquemas

```mermaid
erDiagram
    APPLICATION ||--o{ TEMPLATE : "posee"
    APPLICATION ||--o{ TEMPLATE_DEFINED : "posee"
    APPLICATION ||--o{ VERIFICATION_CODES : "posee"
    USER ||--o{ VERIFICATION_CODES : "solicita"

    CHANNEL_TYPE ||--o{ CAMPAIGNS : "usado por"
    CHANNEL_TYPE ||--o{ CHANNEL_CONFIG : "configurado por"
    CHANNEL_TYPE ||--o{ MESSAGE_DELIVERY_LOG : "entregado vía"

    TEMPLATE_TYPE ||--o{ TEMPLATE : clasifica
    SEVERITY_TYPE ||--o{ TEMPLATE : clasifica
    TEMPLATE ||--o{ TEMPLATE_DEFINED : "basa a"
    FREQUENCY_TYPE ||--o{ TEMPLATE_DEFINED : limita

    TEMPLATE_DEFINED ||--o{ CAMPAIGNS : "es contenido de"
    TEMPLATE_DEFINED ||--o{ MESSAGE_RECORD : "es contenido de"

    CAMPAIGNS ||--o{ CAMPAIGN_METRICS : mide
    CAMPAIGNS ||--o{ MESSAGE_DELIVERY_LOG : genera

    MESSAGE_STATUS_TYPE ||--o{ MESSAGE_RECORD : clasifica

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

| Esquema | Tablas | Propósito |
|---|---|---|
| `mercury` | `campaigns`, `campaign_metrics`, `channel_type`, `channel_config`, `message_delivery_log`, `message_record`, `message_status_type`, `templates`, `template_defined`, `template_types`, `frequency_type`, `severity_type`, `verification_codes` | Todo lo específico del dominio de mensajería |
| `general` | `application`, `user` | Entidades compartidas con otros servicios de la plataforma PRX/UMDC — Mercury solo las **referencia**, no las administra |

> [!TIP]
> `CampaignEntity` y `MessageRecordEntity` apuntan a `TemplateDefinedEntity` — no a `TemplateEntity` directamente. `TemplateDefinedEntity` es la "instancia" de una plantilla para una aplicación concreta (con su propia frecuencia y expiración); `TemplateEntity` es la plantilla genérica reutilizable.

---

## 🍃 MongoDB — dos familias de documentos

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

| Colección | Documento(s) | Repositorio | Ciclo de vida |
|---|---|---|---|
| `messages` | `MessageDocument` (base polimórfica) + `Sms/Telegram/WhatsApp/PushNotificationMessageDocument` | `MessageNSRepository` | Escrito por `ChannelService.send()` al consumir del topic correspondiente. **Sin reconciliación ni purga todavía** — ver [04 · Componentes](04-componentes.md#-known-gaps). |
| propia (email) | `EmailMessageDocument` | `EmailMessageNSRepository` | Escrito al consumir `mercury-email-messages`; `MessageProcessor` lo lee (`OPENED`), lo envía por SMTP, y **lo borra** tras persistir el `MessageRecord` en PostgreSQL. Único documento con ciclo de vida completo. |

`EmailMessageDocument` es deliberadamente **independiente** de la jerarquía `MessageDocument` — no comparte tipo base ni colección con los demás canales.

### Estados de entrega (`DeliveryStatusType`)

```
OPENED → PENDING → IN_PROGRESS → SENT → DELIVERED
                                      ↘ REJECTED / CANCELED / ABORTED / FAILED
```

---

## 🔑 Convenciones

- **Todas las claves primarias son `UUID`** — generadas por la aplicación o la base, nunca `SERIAL`/autoincrement.
- Timestamps: mezcla de `LocalDateTime` (la mayoría de `mercury`) e `Instant` (`MessageDeliveryLogEntity`, `FrequencyTypeEntity`, `SeverityTypeEntity`) — no homogeneizado todavía.
- `active`/`enabled` como `Boolean` (no `boolean` primitivo) en las tablas de catálogo (`channel_type`, `template_types`, `frequency_type`, `severity_type`, `message_status_type`) — permite `NULL` como "no definido" en lugar de forzar `false`.

---

*Generado a partir de una lectura exhaustiva de `jpa/sql/entity/` y `jpa/nosql/document/` — no de documentación previa ni de supuestos.*
