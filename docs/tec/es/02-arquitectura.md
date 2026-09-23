# 🏛️ 02 · Arquitectura

🌐 [Read this in English](../en/02-architecture.md) · ⬅️ [Volver al índice](README.md)

> Mercury es un **microservicio de mensajería multicanal**: expone una API REST, encola mensajes por canal vía Kafka, entrega emails por SMTP con plantillas FreeMarker, y persiste datos en PostgreSQL (durable) y MongoDB (transitorio).

---

## 🧭 Vista de capas

```mermaid
flowchart TB
    subgraph L1["🌐 Presentación"]
        direction LR
        C1["*Controller"]
        C2["*Api interface<br/>(anotaciones OpenAPI)"]
    end

    subgraph L2["🧠 Servicio"]
        direction LR
        S1["*ServiceImpl<br/>(lógica de negocio)"]
        S2["CompletableFuture<br/>(operaciones async)"]
    end

    subgraph L3["🔄 Integración"]
        direction LR
        K1["KafkaTemplate<br/>(productor)"]
        K2["@KafkaListener<br/>(consumidores)"]
        K3["SMTP / FreeMarker"]
        K4["BackboneClient<br/>(Feign)"]
    end

    subgraph L4["🗄️ Persistencia"]
        direction LR
        P1["JPA Repositories<br/>(PostgreSQL)"]
        P2["NoSQL Repositories<br/>(MongoDB)"]
        P3["MapStruct Mappers"]
    end

    subgraph L5["☁️ Configuración externa"]
        direction LR
        E1["Spring Cloud Config<br/>(Git-backed)"]
        E2["HashiCorp Vault<br/>(KV v2, por entorno)"]
    end

    L1 --> L2 --> L3 --> L4
    L5 -.->|arranque: bootstrap.yml| L1
```

| Capa | Responsabilidad | Ejemplos reales |
|---|---|---|
| 🌐 Presentación | Endpoints delgados; las anotaciones OpenAPI viven en la interfaz `*Api`, no en el controlador | `CampaignController` implementa `CampaignApi` |
| 🧠 Servicio | Reglas de negocio; operaciones async devuelven `CompletableFuture` | `CampaignServiceImpl.createCampaign()` |
| 🔄 Integración | Publicación/consumo Kafka, SMTP, clientes HTTP externos | `MessageChannelRouter`, `EmailMessageConsumerServiceImpl` |
| 🗄️ Persistencia | Acceso a datos, mapeo Entity↔TO | `CampaignRepository`, `CampaignMapper` |
| ☁️ Configuración externa | Todo lo que llega desde Vault/Config Server antes de que arranque el contexto principal | Ver [06 · Configuración de Entornos](06-configuracion-entornos.md) |

---

## 🔀 Flujo de mensajes (el corazón de Mercury)

```mermaid
flowchart TD
    REST["POST /api/v1/campaigns"] --> CS["CampaignServiceImpl.createCampaign()<br/>(async)"]
    CS --> Persist["Persiste CampaignEntity +<br/>CampaignMetricsEntity inicial"]
    Persist --> Publish["Publica 1 mensaje Kafka<br/>por destinatario"]

    Publish --> TopicEmail(["topic: mercury-email-messages"])
    Publish --> TopicSms(["topic: mercury-sms-messages"])
    Publish --> TopicTelegram(["topic: mercury-telegram-messages"])
    Publish --> TopicWhatsapp(["topic: mercury-whatsapp-messages"])
    Publish --> TopicPush(["topic: mercury-push-messages"])

    TopicEmail --> EmailListener["MercuryEmailListener"]
    EmailListener --> EmailConsumer["EmailMessageConsumerServiceImpl"]
    EmailConsumer --> MongoEmail[("MongoDB<br/>EmailMessageDocument")]

    TopicSms & TopicTelegram & TopicWhatsapp & TopicPush --> MCListener["MultiChannelListener"]
    MCListener --> Router["MessageChannelRouter<br/>(resuelve plantilla vía TemplateDefinedService)"]
    Router --> ChannelSvc["ChannelService&lt;T&gt;<br/>Sms / Telegram / WhatsApp / Push"]
    ChannelSvc --> MongoMulti[("MongoDB<br/>messages (polimórfico)")]

    Scheduler["SendEmailScheduler<br/>(fixed-rate)"] --> Processor["MessageProcessor"]
    Processor -->|"1. lee OPENED, envía SMTP"| MongoEmail
    Processor -->|"2. lee SENT, escribe MessageRecord,<br/>crea VerificationCode si aplica,<br/>borra doc de Mongo"| Postgres[("PostgreSQL<br/>MessageRecord / VerificationCode")]
```

