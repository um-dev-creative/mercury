# Test recipients for QA

Two settings let a non-production environment send every e-mail to a fixed list of people, so flows can be tested end to end (verification
codes, campaigns, notifications) without writing to real users.

| Property (`umdc.mail.recipients.*`) | Environment variable | Default | Meaning |
|---|---|---|---|
| `defaults` | `MAIL_DEFAULT_RECIPIENTS` | empty | Comma separated list of addresses. |
| `honor-request` | `MAIL_HONOR_REQUEST_RECIPIENTS` | `true` | `true`: use the recipients of the request (form or payload). `false`: ignore them and use `defaults`. |
| `override-profiles` | `MAIL_RECIPIENT_OVERRIDE_PROFILES` | `qa,dev` | Spring profiles where `honor-request=false` is allowed. |

## Behaviour

```mermaid
flowchart TD
    A[E-mail about to be sent<br/>EmailServiceImpl.process] --> B{honor-request}
    B -->|true| C{Request names a recipient?}
    C -->|yes| D[to = request to, cc = request cc]
    C -->|no| E[to = defaults, cc = request cc]
    B -->|false| F[to = defaults, cc = none]
    D --> G[SMTP send]
    E --> G
    F --> G
```

- Applied in one place, `MailRecipientPolicy`, called by `EmailServiceImpl` right before the SMTP send, so it covers every path (`POST
  /api/v1/messages`, `/mail`, campaigns, verification codes).
- With `honor-request=false` the copies (`cc`) of the request are dropped as well, so nobody outside the list gets a copy.
- The message record, the verification code and the campaign metrics are unchanged: they keep the user the application asked for; only the
  SMTP recipients change. The log line `E-mail recipients replaced by the default list (N requested, M used)` shows counts, never addresses.

## Which environment is QA

Mercury does not guess: it reads the active Spring profile (`SPRING_BOOT_PROFILE_ACTIVE`). The override (`honor-request=false`) only starts in a
profile listed in `override-profiles` (default `qa`, `dev`; names are compared ignoring case).

```mermaid
flowchart TD
    S[Mercury starts with honor-request=false] --> P{Active profiles}
    P -->|contain prd, prod or production| X[Refuses to start]
    P -->|none of qa, dev| X
    P -->|qa or dev| L{Default list valid and not empty}
    L -->|no| X
    L -->|yes| OK[Starts, WARN: override ON in profile]
```

- A production profile (`prd`, `prod`, `production`) refuses the override even if it is listed next to `qa`, and listing one in
  `MAIL_RECIPIENT_OVERRIDE_PROFILES` refuses to start too: there is no setting that enables it in production.
- With `honor-request=true` (default) the profile is not checked and nothing changes.

## Safety

- `honor-request=false` with an empty `defaults` refuses to start Mercury (nobody would receive anything), and an invalid address in the list
  does too: the error names the setting.
- At start Mercury logs a `WARN` when the override is on, so it is visible in the logs of the environment.
- Production leaves both unset: behaviour is exactly what it was before.

Classes: `mail/MailRecipientProperties` (binding and validation), `mail/MailRecipientPolicy` (decision). Tests: `MailRecipientPolicyTest` (profile gate),
`MailRecipientPropertiesBindingTest`, `EmailServiceImplRecipientsTest` (inspects the real MIME message).
