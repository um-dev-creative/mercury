## GET /api/v1/campaigns/{id}/progress
Example: `GET /api/v1/campaigns/00000000-0000-0000-0000-0000000000a1/progress`

### Modification Log
| Name              | Detail | Date |
|-------------------|---|---|
| Luis Antonio Mata | Add documentation for Get campaign progress endpoint | 2026-03-14 |

---

### Related Story/Requirement Link(s)
Related Story: [[TBD-005]](https://placeholder.local/TBD-005)

---

## Description
Retrieves progress metrics for a campaign, including counts of sent, delivered, failed, and other channel-specific statistics. This projection is built by the `CampaignProgressService` from metrics stored in the database.

### Implementation Notes
* The `CampaignService#getProgress(UUID)` delegates to `CampaignProgressService#getProgress`. If the campaign is unknown the service may throw an exception.
* The progress object is not persisted in a separate table but is computed from metrics; exact fields depend on the `CampaignProgressTO` shape.

---

### Endpoint
```bash
GET /api/v1/campaigns/{id}/progress
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
Structured `CampaignProgressTO` fields such as:
| Field | Type | Description |
| --- | --- | --- |
| `campaignId` | string (UUID) | Campaign identifier |
| `name` | string | Campaign name |
| `totalRecipients` | integer | Total recipients |
| `sent` | integer | Number of sent messages |
| `delivered` | integer | Number of delivered messages |
| `failed` | integer | Number of failed messages |
| `createdAt` | string (date-time) | When the campaign was created |
| `lastUpdated` | string (date-time) | When progress was last computed |

### Payload
```json
{
  "campaignId": "00000000-0000-0000-0000-0000000000a1",
  "name": "Summer Promo 2026",
  "totalRecipients": 100,
  "sent": 80,
  "delivered": 70,
  "failed": 10,
  "createdAt": "2026-03-14T10:00:00",
  "lastUpdated": "2026-03-14T13:00:00"
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
|200 OK | Progress projection returned | See response headers above |
|400 Bad Request | Invalid UUID format for id | See response headers above |
|404 Not Found | Campaign not found | See response headers above |
|500 Internal Server Error | Unexpected internal error | See response headers above |

### Payload Error Example
```json
{
 "timestamp": "2026-03-14T13:15:00",
 "status": 404,
 "error": "Not Found",
 "message": "Campaign not found: 00000000-0000-0000-0000-0000000000a1",
 "path": "/api/v1/campaigns/00000000-0000-0000-0000-0000000000a1/progress"
}
```

