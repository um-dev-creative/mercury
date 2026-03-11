## GET /api/v1/campaigns/{id}
Example: `GET /api/v1/campaigns/fe5e185c-5525-4155-b361-ce6960525b16`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of get campaign by id endpoint | 2026-03-11 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CAMPAIGN-002]](./placeholder)

---

## Description
Retrieves the full details of a single campaign identified by its UUID. The endpoint returns all stored campaign fields including the channel type code, template reference, current lifecycle status, audit timestamps and optional metadata. This endpoint is intended for campaign inspection, auditing, and admin UI integrations.

### Implementation Notes
* `id` must be a valid UUID string; a non-UUID value causes a `400 Bad Request` with a descriptive validation message.
* If no campaign with the given `id` exists in the database, a `404 Not Found` is returned with a structured error body.
* `createdAt` and `updatedAt` are server-managed timestamps populated at campaign creation and on each subsequent update.
* `metadata` is an optional free-form JSON object stored with the campaign; it may be `null` if no metadata was supplied at creation time.
* `scheduledAt` is `null` for immediately-executed campaigns.
* Authentication is required via the `session-token` header (and optionally `session-token-bkd` for backend calls).

---

### Endpoint
```bash
GET /api/v1/campaigns/{id}
```

### Parameters
| Name | Location | Type | Required | Description |
| --- | --- | --- | --- | --- |
| `id` | path | UUID (string) | Yes | Unique identifier of the campaign to retrieve |

---

## Request

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Accept` | string | Must be `application/json` |
| `session-token` | string | JWT token for session authentication |
| `session-token-bkd` | string | JWT token for backend session (optional for direct client calls) |

### Body
This endpoint does not accept a request body.

---

## Response

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | Response content type — `application/json` |

### Body
| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | Unique identifier of the campaign |
| `name` | string | Human-readable campaign name |
| `channelType` | string | Channel type code (e.g. `email`, `sms`, `telegram`, `whatsapp`, `push`) |
| `templateId` | UUID | UUID of the template definition used by this campaign |
| `status` | string | Current lifecycle status (e.g. `DRAFT`, `IN_PROGRESS`, `COMPLETED`) |
| `totalRecipients` | integer | Total number of recipients enrolled; `null` if not yet determined |
| `scheduledAt` | string (datetime) | Scheduled execution timestamp (`yyyy-MM-dd HH:mm:ss`); `null` for immediate execution |
| `createdAt` | string (datetime) | ISO-formatted timestamp when the campaign record was created (`yyyy-MM-dd HH:mm:ss`) |
| `updatedAt` | string (datetime) | ISO-formatted timestamp of the last update to the campaign record (`yyyy-MM-dd HH:mm:ss`) |
| `metadata` | object | Optional free-form metadata stored with the campaign; `null` if none was provided |

### Payload
```json
{
  "id": "fe5e185c-5525-4155-b361-ce6960525b16",
  "name": "Summer Promo 2026",
  "channelType": "email",
  "templateId": "a3f4e2d1-0001-4abc-8def-000000000001",
  "status": "DRAFT",
  "totalRecipients": 250,
  "scheduledAt": "2026-03-15 08:00:00",
  "createdAt": "2026-02-20 12:34:56",
  "updatedAt": "2026-02-21 08:12:03",
  "metadata": {
    "ownerId": "1a24da99-78d0-4221-96fa-64aa79a47bd1"
  }
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | Campaign found; full detail returned | See response headers above |
| 400 Bad Request | The supplied `id` is not a valid UUID | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to view this campaign | See response headers above |
| 404 Not Found | No campaign exists with the given `id` | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while retrieving the campaign | See response headers above |

### Payload Error Example

**404 Not Found**
```json
{
  "timestamp": "2026-03-11T02:10:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Campaign not found: fe5e185c-5525-4155-b361-ce6960525b16",
  "path": "/api/v1/campaigns/fe5e185c-5525-4155-b361-ce6960525b16"
}
```

**400 Bad Request — invalid UUID**
```json
{
  "timestamp": "2026-03-11T02:10:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value 'not-a-uuid' for parameter 'id'",
  "path": "/api/v1/campaigns/not-a-uuid"
}
```
