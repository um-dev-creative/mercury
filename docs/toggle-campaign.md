## PATCH /api/v1/campaigns/{id}/toggle
Example: `PATCH /api/v1/campaigns/00000000-0000-0000-0000-000000000101/toggle?enabled=false`

### Modification Log
| Name              | Detail | Date |
|-------------------| --- | --- |
| Luis Antonio Mata | Add documentation for Toggle Campaign endpoint | 2026-03-14 |

---

### Related Story/Requirement Link(s)
Related Story: [[TBD-004]](https://placeholder.local/TBD-004)

---

## Description
Enable or disable (pause/resume) a campaign by id. Use this endpoint to temporarily halt dispatch of messages for operational control (emergency stops, throttling, etc.). The change is persisted on the campaign record.

### Implementation Notes
* Path parameter `id` is required and must be a UUID.
* Query parameter `enabled` is required and is a boolean — `true` to enable/resume, `false` to disable/pause.
* Header `session-token` is required; the controller extracts the user id via `JwtUtil.getUidFromToken` and performs a basic ownership permission check: only the campaign owner (`createdBy`) may toggle. A `ForbiddenException` (403) is thrown otherwise.
* This implementation persists the `enabled` flag and will not attempt to cancel already dispatched/in-flight messages. Downstream dispatch/scheduler components should check `campaign.enabled` before publishing messages — toggling to `false` prevents future dispatches but does not interrupt messages already sent.
* Enabling a campaign performs a basic business validation: campaigns must have an associated template to be enabled. Violations return 422 Unprocessable Entity.
* The action is logged at INFO with campaign id, requester id, previous state and new state. An audit event TODO is present (implement audit emission as required by your compliance needs).

---

### Endpoint
```bash
PATCH /api/v1/campaigns/{id}/toggle?enabled={true|false}
```

### Parameters
| Name | Description |
| --- | --- |
| `id` | Path parameter: UUID of the campaign (required) |
| `enabled` | Query parameter: boolean (required). true to enable/resume, false to disable/pause |

---

## Request
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Accept` | string | Specifies the media type expected |
| `session-token` | string | JWT token for session authentication (required) |
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
*N/A* (204 No Content on success)

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
|204 No Content | Campaign toggled successfully | See response headers above |
|400 Bad Request | Invalid parameters (e.g., invalid UUID or malformed query) | See response headers above |
|401 Unauthorized | Invalid or missing authentication token | See response headers above |
|403 Forbidden | Caller lacks permission to toggle this campaign | See response headers above |
|404 Not Found | Campaign not found or soft-deleted | See response headers above |
|422 Unprocessable Entity | Business rule violation (e.g., enabling without required template) | See response headers above |
|500 Internal Server Error | Unexpected error | See response headers above |

### Payload Error Example
```json
{
 "timestamp": "2026-03-14T13:30:00",
 "status": 422,
 "error": "Unprocessable Entity",
 "message": "Cannot enable campaign without associated template",
 "path": "/api/v1/campaigns/00000000-0000-0000-0000-000000000101/toggle"
}
```

---

## Examples (curl)

Disable campaign:
```bash
curl -X PATCH "https://api.example.com/api/v1/campaigns/00000000-0000-0000-0000-000000000101/toggle?enabled=false" \
  -H "session-token: <token>"
# Expected: 204 No Content
```

Enable campaign:
```bash
curl -X PATCH "https://api.example.com/api/v1/campaigns/00000000-0000-0000-0000-000000000101/toggle?enabled=true" \
  -H "session-token: <token>"
# Expected: 204 No Content
```

---

## DB / persistence notes
* Campaign entity now contains a boolean `enabled` column (default true). A migration script has been added under `src/main/resources/db/migration/V2026__add_campaign_enabled_column.sql`.
* Ensure downstream dispatchers check `campaign.enabled` before publishing messages.

---

## Logging & auditing
* Toggle actions are logged at INFO with userId, campaignId, previous state, and new state.
* Consider emitting an audit event (Kafka or audit table) to record toggles for compliance. The current implementation leaves a TODO where this can be added.

---

## Tests to add / updated
* Unit tests for service toggle logic: valid toggles, invalid toggles (e.g., enabling with missing template), permission checks (added).
* Controller MockMvc tests for 204 response and error cases (unit test added). Integration tests recommended for end-to-end verification.

