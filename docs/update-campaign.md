## PUT /api/v1/campaigns/{id}
Example: `PUT /api/v1/campaigns/00000000-0000-0000-0000-0000000000a1`

### Modification Log
| Name              | Detail | Date |
|-------------------| --- | --- |
| Luis Antonio Mata | Add documentation for Update Campaign endpoint | 2026-03-14 |

---

### Related Story/Requirement Link(s)
Related Story: [[TBD-002]](https://placeholder.local/TBD-002)

---

## Description
Updates mutable fields of an existing campaign such as name, template, recipients, scheduledAt, status and metadata. Only the owner (createdBy) is allowed to perform updates — the service enforces a basic permission check.

### Implementation Notes
* The method finds the campaign and applies only provided mutable fields. If no mutable fields are provided, the service logs and returns the existing campaign without persisting changes.
* Template changes validate template existence using the TemplateDefinedRepository — missing template will cause an IllegalArgumentException (400).
* Recipients are deduplicated by identifier; an empty recipient list after deduplication raises IllegalArgumentException (400).

---

### Endpoint
```bash
PUT /api/v1/campaigns/{id}
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
| `session-token` | string | JWT token for session authentication (required) |

### Body
Request body is required and must conform to the UpdateCampaignRequest schema.

### Payload
```json
{
  "name": "Updated Campaign Name",
  "templateId": "00000000-0000-0000-0000-0000000000b2",
  "recipients": [ { "identifier": "a@example.com", "name": "A" } ],
  "templateParams": { "k": "v" },
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
Returns the updated CampaignDetailResponse (same schema as GET by id).

### Payload
```json
{
  "id": "00000000-0000-0000-0000-0000000000a1",
  "name": "Updated Campaign Name",
  "channelType": "email",
  "templateId": "00000000-0000-0000-0000-0000000000b2",
  "status": "SCHEDULED",
  "totalRecipients": 1,
  "scheduledAt": "2026-03-20T10:00:00",
  "createdAt": "2026-03-14T10:00:00",
  "updatedAt": "2026-03-14T12:00:00",
  "metadata": { "templateParams": { "k": "v" } }
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
|200 OK | Campaign updated successfully and returned | See response headers above |
|400 Bad Request | Invalid request payload or template not found | See response headers above |
|401 Unauthorized | Invalid or missing authentication token | See response headers above |
|403 Forbidden | Caller lacks permission to update this campaign | See response headers above |
|404 Not Found | Campaign not found | See response headers above |
|422 Unprocessable Entity | Business rule violation | See response headers above |

### Payload Error Example
```json
{
 "timestamp": "2026-03-14T12:30:00",
 "status": 403,
 "error": "Forbidden",
 "message": "Caller lacks permission to update this campaign",
 "path": "/api/v1/campaigns/00000000-0000-0000-0000-0000000000a1"
}
```

