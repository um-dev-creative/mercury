## POST /api/v1/channel-types
Example: `POST /api/v1/channel-types`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of create channel type endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CHANNEL-005]](./placeholder)

---

## Description
Registers a new communication channel type in the system. Channel types define the supported messaging channels (e.g., Email, SMS, Telegram, WhatsApp) along with their configuration metadata and the Java implementation class responsible for dispatch. This endpoint is intended for administrators or platform operators who need to extend the system with new delivery channels.

### Implementation Notes
* The `code` field must be unique; attempting to create a channel type with a duplicate code will result in an error.
* The `implementationClass` must reference a valid, deployed Spring bean in the application context; an incorrect class name will cause runtime dispatch failures.
* The server generates the `id`, `createdAt`, and `updatedAt` fields automatically; any values provided for these fields in the request are ignored.
* Newly created channel types are **not** enabled by default; use `PATCH /api/v1/channel-types/{id}/toggle?enabled=true` to activate them.

---

### Endpoint
```bash
POST /api/v1/channel-types
```

### Parameters
| Name | Description |
| --- | --- |
| *N/A* | This endpoint does not accept path variables or query parameters. |

---

## Request

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | Must be `application/json` |
| `Accept` | string | Specifies the media type expected — `application/json` |
| `session-token` | string | JWT token for session authentication |
| `session-token-bkd` | string | JWT token for backend session |

### Body
Request body is **required**. Send a JSON payload representing the channel type to be created.

### Payload
```json
{
  "code": "WHATSAPP",
  "name": "WhatsApp",
  "description": "Sends messages via the WhatsApp Business API",
  "icon": "/icons/whatsapp.svg",
  "enabled": false,
  "implementationClass": "com.prx.mercury.channel.WhatsAppChannelService",
  "active": false
}
```

---

## Response

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | Response content type — `application/json` |

### Body
| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | Server-generated unique identifier for the newly created channel type |
| `code` | string | Short uppercase code identifying the channel |
| `name` | string | Human-readable name of the channel type |
| `description` | string | Detailed description of the channel and its intended use |
| `icon` | string | Path or reference to the icon representing this channel |
| `enabled` | boolean | Whether the channel is enabled for sending messages |
| `implementationClass` | string | Fully qualified Java class that implements the message dispatch logic |
| `createdAt` | string (datetime) | Server-generated timestamp when the record was created (`yyyy-MM-dd HH:mm:ss`) |
| `updatedAt` | string (datetime) | Server-generated timestamp of the last update (`yyyy-MM-dd HH:mm:ss`) |
| `active` | boolean | Whether the channel is currently active in the system |

### Payload
```json
{
  "id": "c9d0e1f2-0009-4234-a567-000000000009",
  "code": "WHATSAPP",
  "name": "WhatsApp",
  "description": "Sends messages via the WhatsApp Business API",
  "icon": "/icons/whatsapp.svg",
  "enabled": false,
  "implementationClass": "com.prx.mercury.channel.WhatsAppChannelService",
  "createdAt": "2026-03-10 08:00:00",
  "updatedAt": "2026-03-10 08:00:00",
  "active": false
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | Channel type was created successfully and the new record is returned | See response headers above |
| 400 Bad Request | One or more required fields are missing or the payload is malformed | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have administrative permission to create channel types | See response headers above |
| 404 Not Found | *N/A* | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while creating the channel type | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T08:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: 'code' must not be blank",
  "path": "/api/v1/channel-types"
}
```
