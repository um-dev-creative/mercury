# User guide: where the templates live

🌐 [Leer esto en Español](../es/almacen-de-plantillas.md)

The text of every message Mercury sends (the verification e-mail, a campaign message) comes from a template file. Mercury looks for those files in one folder, set with `TEMPLATE_PATH` (by default `/usr/local/runme/templates`; in Docker you can mount your own folder over it with `MERCURY_TEMPLATES_HOST_PATH`).

## What you need to know

- A template is a single file named with letters, digits, `_`, `-` and `.`, ending in `.ftl`, `.ftlh`, `.md` or `.html`. It cannot be in a sub-folder.
- You can add or change a file in the folder and Mercury uses it within about 5 seconds, without restarting.
- The name you give a template in Mercury (its `location`) is that file name.
- If the folder does not exist when Mercury starts, Mercury stops with an error that names the folder. Create it or fix `TEMPLATE_PATH`.
- A template that is not found, or has a name that is not allowed, is skipped and the message falls back to the plain text of the campaign.

## One folder per application

Each application keeps its templates in its own folder: `<TEMPLATE_PATH or TEMPLATE_S3_PREFIX>/<folder>/<file>`. For example, with `TEMPLATE_S3_PREFIX=lth/mercury/templates/` the Iris templates are `lth/mercury/templates/iris/iris_welcome.ftl`, and the others live in `smartkind/`, `zerobound/` and `latinhub/`.

- The `location` of a template stays the plain file name (`iris_welcome.ftl`). Mercury adds the folder of the template's application, both when it sends a message and when someone edits the template from Iris, so both use the same file.
- The folder of an application is set by its id with `TEMPLATE_APPLICATION_FOLDERS=<application id>=<folder>,...` (Mercury does not read the application code from the database, so only ids are accepted; anything else stops Mercury from starting, with a message). The default already lists the current applications (`iris`, `latinhub`, `lucky_le` for Lucky Leaf and Lucky Leaf REST, `smartkind` and `zerobound`), so nothing needs to be set unless the ids of your Backbone differ.
- The Telegram templates of ZeroBound (`zb_<kind>_tg_<language>.ftl`) are read from `TEMPLATE_TELEGRAM_FOLDER` (default `zerobound`).
- A folder name has lower-case letters, digits, `_` and `-`, up to 64 characters. There is one level of folders only.
- An application with no entry in `TEMPLATE_APPLICATION_FOLDERS` keeps working with files at the root, and Mercury logs a warning naming the application.
- Cloudflare R2: `TEMPLATE_STORAGE=s3` plus `CLOUDFLARE_R2_BUCKET_NAME` (bucket), `CLOUDFLARE_R2_ENDPOINT` (`https://<account id>.r2.cloudflarestorage.com`), `CLOUDFLARE_R2_ACCESS_KEY` and `CLOUDFLARE_R2_SECRET_KEY` (secrets, keep them in Vault), `TEMPLATE_S3_PREFIX=lth/mercury/templates/` (base path of the templates), `TEMPLATE_S3_REGION=auto` and `TEMPLATE_S3_PATH_STYLE=true`.

To check that each application finds its folder, compare the ids in `TEMPLATE_APPLICATION_FOLDERS` with the application ids shown by Backbone (the Applications screen of Stratum) and with the folders of the bucket.

## Using a bucket instead of a folder

If you run several Mercury instances, or you do not want the templates inside the server, keep them in a bucket of Amazon S3 or of an S3-compatible service (MinIO, for example): set `TEMPLATE_STORAGE=s3` and `CLOUDFLARE_R2_BUCKET_NAME` (and `CLOUDFLARE_R2_ENDPOINT` plus `TEMPLATE_S3_PATH_STYLE=true` for MinIO). Upload the template files to the bucket under `TEMPLATE_S3_PREFIX` before restarting Mercury. From then on, templates edited from Iris are written to the bucket and every instance uses them within a few seconds. The access key and secret are secrets: keep them in Vault, or leave them empty to use the server's AWS role.

## Writing a template

A template is either **FreeMarker/HTML** (`.ftl`, `.ftlh`, `.html`) or **Markdown** (`.md`).

- In both, `${name}` is replaced by the value of the parameter `name`; `<#if name??>…</#if>` shows a part only if it was given; `<#list items as item>…</#list>` repeats a part.
- A Markdown template is written as Markdown (`# Title`, `**bold**`, `- list`, `[text](https://…)`) and Mercury turns it into HTML when it sends the message. HTML written inside Markdown is shown as text, not run; use an `.html` template if you need HTML.
- A template cannot be saved if it has a mistake; the error says the line and column.
- Two people editing the same template: the second save is refused until they reload, so nobody loses the other's work.
- Every save is kept as a version, so you can go back. Restoring an old version saves it as a new one; nothing is erased (the latest 50 are kept).
- Templates that other systems depend on (names starting with `zb_`, `sk_`, `iris_`, `ds_`) need an explicit confirmation to save, and the original is kept so it can always be restored.

## Safe edits

Templates cannot create Java objects or call the Java API (see `docs/security/template-security.md`): `?new` and `?api` are refused.

## Listing templates

`GET /api/v1/templates` lists the templates of one application, optionally filtered by type, severity, active state or text. If it answers `500 An unexpected error occurred` with `Could not resolve attribute 'application'` in the log, the running image predates the fix; deploy the current version. Nothing needs to be configured.

