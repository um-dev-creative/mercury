# E-mail subject: defined in the template

The subject line of an e-mail is part of the **template**, so you change it from Iris, in the template editor, without asking
for a code change or a deployment.

## Set or change it
1. In Iris, open the template (each language is its own template: `…_es`, `…_en`).
2. Fill **Subject**. You can use the message data between `${ }`: `Your code is ${vc}`, `Welcome, ${name}`.
3. Save. The next e-mail sent with that template uses it.

To remove it, save the field empty.

## Rules
- Up to 500 characters, one line.
- A placeholder must be sent by the application. If the subject says `${name}` and the message has no `name`, the message is
  rejected with a clear error instead of going out with a broken subject.
- If the application also sends a subject of its own with the message, **that one wins**. Applications that follow this guide
  send none.
- A template with no subject and a message with none are rejected: `subject is required for email`.

## Before you rely on it
Fill the subject of **every language** you offer; each one is a separate template.
