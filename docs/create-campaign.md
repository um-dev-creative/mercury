## POST /api/v1/campaigns
Example: `POST /api/v1/campaigns`

### Modification Log
| Name              | Detail | Date |
|-------------------| --- | --- |
| Luis Antonio Mata | Add documentation for Create Campaign endpoint | 2026-03-14 |

---

### Related Story/Requirement Link(s)
Related Story: [[TBD-000]](https://placeholder.local/TBD-000)

---

## Description
Creates a new messaging campaign and publishes messages to recipients via the specified channel. The controller validates the request, persists the campaign, creates initial metrics and publishes per-recipient messages to Kafka. The endpoint returns the initial campaign details after creation.

### Implementation Notes
* This operation persists the campaign and publishes per-recipient messages to Kafka. The controller waits for the asynchronous creation flow to complete (the service method returns a CompletableFuture that is joined).
* If the requested channel type is disabled or the template is missing the service will reject the request (422 or 400 respectively).
* The endpoint does not require a `session-token` header in the current implementation (no authentication header is read by the controller).

---

### Endpoint
```bash
POST /api/v1/campaigns
```

### Parameters
| Name | Description                              |
| --- |------------------------------------------|
| `id` | Campaign identifier                      |
| `enabled` | Campaign enabled / disable status status |

---

## Request
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Accept` | string | Specifies the media type expected |
| `session-token` | string | JWT token for session authentication (not required by this endpoint) |
| `session-token-bkd` | string | JWT token for backend session |
| `[other-header]` | [type] | [description] |

### Body
Request body is required and must conform to the CreateCampaignRequest schema.

### Payload
```json
{
  "name": "Welcome Campaign",
  "channelTypeCode": "email",
  "templateId": "00000000-0000-0000-0000-000000000001",
  "userId": "00000000-0000-0000-0000-000000000002",
  "recipients": [
    { "identifier": "jane.doe@example.com", "name": "Jane Doe" }
  ],
  "templateParams": {
    "firstName": "Jane"
  },
  "scheduledAt": "2026-03-20T10:00:00",
  "status": "SCHEDULED",
  "applicationId": "00000000-0000-0000-0000-000000000003"
}
```

---

## Response
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | Response content type, usually `application/json` |

### Body
| Field | Type | Description |
| --- | --- | --- |
| `campaignId` | string (UUID) | Unique id of the created campaign |
| `name` | string | Campaign name |
| `status` | string | Resolved lifecycle status (e.g. `DRAFT`, `SCHEDULED`) |
| `totalRecipients` | integer | Total recipients count |
| `createdAt` | string (date-time) | Created timestamp |
| `scheduledAt` | string (date-time) | Scheduled execution time (if provided) |

### Payload
```json
{
  "campaignId": "00000000-0000-0000-0000-0000000000a1",
  "name": "Welcome Campaign",
  "status": "SCHEDULED",
  "totalRecipients": 1,
  "createdAt": "2026-03-14T12:00:00",
  "scheduledAt": "2026-03-20T10:00:00"
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
|201 Created | Campaign created successfully and messages published | See response headers above |
|400 Bad Request | Invalid request payload or missing required fields | See response headers above |
|401 Unauthorized | Invalid or missing authentication token | See response headers above |
|403 Forbidden | Caller lacks permission to create campaigns | See response headers above |
|422 Unprocessable Entity | Channel type not found or disabled | See response headers above |
|500 Internal Server Error | Unexpected internal error | See response headers above |

### Payload Error Example
```json
{
 "timestamp": "2026-03-14T12:00:00",
 "status": 400,
 "error": "Bad Request",
 "message": "At least one recipient is required",
 "path": "/api/v1/campaigns"
}
```

