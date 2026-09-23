# 📦 04 · Catálogo de Componentes

🌐 [Read this in English](../en/04-components.md) · ⬅️ [Volver al índice](README.md)

---

## 🗺️ Mapa de paquetes

| Paquete | Propósito |
|---|---|
| `api/v1/controller` | Controladores delgados que implementan interfaces `*Api` (las anotaciones OpenAPI viven en la interfaz) |
| `api/v1/service` | Lógica de negocio; operaciones async devuelven `CompletableFuture` |
| `api/v1/to` | Records inmutables de Java: sufijos `*Request`, `*Response`, `*TO` |
| `api/v1/exception` | Excepciones de dominio + `GlobalExceptionHandler` |
| `jpa/sql/entity` + `jpa/sql/repository` | Entidades JPA y repositorios Spring Data (PostgreSQL) |
| `jpa/nosql/document` + `jpa/nosql/repository` | Documentos MongoDB (`MessageDocument` polimórfico para Sms/Telegram/WhatsApp/Push; `EmailMessageDocument` aparte) |
| `mapper` | Mappers MapStruct (Entity ↔ TO) |
| `kafka/listener` | Puntos de entrada `@KafkaListener` — `MultiChannelListener`, `MercuryEmailListener`, `NotificationEventListener` (experimental) |
| `kafka/consumer/service` | Implementaciones por canal (`ChannelService`); Email se maneja aparte, vía `EmailMessageConsumerService` |
| `kafka/router` | `MessageChannelRouter` — resuelve la plantilla y despacha al `ChannelService` correcto |
| `kafka/config` | `ConsumerConfig`, `ProducerConfig`, `KafkaSslProps` |
| `kafka/to` | DTOs de los mensajes Kafka |
| `scheduler` | `SendEmailScheduler` — dispara `MessageProcessor` a intervalo configurable |
| `processor` | `MessageProcessor` — orquesta el ciclo envío+persistencia del email |
| `client` | `BackboneClient` — cliente Feign para validación de sesión contra Backbone |
| `constant` | `ChannelType`, `DeliveryStatusType` |
| `config` | Configuración de Mail, FreeMarker, Base de datos |
| `security` | Generación de JWT de sesión (`SessionJwtServiceImpl`) |

---

## 🌐 API REST — endpoints reales

| Recurso | Operación | Método + ruta |
|---|---|---|
| Campañas | `createCampaign` | `POST /api/v1/campaigns` |
| | `getCampaignById` | `GET /api/v1/campaigns/{id}` |
| | `updateCampaign` | `PUT /api/v1/campaigns/{id}` |
| | `getCampaignsByApplication` | `GET /api/v1/campaigns/application/{applicationId}` |
| | `toggleCampaign` | `PATCH /api/v1/campaigns/{id}/toggle` |
| Tipos de canal | `getAllChannelTypes` | `GET /api/v1/channel-types` |
| | `getEnabledChannelTypes` | `GET /api/v1/channel-types/enabled` |
| | `createChannelType` | `POST /api/v1/channel-types` |
| | `getChannelTypeById` | `GET /api/v1/channel-types/{id}` |
| | `getChannelTypeByCode` | `GET /api/v1/channel-types/code/{code}` |
| | `updateChannelType` | `PUT /api/v1/channel-types/{id}` |
| | `toggleChannelType` | `PATCH /api/v1/channel-types/{id}/toggle` |
| Email | `sendEmail` | `POST /api/v1/mail` |
| Verificación | `sendVerificationCode` | `POST /api/v1/verification-code` |
| | `getLatestIsVerifiedStatus` | `GET /api/v1/verification-code/latest-status` |

📖 Contrato completo: `src/main/resources/api/openapi.yaml` — servido en `/v3/api-docs` y Swagger UI en runtime.

---

## 📡 Canales de mensajería — estado real por canal

| Canal | `ChannelService` | Entrega real | Notas |
|---|---|---|---|
| 📧 Email | *(fuera de `ChannelService`)* `EmailMessageConsumerServiceImpl` → `MessageProcessor` | ✅ SMTP real | Único canal con ciclo de vida completo (ver [02 · Arquitectura](02-arquitectura.md)) |
| 💬 SMS | `SmsChannelService` | ❌ Solo persiste en Mongo | Sin proveedor (Twilio, etc.) integrado |
| ✈️ Telegram | `TelegramChannelService` | ❌ Solo persiste en Mongo | Dependencias `telegrambots-*` presentes en `pom.xml` pero sin uso en esta rama |
| 🟢 WhatsApp | `WhatsAppChannelService` | ❌ Solo persiste en Mongo | Sin integración con WhatsApp Cloud API |
| 🔔 Push | `PushChannelService` | ❌ Solo persiste en Mongo | Sin integración FCM/APNs |

Contrato común (`ChannelService<T extends MessageDocument>`):

```java
T send(T message, TemplateDefinedTO template);
void updateStatus(T message);
List<T> findByDeliveryStatus(DeliveryStatusType status);
```

> [!NOTE]
> Cualquier canal nuevo debe implementar estos tres métodos. `MessageChannelRouter` resuelve la plantilla vía `TemplateDefinedService` antes de despachar — la lógica de "qué canal es" nunca vive en el propio `ChannelService`.

---

## 🏭 Flujo de campañas — paso a paso

1. `POST /api/v1/campaigns` → `CampaignServiceImpl.createCampaign()` (async)
2. Valida el tipo de canal (habilitado), resuelve la plantilla, persiste `CampaignEntity` + `CampaignMetricsEntity` inicial
3. Publica un mensaje Kafka por destinatario al topic del canal (p. ej. `mercury-email-messages`)
4. El consumidor Kafka correspondiente recoge los mensajes; para email, `SendEmailScheduler` dispara `MessageProcessor` para enviar y reconciliar

---

## ⚠️ Known Gaps

> [!IMPORTANT]
> Estos son los huecos funcionales reales a la fecha de esta documentación — no aspiraciones, hechos verificados contra el código.

- **Ningún canal fuera de Email llega a un proveedor real.** SMS/Telegram/WhatsApp/Push persisten el mensaje y quedan ahí — listos para cuando se conecte Twilio/Telegram Bot API/WhatsApp Cloud API/FCM-APNs.
- **"Nexus" (`NotificationEventListener`) es experimental.** Consume un topic unificado pero nada lo publica todavía; sus manejadores por canal solo hacen `log`.
- **Los mensajes no-email no se migran a PostgreSQL ni se purgan de Mongo.** `ChannelService.updateStatus()` existe pero no tiene ningún llamador real en el flujo — no hay reconciliación de estado terminal para SMS/Telegram/WhatsApp/Push.
- **`EmailMessageDocument`/`EmailMessageTO` no llevan `campaignId`.** El vínculo campaña↔email en tránsito no existe hoy; solo se reconstruye al llegar a `MessageRecord` en PostgreSQL.
- **`ChannelConfigEntity` está definido pero no cableado** a ningún flujo de lectura/escritura activo.
- **Sin reconciliación de entregas** (`MessageDeliveryLogEntity` existe en el esquema pero nada la escribe desde el código explorado).

---

*Generado a partir de una lectura exhaustiva de `api/`, `kafka/`, `scheduler/` y `processor/` — no de documentación previa ni de supuestos.*
