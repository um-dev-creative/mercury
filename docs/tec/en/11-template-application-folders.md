# 11 · Template folders per application

⬅️ [Back to index](README.md)

Template content lives in a `TemplateStorage` (local folder or S3/R2 bucket). Every application has its own folder: `<root or s3 prefix>/<folder>/<file>`, for example `lth/mercury/templates/iris/iris_welcome.ftl`.

```mermaid
flowchart LR
  subgraph callers
    E[EmailServiceImpl] --> F
    C[ChannelTemplateRenderer] --> F
    P[TemplateContentServiceImpl<br/>read / save / restore / preview] --> F
  end
  F[ApplicationTemplateFolders.nameOf<br/>application id + location] -->|iris/iris_welcome.ftl| S[(TemplateStorage<br/>Local or S3/R2)]
  T[TelegramTemplateRenderer] -->|zerobound/zb_..._tg_es.ftl| S
  S --> FM[StorageTemplateLoader → FreeMarker]
```

- `TemplateEntity.location` is still the plain file name. `ApplicationTemplateFolders.nameOf(applicationId, location)` returns `folder/location`; a location that already has a `/` is left alone.
- Folder of an application: `umdc.templates.application-folders` (`<application id>=<folder>,...`, env `TEMPLATE_APPLICATION_FOLDERS`), else the lower-cased `general.application.code_name` (`ApplicationEntity.codeName`, read-only). An application without a valid folder falls back to the root and logs a WARN; the miss is not cached.
- `TemplateNames` accepts `file` or `folder/file` (one level; folder `[a-z0-9][a-z0-9_-]{0,63}`); `isFileName` is the bare form, which is what `location` must be to be editable. Protected prefixes (`zb_`, `sk_`, `iris_`, `ds_`) are checked on the file name, not on the folder.
- `LocalTemplateStorage` creates the folder on write (temporary file in the same folder, atomic move) and `list()` walks the root and one level of valid folders. `S3TemplateStorage.list()` no longer uses a delimiter, so it returns `folder/file` keys too; deeper keys are ignored.
- Preview renders under the qualified name, so a relative `<#include "header.ftl">` resolves inside the application folder, as it does when sending.
- Telegram (ZeroBound): `umdc.templates.telegram-folder` (env `TEMPLATE_TELEGRAM_FOLDER`, default `zerobound`).
- Cloudflare R2 is configured with `umdc.templates.s3.*`, fed by `CLOUDFLARE_R2_BUCKET_NAME`, `CLOUDFLARE_R2_ENDPOINT` (`https://<account>.r2.cloudflarestorage.com`), `CLOUDFLARE_R2_ACCESS_KEY` and `CLOUDFLARE_R2_SECRET_KEY`; `TEMPLATE_S3_PREFIX` (`lth/mercury/templates/`) stays the base path, with `TEMPLATE_S3_REGION=auto` and `TEMPLATE_S3_PATH_STYLE=true`.

Tests: `TemplateNamesTest`, `LocalTemplateStorageTest`, `S3TemplateStorageTest`, `ApplicationTemplateFoldersTest`, `TemplateContentServiceImplTest`, `ChannelTemplateRendererTest`, `TelegramTemplateRendererTest`.

User guide: [`template-storage`](../../user-guide/en/template-storage.md).
