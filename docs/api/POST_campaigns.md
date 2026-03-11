## POST /api/v1/campaigns
Example: `POST /api/v1/campaigns`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of create campaign endpoint | 2024-01-01 |
| Luis Antonio Mata | Updated response status to 201 Created; added GlobalExceptionHandler for structured error responses | 2026-03-11 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-CAMPAIGN-001]](./placeholder)

---

## Description
Creates a new messaging campaign and asynchronously publishes individual messages to all specified recipients via the chosen communication channel (e.g., email, SMS, Telegram, WhatsApp). The endpoint returns a `201 Created` response after persisting the campaign, while message delivery is handled in the background via the configured message broker (Kafka). This design allows callers to submit large recipient lists without blocking.

### Implementation Notes
* The response status `201 Created` confirms the campaign record was created and persisted in the database with a generated UUID and audit timestamps.
* Message dispatching to Kafka is initiated asynchronously after the 201 is returned — it does **not** guarantee that all messages have been delivered.
* `channelTypeCode` must match an existing, **enabled** channel type; if the channel is not found or is disabled a `422 Unprocessable Entity` is returned.
* `scheduledAt` is optional. When omitted, the campaign is executed immediately. When provided, execution is deferred to the specified date/time.
* `templateParams` are key-value pairs merged into the template at render time; keys must correspond to placeholders defined in the referenced template.
* Each recipient's `customParams` can override global `templateParams` at the individual message level.
* Campaign progress can be tracked via the campaign progress tracking mechanism once the campaign is created.

---

### Endpoint
```bash
POST /api/v1/campaigns
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
Request body is **required**. Send a JSON payload describing the campaign to be created.

### Payload
```json
{
  "name": "Spring Promotion 2026",
  "channelTypeCode": "EMAIL",
  "templateId": "a3f4e2d1-0001-4abc-8def-000000000001",
  "userId": "b1c2d3e4-0002-4bcd-9ef0-000000000002",
  "recipients": [
    {
      "identifier": "alice@example.com",
      "name": "Alice Smith",
      "customParams": {
        "firstName": "Alice"
      }
    },
    {
      "identifier": "bob@example.com",
      "name": "Bob Jones",
      "customParams": {}
    }
  ],
  "templateParams": {
    "promoCode": "SPRING20",
    "expiryDate": "2026-04-30"
  },
  "scheduledAt": "2026-03-15 08:00:00",
  "status": "DRAFT",
  "applicationId": "d4e5f6a7-0004-4def-b012-000000000004"
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
| `campaignId` | UUID | Unique identifier assigned to the newly created campaign |
| `name` | string | Name of the campaign as provided in the request |
| `status` | string | Initial lifecycle status of the campaign (e.g., `DRAFT`, `QUEUED`) |
| `totalRecipients` | integer | Total number of recipients registered for this campaign |
| `createdAt` | string (datetime) | ISO-formatted timestamp when the campaign record was created (`yyyy-MM-dd HH:mm:ss`) |
| `scheduledAt` | string (datetime) | ISO-formatted timestamp when the campaign is scheduled to run; `null` if immediate |

### Payload
```json
{
  "campaignId": "e5f6a7b8-0005-4ef0-c123-000000000005",
  "name": "Spring Promotion 2026",
  "status": "QUEUED",
  "totalRecipients": 2,
  "createdAt": "2026-03-10 07:45:00",
  "scheduledAt": "2026-03-15 08:00:00"
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 201 Created | Campaign was created and persisted; messages are being queued for delivery | See response headers above |
| 400 Bad Request | One or more required fields are missing, blank, or the recipient list is empty | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to create campaigns for the specified `applicationId` | See response headers above |
| 404 Not Found | The referenced `templateId` does not exist | See response headers above |
| 422 Unprocessable Entity | The specified `channelTypeCode` was not found or is currently disabled | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while creating the campaign | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T07:45:00.000Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Channel type 'WHATSAPP' is not enabled",
  "path": "/api/v1/campaigns"
}
```
