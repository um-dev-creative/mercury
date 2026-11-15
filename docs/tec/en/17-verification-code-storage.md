# Verification code storage

A client application (ZeroBound, Smartkin, Directory) sends a **verification e-mail** through `POST /api/v1/messages` with the code in the
`vc` parameter (and `expiresInMinutes`). Mercury keeps that code in `mercury.verification_codes` so the application can later confirm it with
`POST /api/v1/verification-code`.

## Flow

```mermaid
sequenceDiagram
    participant App as Client application
    participant Ing as /api/v1/messages
    participant Mongo as MongoDB (OPENED)
    participant Proc as MessageProcessor
    participant SQL as PostgreSQL
    App->>Ing: e-mail, templateDefinedId, params {vc, expiresInMinutes}
    Ing->>Mongo: EmailMessageDocument OPENED
    Note over Proc: every 60 s
    Proc->>Mongo: read OPENED, send by SMTP
    Proc->>Mongo: status SENT
    alt template type is VERIFICATION_CODE
        Proc->>SQL: message record + verification_codes row (right now)
        Proc->>Mongo: delete the document
    else any other template
        Note over Proc: the 5 min reconciliation records it later
    end
    App->>SQL: POST /api/v1/verification-code (userId, applicationId, code)
```

## Rules

- **A template is a verification-code template** when its type id is `umdc.verification.code.template.id`
  (`PRX_VERIFICATION_CODE_TEMPLATE_ID`) **or** its type is named `VERIFICATION_CODE` (case, spaces and hyphens ignored). The name is accepted
  because a deployment whose variable did not match the type id in the database used to record the message, delete the document and store
  **no code**, without any log.
- **The code is stored as soon as the e-mail is sent**, in the same run that sends it. Before, it waited for the reconciliation
  (`umdc.scheduler.save-message-processed.fixed-rate`, 5 min by default), so a user who typed the code earlier was told there was none.
  Other e-mails are still reconciled by the periodic job.
- A verification message **without `vc`** logs an error and stores no code; the message is still recorded and removed.
- A message with **no `sendDate`** lives from now (`expiresInMinutes`, never more than two days).
- A new code expires the pending ones of the same user and application (resend).
- Logs: `Verification code stored for user <id> (message <id>)` (INFO).

## Tests

`MessageProcessorTest`: type by name, type by id, another type, missing `vc`, missing `sendDate`, immediate storage after sending, failed send, and an ordinary e-mail left for the reconciliation.
