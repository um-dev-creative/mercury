## POST /api/v1/verification-code
Example: `POST /api/v1/verification-code`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of send verification code endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-VERIFICATION-001]](./placeholder)

---

## Description
Dispatches a verification code to the user identified by `userId` within the scope of the specified `applicationId`. The caller supplies the code to be delivered; the platform routes the message through the appropriate channel configured for the application. This endpoint is typically invoked during account verification, two-factor authentication, or phone/email confirmation flows.

### Implementation Notes
* The `code` field must not be null or empty — validation is enforced in the request record's compact constructor and will return `400 Bad Request` if violated.
* The `userId` must not be null; the service uses it to resolve the recipient's contact information and preferred delivery channel.
* The endpoint returns `200 OK` with **no body** on success — the caller should not attempt to parse a response payload.
* A `400 Bad Request` is returned if the resolved contact information (e.g., phone number) is invalid or undeliverable.
* A `500 Internal Server Error` indicates a failure in the downstream messaging infrastructure.

---

### Endpoint
```bash
POST /api/v1/verification-code
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
Request body is **required**. Send a JSON payload identifying the user and the verification code to send.

### Payload
```json
{
  "applicationId": "d4e5f6a7-0004-4def-b012-000000000004",
  "userId": "b1c2d3e4-0002-4bcd-9ef0-000000000002",
  "code": "847291"
}
```

---

## Response

### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | Not present — response has no body on success (`200 OK` with empty body) |

### Body
This endpoint returns **no body** on success.

| Field | Type | Description |
| --- | --- | --- |
| *N/A* | — | No response body is returned on a successful `200 OK` |

### Payload
*N/A*

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | Verification code was dispatched successfully to the user | See response headers above |
| 400 Bad Request | The `code` is null/empty, `userId` is null, or the target contact information (e.g., phone number) is invalid | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to send verification codes for the specified `applicationId` | See response headers above |
| 404 Not Found | The specified `userId` or `applicationId` does not exist | See response headers above |
| 500 Internal Server Error | An unexpected error occurred in the messaging infrastructure while sending the code | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T09:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: 'code' must not be null or empty",
  "path": "/api/v1/verification-code"
}
```
