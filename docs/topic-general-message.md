Since we need a single, unified structure capable of handling **WhatsApp, Telegram, Email, Push notifications, and SMS**, we must move away from "Type-specific" flat structures to a **Polymorphic Messaging Schema**.

The strategy here is to separate the **Context** (Campaign info, Timing) from the **Payload** (Content specific to the delivery channel). By using a generic `payload` or `meta` block for specifics like Telegram's "Amount" or Push "Badge", we ensure the core message pipeline remains clean while being flexible enough for any future channel.

---

# 🚀 Unified Messaging Schema: Mercury Nexus
> **Version:** 1.0.0 | **Date:** 2023-10-27  
> *Unified Kafka Message structure for multi-channel communication.*

The goal of the **Nexus Schema** is to eliminate redundant logic in downstream consumers by standardizing how a message is identified, routed, and rendered regardless of whether it's a WhatsApp notification, an SMS alert, or a Push notification.

## 🏗 The Unified Structure (JSON Representation)
This is the canonical structure used for all topics related to outbound messaging.

```json
{
  "header": {
    "campaignId": "92a1b7c4-5678-4321-0987-1234567890ab",
    "messageId": "f3d2e1f0-4321-0987-1234-567890123456",
    "timestamp": "2023-11-15T10:30:00Z",
    "traceId": "a1b2c3d4e5f6g7h8"
  },
  "routing": {
    "channel": "WHATSAPP | TELEGRAM | EMAIL | PUSH | SMS",
    "recipient_id": "f47ac10b-58cc-4373-a567-0e02b2c3d479",
    "target_identifier": "+34600112233" 
  },
  "content": {
    "templateId": "550e8400-e29b-41d4-a716-446655440000",
    "templateName": "welcome_confirmation_v1",
    "language": "es",
    "body": "Hola, gracias por tu compra. Tu pedido está en camino.",
    "mediaUrl": "https://cdn.ejemplo.com/img.jpg",
    "mediatype": "IMAGE | VIDEO | NONE"
  },
  "payload": {
    "context": {}, 
    "delivery_instructions": {}
  }
}
```

---

## 📋 Field Documentation

### 1. Header (Metadata)
*Core data used for analytics, tracking, and debugging.*

| Field | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `campaignId` | UUID/String | Unique identifier for the marketing or service campaign. | `"a9e1b7c4-..."` |
| `messageId` | UUID/String | Unique ID for this specific message instance (for idempotency). | `"f3d2e1f0-..."` |
| `timestamp` | ISO-8601 | The exact time the request was received. | `"2023-11-15T10:30:00Z"` |
| `traceId` | String | Distributed tracing ID (e.g., Jaeger/Zipkin). | `"a1b2c3d4..."` |

### 2. Routing (Delivery Context)
*Information needed to decide which gateway or provider should handle the message.*

| Field | Type | Description | Notes |
| :--- | :--- | :--- | :--- |
| `channel` | Enum | The target medium: `WHATSAPP`, `TELEGRAM`, `EMAIL`, `PUSH`, `SMS`. | **Required** |
| `recipient_id` | UUID/String | Internal unique identifier for the user/customer. | |
| `target_identifier`| String | The "Address": Phone number, Email address, or Device Token. | |

### 3. Content (The "What")
*The actual message content and visual assets.*

| Field | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `templateId` | UUID/String | ID of the pre-defined template in the CRM/Engine. | `"550e8...4000"` |
| `templateName`| String | Human-readable name of the template used. | `"welcome_v1"` |
| `language` | String | ISO 639-1 language code. | `"en"`, `"es"` |
| `body` | Text | The final rendered text content (fallback/plain). | `"Your order is ready!"` |
| `mediaUrl` | URL | Link to a hosted image, video, or document. | `https://...` |
| `mediatype` | Enum | Type of media provided: `IMAGE`, `VIDEO`, `NONE`. | `"IMAGE"` |

### 4. Payload (Dynamic/Extended Data)
*This is the "flexible bucket" for data specific to certain channels.*

| Field | Type | Description | Logic |
| :--- | :--- | :--- | :--- |
| `context` | Object | Key-Value pairs used by the message engine to populate templates. | Includes: `name`, `order_id`, `amount`, etc. |
| `delivery_instructions`| Object | Options for specific providers (e.g., notification sounds, skip_retry). | For Push or WhatsApp specifics. |

---

## 🗺 Mapping Table (Legacy $\rightarrow$ New)
*How the previous schemas map to the new Unified logic.*

| Old Source Schema | `medium` | Key Maps To ... | Logic Note |
| :--- | :--- | :--- | :--- |
| **WhatsApp** | `WHATSAPP` | `templateComponents` $\rightarrow$ `content.body` (handled by formatter) | Merge complex segments into a clean body via engine. |
| **Telegram** | `TELEGRAM` | `amount`, `itemSKUs` $\rightarrow$ `payload.context` | Telegram specifics move to the context object. |
| **Push** | `PUSH` | `title/body` $\rightarrow$ `content.body`; `icon`/`image` $\rightarrow$ `content.mediaUrl` | Mapping items needed for APNs/FCM. |
| **SMS** | `SMS` | `senderId`, `message` $\rightarrow$ `target_identifier`, `content.body` | Flattened representation of mobile SMS. |
| **Email** | `EMAIL` | Same as WhatsApp. | Email follows the standard multi-media flow. |

---

## 💡 Implementation Notes for Developers
1.  **Idempotency:** All messages should use the unique `messageId` from the header to prevent duplicate sends in case of Kafka retries.
2.  **Expansion:** If a new channel (e.g., "WhatsApp Business API" specific features) is added, only `payload.delivery_instructions` or `payload.context` needs to change; the core infrastructure remains untouched.
3.  **Validation:** The `target_identifier` must be validated based on the `channel`. If `channel == 'SMS'`, valid data must be a phone number.
