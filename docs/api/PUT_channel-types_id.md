## PUT /api/v1/channel-types/{id}
Example: `PUT /api/v1/channel-types/f6a7b8c9-0006-4f01-d234-000000000006`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of update channel type endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CHANNEL-006]](./placeholder)

---

## Description
Performs a **full replacement** of an existing communication channel type identified by its UUID. All fields in the request body overwrite the current values stored for the record. This endpoint is used by platform administrators to update the metadata, configuration, or implementation class of a channel type — for example, when migrating to a new dispatch library or correcting an icon reference.

### Implementation Notes
* This is a **full update** (PUT semantics): every editable field in the payload replaces the existing value. Omitting a field will reset it to `null` or its default unless the service layer enforces otherwise.
* The `id`, `createdAt`, and `updatedAt` fields are managed by the server; any values provided for these fields in the request body are ignored — the path `{id}` is the authoritative identifier.
* The `code` field must remain unique across all channel types; changing the code to one that already exists will result in a conflict error.
* `updatedAt` is automatically refreshed to the current server timestamp on a successful update.

---

### Endpoint
```bash
PUT /api/v1/channel-types/{id}
```

### Parameters
| Name | Description |
| --- | --- |
| `id` | UUID of the channel type to update (format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`) |

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
Request body is **required**. Provide the full updated representation of the channel type.

### Payload
```json
{
  "code": "EMAIL",
  "name": "Email (Updated)",
  "description": "Sends transactional and marketing messages via SMTP",
  "icon": "/icons/email-v2.svg",
  "enabled": true,
  "implementationClass": "com.prx.mercury.channel.EmailChannelServiceV2",
  "active": true
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
| `id` | UUID | Unique identifier for the channel type (unchanged) |
| `code` | string | Short uppercase code identifying the channel |
| `name` | string | Updated human-readable name of the channel type |
| `description` | string | Updated description of the channel |
| `icon` | string | Updated path or reference to the channel icon |
| `enabled` | boolean | Updated enabled flag |
| `implementationClass` | string | Updated fully qualified Java class for message dispatch |
| `createdAt` | string (datetime) | Original creation timestamp (unchanged, `yyyy-MM-dd HH:mm:ss`) |
| `updatedAt` | string (datetime) | Server-refreshed timestamp of this update (`yyyy-MM-dd HH:mm:ss`) |
| `active` | boolean | Updated active flag |

### Payload
```json
{
  "id": "f6a7b8c9-0006-4f01-d234-000000000006",
  "code": "EMAIL",
  "name": "Email (Updated)",
  "description": "Sends transactional and marketing messages via SMTP",
  "icon": "/icons/email-v2.svg",
  "enabled": true,
  "implementationClass": "com.prx.mercury.channel.EmailChannelServiceV2",
  "createdAt": "2025-01-15 10:00:00",
  "updatedAt": "2026-03-10 09:00:00",
  "active": true
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | Channel type was updated successfully and the revised record is returned | See response headers above |
| 400 Bad Request | The payload is malformed or a required field is missing/blank | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have administrative permission to update channel types | See response headers above |
| 404 Not Found | No channel type with the specified UUID was found | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while updating the channel type | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T09:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Channel type with id 'f6a7b8c9-0006-4f01-d234-000000000006' not found",
  "path": "/api/v1/channel-types/f6a7b8c9-0006-4f01-d234-000000000006"
}
```
