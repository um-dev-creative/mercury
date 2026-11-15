# Campaign progress

`GET /api/v1/campaigns/{id}/progress` shows how many recipients were sent, failed and are still pending.

- **E-mail and Telegram campaigns** advance as each message is sent: `totalSent` when it leaves, `failed` when sending fails.
- A recipient whose message could not even be queued counts as `failed`.
- **SMS, WhatsApp and Push** campaigns keep every recipient as `pending`: those channels have no provider connected yet.
- `delivered`, `opened` and `clicked` stay at 0: nothing reports them yet.
- Messages sent outside a campaign (verification codes, direct sends) never count.

E-mails are sent every minute, so the numbers move within about a minute of creating the campaign.
