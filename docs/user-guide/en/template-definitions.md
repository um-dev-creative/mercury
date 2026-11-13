# Templates are usable as soon as you create them

When you create a template (`POST /api/v1/templates`, or the Templates section in Iris), Mercury now also registers it so that it can be sent. Before this, a template created through the API could not be used: every `POST /api/v1/messages` call that named it failed with `The template '...' is not registered for this application`.

## What you do

1. Create the template with `POST /api/v1/templates` (`location`, `fileFormat`, `templateTypeId`, `severityTypeId`, `applicationId`, `description`).
   Name the file with its language as a suffix, for example `welcome_es.ftl`: the language is `es` and the name callers use is `welcome`.
2. Check what the application offers: `GET /api/v1/messages/templates?applicationId=<uuid>`. It lists each name with its languages.
3. Send with that name:

```json
{
  "channel": "email",
  "userId": "05cbfc91-6440-0408-7ba4-74a102026faf",
  "target": "someone@example.com",
  "template": "welcome",
  "applicationId": "81398159-1785-46a5-15ae-f491fd1dc509",
  "language": "es",
  "from": "no-reply@umdc.com",
  "subject": "Welcome"
}
```

`template` is the **name** (never an id). Use either `template` + `applicationId` + `language`, or `templateDefinedId`, not both.

## Good to know

- Deleting a template also switches its definition off, so it can no longer be sent.
- A template with no language (no `_es` suffix in `location`) is created, but it cannot be found by name; rename the file or set its language.
- Templates that already existed are fixed by migration `V2035` when Mercury starts: nothing to run by hand.
- Every definition uses the `ON_DEMAND` frequency (sent when asked for). Change the name with `umdc.template.default-frequency-type` if your catalogue uses another.
