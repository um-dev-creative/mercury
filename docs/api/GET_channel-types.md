## GET /api/v1/channel-types
Example: `GET /api/v1/channel-types`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of get all channel types endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CHANNEL-001]](./placeholder)

---

## Description
Retrieves the complete list of all communication channel types registered in the system, regardless of their enabled or active state. Channel types define the supported messaging channels (e.g., Email, SMS, Telegram, WhatsApp) and their associated configuration metadata. This endpoint is typically used by administrative UIs or back-office tools to display and manage available channels.

### Implementation Notes
* The response includes **all** channel types — both enabled and disabled.
* Use `GET /api/v1/channel-types/enabled` to retrieve only the channels available for active use.
* The `implementationClass` field identifies the Java class responsible for message dispatch on that channel; it is informational and not required by API consumers.
* Results are not paginated; the full list is always returned in a single response.

---

### Endpoint
```bash
GET /api/v1/channel-types
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
The response is an array of `ChannelTypeTO` objects.

| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | Unique identifier for the channel type |
| `code` | string | Short uppercase code identifying the channel (e.g., `EMAIL`, `SMS`) |
| `name` | string | Human-readable name of the channel type |
| `description` | string | Detailed description of the channel and its intended use |
| `icon` | string | Path or reference to the icon representing this channel |
| `enabled` | boolean | Whether the channel is enabled for sending messages |
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
    "id": "a7b8c9d0-0007-4012-e345-000000000007",
    "code": "SMS",
    "name": "SMS",
    "description": "Sends messages via short message service",
    "icon": "/icons/sms.svg",
    "enabled": false,
    "implementationClass": "com.prx.mercury.channel.SmsChannelService",
    "createdAt": "2025-01-15 10:05:00",
    "updatedAt": "2025-07-01 09:00:00",
    "active": false
  }
]
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | List of all channel types returned successfully | See response headers above |
| 400 Bad Request | Malformed request (e.g., incorrect Accept header format) | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to view channel type configuration | See response headers above |
| 404 Not Found | *N/A* — this endpoint always returns an array (empty if no records exist) | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while retrieving channel types | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T07:45:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/channel-types"
}
```
