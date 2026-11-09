# User guide: where the templates live

🌐 [Leer esto en Español](../es/almacen-de-plantillas.md)

The text of every message Mercury sends (the verification e-mail, a campaign message) comes from a template file. Mercury reads them from the storage set with `TEMPLATE_STORAGE`: a bucket (`s3`/`r2`/`cloud`) or a local folder (`local` uses `TEMPLATE_PATH`, by default `/usr/local/runme/templates`; any other value is the folder's path). In Docker, mount a writable volume over the folder for local storage.

## What you need to know

- A template is a single file named with letters, digits, `_`, `-` and `.`, ending in `.ftl`, `.ftlh`, `.md` or `.html`. It cannot be in a sub-folder.
- You can add or change a file in the folder and Mercury uses it within about 5 seconds, without restarting.
- The name you give a template in Mercury (its `location`) is that file name.
- **The folder must be writable by Mercury** (with `TEMPLATE_STORAGE=local`): saving a template edited or created from Iris writes the file, and creates the application's sub-folder, there. A read-only mount (for example a Docker volume with `:ro`) makes every save answer `500` with `Read-only file system`; mount the folder read-write, give the container's user write permission on it, or keep the templates in a bucket instead.
- If the folder does not exist when Mercury starts, Mercury stops with an error that names the folder. Create it or fix `TEMPLATE_PATH`.
- A template that is not found, or has a name that is not allowed, is skipped and the message falls back to the plain text of the campaign.

## One folder per application

Each application keeps its templates in its own folder: `<TEMPLATE_PATH or TEMPLATE_S3_PREFIX>/<folder>/<file>`. For example, with `TEMPLATE_S3_PREFIX=lth/mercury/templates/` the Iris templates are `lth/mercury/templates/iris/iris_welcome.ftl`, and the others live in `smartkind/`, `zerobound/` and `latinhub/`.

- The `location` of a template stays the plain file name (`iris_welcome.ftl`). Mercury adds the folder of the template's application, both when it sends a message and when someone edits the template from Iris, so both use the same file.
- Every application has its own folder, and it is managed by Mercury itself: when you create the **first template of a new application**, Mercury creates its folder (named like the application id) and uses it from then on. Nothing has to be configured when a new application appears.
- To give an application a friendlier folder name, set it with `PUT /api/v1/templates/applications/<application id>/folder` and the body `{"folder": "my-app"}` (you need the template-manage permission in that application; `GET` on the same path shows the current one). A folder belongs to one application only. Changing it does **not** move the files: copy the templates to the new folder first.
- Applications that already keep their templates in a named folder are registered once with the script `scripts/templates/seed-application-folders.sql` (check the application ids in it first). There is no configuration variable for folders, and a new application needs nothing.
- The Telegram templates of ZeroBound (`zb_<kind>_tg_<language>.ftl`) are read from `TEMPLATE_TELEGRAM_FOLDER` (default `zerobound`).
- A folder name has lower-case letters, digits, `_` and `-`, up to 64 characters. There is one level of folders only.
- An application that has no folder yet (no templates created through Mercury since folders exist) keeps working with its files at the root of the storage.
- Cloudflare R2: `TEMPLATE_STORAGE=s3` plus `CLOUDFLARE_R2_BUCKET_NAME` (bucket), `CLOUDFLARE_R2_ENDPOINT` (`https://<account id>.r2.cloudflarestorage.com`), `CLOUDFLARE_R2_ACCESS_KEY` and `CLOUDFLARE_R2_SECRET_KEY` (secrets, keep them in Vault), `TEMPLATE_S3_PREFIX=lth/mercury/templates/` (base path of the templates), `TEMPLATE_S3_REGION=auto` and `TEMPLATE_S3_PATH_STYLE=true`.

To see which folder an application uses, call `GET /api/v1/templates/applications/<application id>/folder`, or read the table `mercury.application_template_folders` (application id and folder).

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


## If templates are still saved in `/usr/local/runme/templates`

Check that the container really has `TEMPLATE_STORAGE=s3` and `CLOUDFLARE_R2_BUCKET_NAME`. Since this fix the variable is read when
Mercury starts, also in the native image (older images ignored it and always used the folder, which is why saves failed with
`Read-only file system`). If `TEMPLATE_STORAGE=s3` is set without the bucket, Mercury refuses to start and says so in the log.

> **Mercury ships no templates.** `src/main/resources/templates` no longer exists and the images do not contain templates. Where they live is decided by `TEMPLATE_STORAGE`: `s3` (or `r2`, `cloud`) = the Cloudflare R2 bucket; `local` = the folder `TEMPLATE_PATH`; any other value is the path of the local folder itself (created if it does not exist). Upload the template files to the chosen storage before starting Mercury.
