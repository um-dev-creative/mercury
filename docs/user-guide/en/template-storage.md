# User guide: where the templates live

🌐 [Leer esto en Español](../es/almacen-de-plantillas.md)

The text of every message Mercury sends (the verification e-mail, a campaign message) comes from a template file. Mercury looks for those files in one folder, set with `TEMPLATE_PATH` (by default `/usr/local/runme/templates`; in Docker you can mount your own folder over it with `MERCURY_TEMPLATES_HOST_PATH`).

## What you need to know

- A template is a single file named with letters, digits, `_`, `-` and `.`, ending in `.ftl`, `.ftlh`, `.md` or `.html`. It cannot be in a sub-folder.
- You can add or change a file in the folder and Mercury uses it within about 5 seconds, without restarting.
- The name you give a template in Mercury (its `location`) is that file name.
- If the folder does not exist when Mercury starts, Mercury stops with an error that names the folder. Create it or fix `TEMPLATE_PATH`.
- A template that is not found, or has a name that is not allowed, is skipped and the message falls back to the plain text of the campaign.

## Safe edits

Templates cannot create Java objects or call the Java API (see `docs/security/template-security.md`): `?new` and `?api` are refused.
