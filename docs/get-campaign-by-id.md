## GET /api/v1/campaigns/{id}
Example: `GET /api/v1/campaigns/00000000-0000-0000-0000-0000000000a1`

### Modification Log
| Name              | Detail | Date |
|-------------------| --- | --- |
| Luis Antonio Mata | Add documentation for Get campaign by ID endpoint | 2026-03-14 |

---

### Related Story/Requirement Link(s)
Related Story: [[TBD-001]](https://placeholder.local/TBD-001)

---

## Description
Retrieves the full details of a campaign identified by its UUID. Returns channel-type, template reference, audit timestamps, status and metadata.

### Implementation Notes
* Returns 404 when campaign not found. The controller delegates to CampaignService#getById which throws CampaignNotFoundException for missing entities.
* No session-token header is currently enforced for this endpoint at controller level (assumed to be handled elsewhere if required).

---

### Endpoint
```bash
GET /api/v1/campaigns/{id}
```

### Parameters
| Name | Description |
| --- | --- |
| `id` | Path parameter: UUID of the campaign (required) |

---

## Request
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Accept` | string | Specifies the media type expected |
| `session-token` | string | JWT token for session authentication |
| `session-token-bkd` | string | JWT token for backend session |

### Body
*N/A*

### Payload
*N/A*

---

## Response
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | Response content type, usually `application/json` |

### Body
| Field | Type | Description |
| --- | --- | --- |
| `id` | string (UUID) | Unique identifier of the campaign |
| `name` | string | Human-readable campaign name |
| `channelType` | string | Channel type code (e.g. `email`, `sms`) |
| `templateId` | string (UUID) | UUID of the associated template definition |
| `status` | string | Current lifecycle status (e.g. `DRAFT`, `IN_PROGRESS`) |
| `totalRecipients` | integer | Total number of recipients |
| `scheduledAt` | string (date-time) | Scheduled execution timestamp or null |
| `createdAt` | string (date-time) | Creation timestamp |
| `updatedAt` | string (date-time) | Last update timestamp |
| `metadata` | object | Free-form metadata stored with the campaign |

### Payload
```json
{
  "id": "00000000-0000-0000-0000-0000000000a1",
  "name": "Summer Promo 2026",
  "channelType": "email",
  "templateId": "00000000-0000-0000-0000-0000000000b2",
  "status": "DRAFT",
  "totalRecipients": 50,
  "scheduledAt": null,
  "createdAt": "2026-03-14T10:00:00",
  "updatedAt": "2026-03-14T10:00:00",
  "metadata": { "owner": "user-123" }
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
|200 OK | Campaign found and returned. | See response headers above |
|400 Bad Request | Invalid UUID format supplied for id. | See response headers above |
|401 Unauthorized | Invalid or missing authentication token | See response headers above |
|403 Forbidden | Caller lacks permission to view this campaign | See response headers above |
|404 Not Found | Campaign with the given id does not exist | See response headers above |
|500 Internal Server Error | Unexpected internal error | See response headers above |

### Payload Error Example
```json
{
 "timestamp": "2026-03-14T12:00:00",
 "status": 404,
 "error": "Not Found",
 "message": "Campaign not found: 00000000-0000-0000-0000-0000000000a1",
 "path": "/api/v1/campaigns/00000000-0000-0000-0000-0000000000a1"
}
```

