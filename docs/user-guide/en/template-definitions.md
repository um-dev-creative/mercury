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

## Sending by id without keeping ids

`GET /api/v1/messages/template-definitions?applicationId=<uuid>` returns, for your own application, each usable template as `{templateDefinedId, location, language}`.
Keep that list in memory and refresh it now and then: a template you create, edit or switch off in mercury is seen by the next refresh, so you never copy ids into your own database.
Send the id as `templateDefinedId` to `POST /api/v1/messages` (e-mail) or `POST /api/v1/telegram/messages`.

## Template language

To send a template by name, Mercury needs to know its language. When you create a template you can set `language` (`es`, `en`, `pt-BR`...). If you leave it out, Mercury reads it from the end of the file name: `welcome_es.ftl` or `welcome-es.ftl` both mean Spanish, and the name you send is `welcome`.
A template with no language is created but does not appear in `GET /api/v1/messages/templates` and cannot be sent by name. Templates that were created without one are fixed automatically when Mercury is updated.
