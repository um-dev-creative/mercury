## GET /api/v1/channel-types/code/{code}
Example: `GET /api/v1/channel-types/code/EMAIL`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of get channel type by code endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CHANNEL-003]](./placeholder)

---

## Description
Retrieves a single communication channel type identified by its short **code** (e.g., `EMAIL`, `SMS`, `TELEGRAM`). This endpoint is useful when the caller knows the channel code — typically obtained from the campaign creation form or a business rule — and needs to retrieve the full configuration metadata for that channel before initiating a campaign or validating a request.

### Implementation Notes
* The `code` path variable is **case-sensitive**; use uppercase codes as stored in the system (e.g., `EMAIL`, not `email`).
* A `404 Not Found` response is returned if no channel type with the given code exists.
* This endpoint returns both enabled and disabled channel types; use `enabled` and `active` fields to determine usability.

---

### Endpoint
```bash
GET /api/v1/channel-types/code/{code}
```

### Parameters
| Name | Description |
| --- | --- |
| `code` | Short uppercase code of the channel type to retrieve (e.g., `EMAIL`, `SMS`, `TELEGRAM`, `WHATSAPP`) |

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
| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | Unique identifier for the channel type |
| `code` | string | Short uppercase code identifying the channel |
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
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | Channel type with the given code was found and returned | See response headers above |
| 400 Bad Request | The `code` path variable is blank or malformed | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to view channel type details | See response headers above |
| 404 Not Found | No channel type with the specified code was found | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while retrieving the channel type | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T07:45:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Channel type with code 'UNKNOWN' not found",
  "path": "/api/v1/channel-types/code/UNKNOWN"
}
```
