# Verification code: the whole cycle

## Why `mercury.verification_codes` exists

ZeroBound creates the account in Backbone **inactive** and generates a 6-digit code. It keeps **no copy** of it. Mercury is the one that remembers the code
(table `verification_codes`) so that, when the user types it, someone can answer "is this the code that was sent to this user?". The table holds, per
code: the user, the application, the code, when it expires (`expiresInMinutes` of the e-mail, at most two days), `attempts` / `max_attempts` (3),
`is_verified` and `verified_at`, and the `message_record_id` of the e-mail that carried it. It is read and written only by Mercury
(`POST /api/v1/verification-code`, `GET /api/v1/verification-code/latest-status`).
**Without a row there, every code the user types is answered `404` ("no code") and the account is never activated.**

## Sequence

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant Z as ZeroBound (BFF + zerobound-rest)
    participant B as Backbone
    participant M as Mercury API
    participant MG as MongoDB (messages)
    participant S as MessageProcessor (every 60 s)
    participant SM as SMTP
    participant PG as PostgreSQL (mercury)

    U->>Z: POST /api/v1/auth/register
    Z->>B: create user (inactive)
    Z->>Z: generate 6-digit code (kept nowhere)
    Z->>M: POST /api/v1/messages (templateDefinedId, params vc, expiresInMinutes, Idempotency-Key)
    M->>MG: EmailMessageDocument OPENED
    M-->>Z: 202 Queued (id)
    Z-->>U: PENDING_VERIFICATION (confirmation page)

    S->>MG: read OPENED (e-mails only)
    S->>SM: send (subject and body from the template)
    SM-->>U: e-mail with the code
    S->>MG: status SENT
    alt SMTP fails
        S-->>MG: stays OPENED, retried next run
    end
    S->>PG: message_record (id = message id)
    S->>PG: verification_codes (code, expiry, 3 attempts)
    S->>MG: delete the document
    alt PostgreSQL refuses the row
        S-->>MG: stays SENT, error logged, retried by the 5 min reconciliation
    end

    U->>Z: POST /api/v1/auth/verify-email (alias, code)
    Z->>B: find the user by alias
    Z->>M: POST /api/v1/verification-code (userId, applicationId, code)
    M->>PG: read the pending codes of the user
    alt right code
        M->>PG: is_verified = true
        M-->>Z: 202
        Z->>B: activate the user
        Z-->>U: ACTIVE (can log in)
    else wrong code
        M->>PG: attempts + 1
        M-->>Z: 406 (Attempts-Remaining)
        Z-->>U: wrong code, N attempts left
    else no code / expired / attempts used
        M-->>Z: 404
        Z-->>U: "this code no longer works, ask for a new one"
    end

    U->>Z: POST /api/v1/auth/resend-verification
    Note over Z,PG: same flow from step 4, and the new code expires the pending ones of the user
```

## Where it can stop (and the log that tells)

| Step | Symptom | Log / check |
|---|---|---|
| ZeroBound -> Mercury | no message accepted | zerobound: `mercury did not take the message` |
| Mercury sends | e-mail never arrives, stays `OPENED` | `SMTP failure sending message ..., left OPENED to retry` |
| Mercury records | e-mail arrives, **table empty**, document stays `SENT` in MongoDB | `Could not record message ... (it stays SENT in MongoDB and is retried): <database error>` |
| Mercury records | row stored | `Verification code stored for user ...` |
| Confirm | `404` | no row, expired, or 3 wrong attempts |

## The database check that blocked the rows

`mercury.verification_codes` has the check `chk_verification_code_length`, which is not defined in this repository and refused the 6-digit code
(`new row for relation "verification_codes" violates check constraint "chk_verification_code_length"`). `V2038__verification_code_length.sql`
replaces it with `char_length(verification_code) BETWEEN 4 AND 9` (the column is `varchar(9)`), `NOT VALID` so existing rows are not re-checked.
Each message is now recorded in **its own transaction**: a row the database refuses is logged with its message id and retried, and it no longer
undoes or blocks the others (before, the whole run rolled back at commit). The `modified_by` column received a timestamp (`modifiedBy` was mapped from
`modifiedAt`); it now comes from `updatedBy`.
