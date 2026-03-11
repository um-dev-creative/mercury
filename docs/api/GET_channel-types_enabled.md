## GET /api/v1/channel-types/enabled
Example: `GET /api/v1/channel-types/enabled`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of get enabled channel types endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CHANNEL-002]](./placeholder)

---

## Description
Retrieves only the communication channel types that are currently **enabled** in the system. This is the recommended endpoint for consumer-facing features such as campaign creation wizards or messaging configuration screens, where only viable send channels should be offered to the user. Disabled channels are excluded from the response.

### Implementation Notes
* Only channel types where `enabled = true` are included in the response.
* This endpoint is preferred over `GET /api/v1/channel-types` when the caller needs to present selectable channels to an end user.
* Results are not paginated; the full list of enabled channels is always returned in a single response.
* A channel may be `enabled` but not yet `active`; consumers should check the `active` flag if they require confirmed operational readiness.

---

### Endpoint
```bash
GET /api/v1/channel-types/enabled
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
| `Accept` | string | Specifies the media type expected — `application/json` |
| `session-token` | string | JWT token for session authentication |
| `session-token-bkd` | string | JWT token for backend session |

### Body
Request body is **not required**.

### Payload
*N/A*

---

## Response

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | Response content type — `application/json` |

### Body
The response is an array of `ChannelTypeTO` objects where `enabled = true`.

| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | Unique identifier for the channel type |
| `code` | string | Short uppercase code identifying the channel (e.g., `EMAIL`, `TELEGRAM`) |
| `name` | string | Human-readable name of the channel type |
| `description` | string | Detailed description of the channel and its intended use |
| `icon` | string | Path or reference to the icon representing this channel |
| `enabled` | boolean | Always `true` in this response — only enabled channels are returned |
| `implementationClass` | string | Fully qualified Java class that implements the message dispatch logic |
| `createdAt` | string (datetime) | Timestamp when this channel type was registered (`yyyy-MM-dd HH:mm:ss`) |
| `updatedAt` | string (datetime) | Timestamp of the last update to this channel type (`yyyy-MM-dd HH:mm:ss`) |
| `active` | boolean | Whether the channel is currently active in the system |

### Payload
```json
[
  {
    "id": "f6a7b8c9-0006-4f01-d234-000000000006",
    "code": "EMAIL",
    "name": "Email",
    "description": "Sends messages via SMTP email protocol",
    "icon": "/icons/email.svg",
    "enabled": true,
    "implementationClass": "com.prx.mercury.channel.EmailChannelService",
    "createdAt": "2025-01-15 10:00:00",
    "updatedAt": "2025-06-20 14:30:00",
    "active": true
  },
  {
    "id": "b8c9d0e1-0008-4123-f456-000000000008",
    "code": "TELEGRAM",
    "name": "Telegram",
    "description": "Sends messages via the Telegram Bot API",
    "icon": "/icons/telegram.svg",
    "enabled": true,
    "implementationClass": "com.prx.mercury.channel.TelegramChannelService",
    "createdAt": "2025-03-10 11:00:00",
    "updatedAt": "2025-08-05 16:45:00",
    "active": true
  }
]
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | List of enabled channel types returned successfully (may be an empty array if none are enabled) | See response headers above |
| 400 Bad Request | Malformed request (e.g., incorrect Accept header format) | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to view channel type configuration | See response headers above |
| 404 Not Found | *N/A* — this endpoint always returns an array (empty if no enabled records exist) | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while retrieving enabled channel types | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T07:45:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/channel-types/enabled"
}
```
