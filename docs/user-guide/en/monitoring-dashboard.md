# Monitoring dashboard

> **Status: in progress.** Phases 1 and 2 (the data behind the dashboard, the state of the services and the live feed) are delivered in Mercury: it
> records every delivery outcome, watches its services and answers the monitoring questions through its API. You will not see it on screen until the Iris views of phase 3 are built. Requirements and scope:
> `docs/tec/en/23-monitoring-dashboard-requirements.md`; how phase 1 works: `docs/tec/en/24-monitoring-api.md`; phase 2: `docs/tec/en/25-monitoring-health-and-stream.md`.

## Where you will find it

In **Iris**, on the home page (the dashboard you already see after logging in) and in a new Monitoring section. What you see today (campaign
totals, delivery rate, failed and pending messages, the state of Mercury and Backbone, and the latest campaigns) stays; the items below are
added to it. You see the data of your own application.

## What it is for

See, as it happens, whether your e-mails, SMS, Telegram, WhatsApp and Push messages and your verification codes are being sent, and know
quickly when a service fails or goes down.

## What you will be able to do

```mermaid
flowchart LR
    A[Open the dashboard] --> B[Read the live summary]
    B --> C{Something looks wrong?}
    C -->|a channel| D[Click the channel: every view filters to it]
    C -->|a service| E[Open service health: see the cause and the outage history]
    D --> F[Open the message table and find the failed messages]
    E --> F
    F --> G[Retry a failed message or export the list]
```

| You want to | You will use | Phase |
|---|---|---|
| Know how many messages went out, failed or are waiting | Summary cards and the success-rate ring | 1 |
| See the trend of the last hour, day, week or month | Time-series charts with a range selector | 1 |
| Find why messages fail and which are stuck | Failures view (top causes, old queued messages) | 1 |
| Follow verification codes | Codes view: generated, sent, confirmed, expired, failed | 1 |
| Find one message | Message table with filters (channel, status, application, template, date) | 1 |
| Know whether SMTP, Kafka, the databases, Backbone or Telegram are up | Service health with history and cause | 2 |
| See it update by itself | Live updates (no need to reload the page) | 2 |
| Use the charts and filters together | Interactive, linked views | 3 |
| Be warned, retry or export | Alerts, retry button, CSV/JSON export | 4 |

## Things to know

- **Linked views:** clicking a channel, a bar or a service filters everything else on the screen. Click it again to clear the filter.
- **Channels without a provider:** SMS, WhatsApp and Push messages are shown as **"queued, no provider"**, not as delivered, because no
  provider is connected yet.
- **Privacy:** recipients appear masked, and message bodies and verification codes are never shown.
- **Stale data:** if the live connection drops the dashboard shows a warning and keeps refreshing by itself.
- **Permissions:** viewing needs a dedicated monitoring permission. An application sees only its own data.

## What phase 1 already keeps

From the day phase 1 is deployed, Mercury keeps one record per delivery outcome (sent or failed) of e-mail and Telegram, including
verification codes, for 90 days. It starts empty: there is no history before that day. The recipient is stored masked (`j***@e***.com`), and
neither message bodies nor verification codes are kept.

- **E-mail and Telegram** report sent and failed. **SMS, WhatsApp and Push** have no provider yet, so they only ever show as **queued**.
- A failed message shows a coarse cause today (`SEND_FAILED` for e-mail, `DELIVERY_FAILED` for Telegram). A temporary SMTP problem is not a
  failure yet: the message stays queued and is retried, so it is not counted until it is sent or fails. The detailed causes come with the
  service-health view (phase 2).
- A message waiting longer than 15 minutes is reported as **stuck**.

## What phase 2 adds: are the services up?

Mercury now checks its own services (PostgreSQL, MongoDB, Kafka, the e-mail server, Backbone and Telegram) and keeps the history of their outages.

| State | What it means for you |
|---|---|
| **UP** | It works. |
| **DEGRADED** | It answers but something is wrong: a sending limit, a rate limit or a slow reply. Messages may be late. |
| **DOWN** | It does not answer or refuses us. Messages that need it wait. |
| **NOT CONFIGURED** | It is switched off in this environment (for example Kafka locally, or Telegram without a bot). It is **not** a failure. |
| **UNKNOWN** | Mercury has just started and has not checked it yet. |

- Each service shows how fast it answers, since when it has been in that state, and **why** in a fixed sentence, for example "The sending limit of
  the account was exceeded". Passwords, addresses and raw error texts are never shown.
- The e-mail server is also judged by the e-mails that really go out: if the provider refuses one, the state changes within a minute without waiting
  for the next check.
- The **history of outages** lists each one with when it began, when it ended (or "ongoing") and how long it lasted, for the last hour, day, week or month.
- The **live feed** sends the summary and the service states every 10 seconds, so the dashboard updates by itself. It reconnects on its own about every
  15 minutes; if your session expired it stops receiving data until you sign in again.
