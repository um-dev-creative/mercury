## GET /api/v1/verification-code/latest-status?userId={userId}
Example: `GET /api/v1/verification-code/latest-status?userId=b1c2d3e4-0002-4bcd-9ef0-000000000002`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of get latest verification status endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-VERIFICATION-002]](./placeholder)

---

## Description
Returns the `is_verified` status of the **most recent** verification code record for a given user. This endpoint is used by client applications to poll or check whether a user has successfully verified their account or completed a two-factor authentication challenge. The response is a single boolean value (`true` if the latest code has been verified, `false` otherwise).

### Implementation Notes
* The `userId` query parameter is **required**; omitting it will result in a `400 Bad Request`.
* Only the **latest** verification code record for the user is evaluated — previous codes are not considered.
* Returns `null` (in the JSON body) if no verification code record has ever been created for the given `userId`.
* A `404 Not Found` response is returned if no verification code record exists for the user.
* The boolean response body is returned directly — it is not wrapped in a JSON object.

---

### Endpoint
```bash
GET /api/v1/verification-code/latest-status?userId={userId}
```

### Parameters
| Name | Description |
| --- | --- |
| `userId` | The unique identifier of the user whose latest verification status is being queried (string, required) |

---

## Request

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Accept` | string | Specifies the media type expected — `application/json` |
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
| `Content-Type` | string | Response content type — `application/json` |

### Body
The response body is a single **boolean** value — not a JSON object.

| Field | Type | Description |
| --- | --- | --- |
| *(root value)* | boolean | `true` if the latest verification code for the user has been verified; `false` if it has not; `null` if no record exists |

### Payload
```json
true
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | The `is_verified` status of the latest verification code was retrieved successfully | See response headers above |
| 400 Bad Request | The `userId` query parameter is missing or malformed | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to query verification status for this user | See response headers above |
| 404 Not Found | No verification code record was found for the specified `userId` | See response headers above |
| 500 Internal Server Error | An unexpected error occurred while retrieving the verification status | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T09:45:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "No verification code found for userId 'b1c2d3e4-0002-4bcd-9ef0-000000000002'",
  "path": "/api/v1/verification-code/latest-status"
}
```
