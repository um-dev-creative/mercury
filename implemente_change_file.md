# Campaign metadata update (2026-02-28T11:02:26.642Z)

## Implemented changes
- Campaign creation now persists the campaign record before publishing messages so Kafka payloads carry a valid `campaignId`.
- A structured metadata document is stored in `campaigns.metadata`, capturing channel configuration, template parameters, scheduling info, execution stats, and audit details.
- The REST contract now requires recipients explicitly, and campaign status flows from the request down to the persisted entity and metadata.

## Metadata schema
The JSON document stored in `campaigns.metadata` uses the following root keys:

| Key | Required fields | Description |
| --- | --------------- | ----------- |
| `channel` | `code`, `name`, `implementationClass`, `enabled` | Snapshot of the selected channel type at creation time. |
| `template` | `id`, `params` | Identifies the template and the parameter map supplied with the request. |
| `scheduling` | `scheduled` (boolean), `scheduledAt` (ISO-8601 or `null`) | Indicates whether execution is delayed and when it should start. |
| `execution` | `status`, `totalRecipients`, `batchSize`, `createdAt` | Tracks operational intent when the campaign was queued. |
| `audit` | `requestedBy` | Records the user responsible for the campaign submission. |

### Example payload
```json
{
  "channel": {
    "code": "email",
    "name": "Email",
    "implementationClass": "com.prx.mercury.channel.EmailChannel",
    "enabled": true
  },
  "template": {
    "id": "8e8f3d3a-9bc2-4e8f-8fb5-1a7c0d9ff48f",
    "params": {
      "subject": "Hello",
      "from": "no-reply@example.com"
    }
  },
  "scheduling": {
    "scheduled": true,
    "scheduledAt": "2026-03-01T10:00:00Z"
  },
  "execution": {
    "status": "SCHEDULED",
    "totalRecipients": 250,
    "batchSize": 100,
    "createdAt": "2026-02-28T11:02:26.642Z"
  },
  "audit": {
    "requestedBy": "0b040f33-7c8a-4aba-a134-ec343d17f6d1"
  }
}
```