> [!IMPORTANT]
> Solo **Email** tiene un ciclo de vida completo (Kafka → Mongo → SMTP → PostgreSQL → borrado de Mongo). Los demás canales (SMS/Telegram/WhatsApp/Push) persisten el mensaje en la colección `messages` de Mongo — vía `ChannelService.send()` — pero ningún proceso hoy lo lleva a un estado terminal de entrega ni lo migra a PostgreSQL. Ver [04 · Componentes § Known Gaps](04-componentes.md#-known-gaps).

---

## 🧩 Los dos modelos de persistencia

Ver el detalle completo (todas las tablas, todos los documentos, y por qué están separados así) en [03 · Modelo de Datos](03-modelo-datos.md).

- **PostgreSQL** — durable, relacional: campañas, métricas de campaña, tipos de canal, plantillas, registros de mensaje, códigos de verificación, usuarios, aplicaciones.
- **MongoDB** — transitorio, documental: mensajes en vuelo. Dos familias de documentos independientes:
  - `EmailMessageDocument` — colección propia, ciclo de vida completo con `MessageProcessor`.
  - `MessageDocument` (base polimórfica) con subtipos `Sms/Telegram/WhatsApp/PushNotificationMessageDocument` — todos mapeados a la **misma** colección `messages`, escritos por los `ChannelService` pero sin reconciliación/purga todavía.

---

## 🔐 Seguridad de la API

```mermaid
sequenceDiagram
    participant Client
    participant Mercury
    participant Backbone as Backbone (Feign)
    Client->>Mercury: Request + Bearer token
    Mercury->>Backbone: Valida sesión (OAuth2)
    Backbone-->>Mercury: OK / Rechazado
    Mercury-->>Client: 200 / 401
```

- Autenticación de clientes vía OAuth2 (registro `mercury-backend-client`, `authorization-grant-type: password`) contra el proveedor `external` (Keycloak-style, `${AUTH_URI}`).
- `BackboneClient` (Feign) valida tokens de sesión contra el servicio Backbone.
- JWT propio (`SessionJwtServiceImpl`) para tokens de sesión de la aplicación — secreto y expiración configurables (`APP_TOKEN_SECRET`, `APP_TOKEN_EXPIRATION`).
- `spring.autoconfigure.exclude: ServletWebSecurityAutoConfiguration` — la seguridad HTTP estándar de Spring Boot está excluida; Mercury implementa su propio esquema.

---

## 🌱 Los dos diseños de mensajería que coexisten

Mercury tiene **dos** caminos de ingestión de mensajes en el código, en distinto grado de madurez:

1. **El camino en producción** (documentado arriba): un topic por canal, un `ChannelService` por canal, `MessageChannelRouter` como despachador.
2. **"Nexus" (experimental)**: `NotificationEventListener` consume un topic unificado (`${umdc.consumer.topics.notification}`) vía `NotificationEventConsumerServiceImpl` — pero **nada** en el flujo de creación de campañas publica ahí todavía, y sus manejadores por canal solo hacen `log`, sin persistencia ni envío real.

> [!NOTE]
> Tratar "Nexus" como experimental/no cableado hasta que se tome una decisión explícita de migrar el lado productor hacia ese diseño.

---

*Generado a partir de una lectura exhaustiva del código fuente real (`kafka/`, `api/v1/service/`, `bootstrap.yml`) — no de documentación previa ni de supuestos.*
