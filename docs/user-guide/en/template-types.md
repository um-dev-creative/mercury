# Template types

Every template has a **type** that says what kind of message it is. The types Mercury ships with:

| Type | Use it for |
|---|---|
| Verification code | Messages with a code to confirm an e-mail (the code expires). Mercury only keeps and checks codes for this type. |
| Financial alert | Warnings: a budget is exceeded, a price reaches a threshold, an important insight. |
| Payment reminder | A reminder before a due date, for example a debt. |
| Goal achievement | A congratulation when someone reaches a goal. |
| Periodic summary | A recurring digest, for example the weekly summary. |
| Generic message | A plain message with no business meaning, to try a channel. |

When you create a template in Iris, pick the type that matches. If you pick **Verification code** the template must show the code (`${vc}`) and how long it lasts (`${expiresInMinutes}`).

An administrator adds the types to a database once with `scripts/templates/seed-template-types.sql` (see the technical guide, `docs/tec/en/13-template-types.md`). It can be run again safely: it never changes or duplicates a type that already exists.
