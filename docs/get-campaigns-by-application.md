## GET /api/v1/campaigns/application/{applicationId}
Example: `GET /api/v1/campaigns/application/00000000-0000-0000-0000-000000000003`

### Modification Log
| Name              | Detail | Date |
|-------------------| --- | --- |
| Luis Antonio Mata | Add documentation for Get campaigns by application endpoint | 2026-03-14 |

---

### Related Story/Requirement Link(s)
Related Story: [[TBD-003]](https://placeholder.local/TBD-003)

---

## Description
Retrieves campaigns filtered by application id for the authenticated user. User id is derived from the `session-token` header.

### Implementation Notes
* Controller extracts user id from the session-token via `JwtUtil.getUidFromToken` and delegates to `CampaignService#getByUserIdAndApplicationId`.
* Returns an array of `CampaignDetailResponse` objects for campaigns created by the user and matching the application id.

---

### Endpoint
```bash
GET /api/v1/campaigns/application/{applicationId}
```

### Parameters
| Name | Description |
| --- | --- |
| `applicationId` | Path parameter: UUID of the application (required) |
| `session-token` | Header: session token used to derive the user id (required) |

---

## Request
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Accept` | string | Specifies the media type expected |
| `session-token` | string | JWT token for session authentication (required) |

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
Array of CampaignDetailResponse objects (see GET by id schema).

### Payload
```json
[
  {
    "id": "00000000-0000-0000-0000-0000000000a1",
    "name": "Name",
    "channelType": "email",
    "templateId": "00000000-0000-0000-0000-0000000000b2",
    "status": "DRAFT",
    "totalRecipients": null,
    "scheduledAt": null,
    "createdAt": null,
    "updatedAt": null,
    "metadata": null
  }
]
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
|200 OK | Campaigns retrieved successfully | See response headers above |
|400 Bad Request | Invalid request parameters | See response headers above |
|401 Unauthorized | Invalid or missing authentication token | See response headers above |
|403 Forbidden | Caller lacks permission to view these campaigns | See response headers above |

### Payload Error Example
```json
{
 "timestamp": "2026-03-14T13:00:00",
 "status": 401,
 "error": "Unauthorized",
 "message": "Invalid or missing authentication token",
 "path": "/api/v1/campaigns"
}
```


