## PATCH /api/v1/channel-types/{id}/toggle?enabled={true|false}
Example: `PATCH /api/v1/channel-types/f6a7b8c9-0006-4f01-d234-000000000006/toggle?enabled=true`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of toggle channel type endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CHANNEL-007]](./placeholder)

---

## Description
Enables or disables a communication channel type identified by its UUID without modifying any other attributes of the record. This is a lightweight administrative operation used to control which channels are available for campaign creation and message dispatch. Disabling a channel prevents new campaigns from selecting it, but does not affect campaigns already in progress.

### Implementation Notes
* This endpoint returns `204 No Content` on success — there is **no** response body.
* The `enabled` query parameter is **required** and must be a boolean (`true` or `false`).
* Toggling a channel to `enabled=false` will cause subsequent calls to `GET /api/v1/channel-types/enabled` to exclude it, and `POST /api/v1/campaigns` with that channel code will return `422 Unprocessable Entity`.
* Campaigns already queued or in flight when a channel is disabled are **not** interrupted.
* The server automatically updates the `updatedAt` timestamp of the record on a successful toggle.

---

### Endpoint
```bash
PATCH /api/v1/channel-types/{id}/toggle?enabled={true|false}
```

### Parameters
| Name | Description |
| --- | --- |
| `id` | UUID of the channel type to enable or disable (format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`) |
| `enabled` | Boolean flag — `true` to enable the channel, `false` to disable it |

---

## Request

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Accept` | string | Specifies the media type expected — not required for `204 No Content` responses |
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
| `Content-Type` | string | Not present — response has no body (`204 No Content`) |

### Body
This endpoint returns **no body**. A `204 No Content` status indicates the operation was applied successfully.

| Field | Type | Description |
| --- | --- | --- |
| *N/A* | — | No response body is returned on success |

### Payload
*N/A*

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 204 No Content | Channel type was successfully enabled or disabled | See response headers above |
| 400 Bad Request | The `enabled` query parameter is missing or is not a valid boolean value | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have administrative permission to toggle channel types | See response headers above |
| 404 Not Found | No channel type with the specified UUID was found | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while toggling the channel type | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T09:15:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Channel type with id 'f6a7b8c9-0006-4f01-d234-000000000006' not found",
  "path": "/api/v1/channel-types/f6a7b8c9-0006-4f01-d234-000000000006/toggle"
}
```
