## POST /api/v1/mail
Example: `POST /api/v1/mail`

### Modification Log
| Name | Detail | Date |
| --- | --- | --- |
| Luis Antonio Mata | Initial implementation of send email endpoint | 2024-01-01 |

---

### Related Story/Requirement Link(s)
Related Story: [[MERCURY-MAIL-001]](./placeholder)

---

## Description
Sends an email to one or more recipients using a pre-defined template. The caller provides the template identifier, sender, recipients, subject, body, and optional dynamic parameters that are merged into the template at send time. This endpoint is the primary entry point for all transactional and notification emails dispatched by the Mercury platform.

### Implementation Notes
* The `templateDefinedId` must reference an existing, active template; passing an unknown UUID will result in a processing error.
* The `params` map is merged into the template at render time; keys must match the placeholders defined in the referenced template.
* `sendDate` is optional; when omitted the email is dispatched immediately by the underlying mail service.
* Both `to` and `cc` lists must not be null; pass an empty list `[]` for `cc` when no carbon-copy recipients are required.
* Each `EmailContact` in the `to`/`cc` lists must contain a valid RFC-5321 email address.

---

### Endpoint
```bash
POST /api/v1/mail
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
Request body is **required**. Send a JSON payload representing the email to be dispatched.

### Payload
```json
{
  "templateDefinedId": "a3f4e2d1-0001-4abc-8def-000000000001",
  "userId": "b1c2d3e4-0002-4bcd-9ef0-000000000002",
  "from": "no-reply@prx.com",
  "to": [
    {
      "email": "john.doe@example.com",
      "name": "John Doe",
      "alias": "johnd"
    }
  ],
  "cc": [
    {
      "email": "manager@example.com",
      "name": "Manager",
      "alias": null
    }
  ],
  "subject": "Your account has been activated",
  "body": "Hello John, your account is now active.",
  "sendDate": "2026-03-10 09:00:00",
  "params": {
    "activationLink": "https://app.prx.com/activate?token=abc123",
    "userName": "John Doe"
  }
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
| `id` | UUID | Unique identifier assigned to the dispatched email record |
| `status` | string | Current status of the send operation (e.g., `SENT`, `QUEUED`, `FAILED`) |
| `message` | string | Human-readable description of the operation outcome |

### Payload
```json
{
  "id": "c9d8e7f6-0003-4cde-af01-000000000003",
  "status": "SENT",
  "message": "Email dispatched successfully"
}
```

### Code Response
| Status Code | Description | Headers |
| --- | --- | --- |
| 200 OK | Email was accepted and dispatched successfully | See response headers above |
| 400 Bad Request | One or more required fields are missing, blank, or contain an invalid email format | See response headers above |
| 401 Unauthorized | Invalid or missing authentication token | See response headers above |
| 403 Forbidden | The caller does not have permission to send emails on behalf of the specified `userId` | See response headers above |
| 404 Not Found | The referenced `templateDefinedId` does not exist | See response headers above |
| 500 Internal Server Error | An unexpected error occurred in the mail dispatch service | See response headers above |

### Payload Error Example
```json
{
  "timestamp": "2026-03-10T09:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: 'from' must not be blank",
  "path": "/api/v1/mail"
}
```
