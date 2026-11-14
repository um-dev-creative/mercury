# 🧩 Template Management API

> **Base path:** `/api/v1/templates` · **Auth:** `Authorization: Bearer` (service) + `session-token` header (end user) · **Status:** Implemented (MER-6)

### Modification Log
| Name              | Detail | Date |
|-------------------|--------|------|
| Luis Antonio Mata | Add Template Management API documentation | 2026-09-27 |

---

## 🎯 What this API manages

`TemplateEntity` (`mercury.templates`) is the **reusable base template**: description, metadata-only
`location`, `fileFormat`, classification (`templateType`, `severityType`), owning `application`, and a
soft-delete `active` flag.

This is **not** `TemplateDefinedEntity`. That table represents an application/user-specific *usage* of a
base template and is referenced by campaigns and message records — its lifecycle, and the campaign/message
flows that depend on it, are untouched by this API.

| Entity | Table | Owned by this API? | Referenced by |
|---|---|:---:|---|
| `TemplateEntity` | `mercury.templates` | ✅ | `TemplateDefinedEntity.template` |
| `TemplateDefinedEntity` | `mercury.template_defined` | ❌ (out of scope) | `CampaignEntity`, message records |

Deleting (deactivating) a `TemplateEntity` never touches `TemplateDefinedEntity` rows, campaigns, or message
history — see [Soft-delete & history retention](#-soft-delete--history-retention).

---

## 🔌 Endpoints

| Operation | Method & Path | Success | Errors |
|---|---|---:|---|
| Create | `POST /api/v1/templates` | `201 Created` + `Location` | `400`, `401`, `403`, `500` |
| Update | `PUT /api/v1/templates/{id}` | `200 OK` | `400`, `401`, `403`, `404`, `500` |
| Deactivate | `DELETE /api/v1/templates/{id}` | `204 No Content` | `400`, `401`, `403`, `404`, `500` |
| Get one | `GET /api/v1/templates/{id}` | `200 OK` | `400`, `401`, `403`, `404`, `500` |
| Search | `GET /api/v1/templates` | `200 OK` | `400`, `401`, `403`, `500` |
| Template types | `GET /api/v1/templates/types` | `200 OK` | `401` |
| Severity types | `GET /api/v1/templates/severities` | `200 OK` | `401` |
| Templates usable in a campaign | `GET /api/v1/templates/defined?applicationId=` | `200 OK` | `400`, `401`, `403` |
| Read the content | `GET /api/v1/templates/{id}/content` | `200 OK` + `ETag` | `401`, `403`, `404` |
| Save the content | `PUT /api/v1/templates/{id}/content` | `200 OK` + `ETag` | `401`, `403`, `404`, `412`, `413`, `422`, `428` |
| Preview | `POST /api/v1/templates/preview` | `200 OK` | `401`, `403`, `413` |
| List versions | `GET /api/v1/templates/{id}/versions` | `200 OK` | `401`, `403`, `404` |
| Read a version | `GET /api/v1/templates/{id}/versions/{version}` | `200 OK` | `401`, `403`, `404` |
| Restore a version | `POST /api/v1/templates/{id}/versions/{version}/restore` | `200 OK` + `ETag` | `403`, `404`, `412`, `422`, `428` |

OpenAPI annotations live on `TemplateApi` only (`src/main/java/com/umdc/mercury/api/v1/controller/TemplateApi.java`);
`TemplateController` is a thin implementation. The machine-readable contract is mirrored in
[`src/main/resources/api/openapi.yaml`](../src/main/resources/api/openapi.yaml).

### 🔐 Auth & application scope

Every operation requires the calling service's `Authorization: Bearer` token (scope `mercury:message:read` for
GET, `mercury:message:write` otherwise, checked by `ManagedClientSecurityConfig`) **and** a `session-token` header
(the end user's backbone-rest session JWT). Missing/invalid tokens are rejected at the MVC dispatch layer
(`SessionJwtInterceptor`) with `401` before a controller method ever runs.

Beyond authentication, every operation is authorized against the **real** `general.application_role_user` ACL — the shared, canonical
user↔application↔role relation that already lives in backbone-rest's persistence layer (`com.umdc:persistence`,
`ApplicationRoleUserEntity`). Mercury does **not** keep its own copy of that table; every authorization
decision is a live call to backbone-rest.

`TemplateServiceImpl` calls `ApplicationAuthorizationService.assertPermission(sessionToken, applicationId,
permission)` before every mutation and before every read, where `applicationId` is either the request's own
field (create, search) or the fetched template's owning application (update, delete, get — the entity is
loaded first so its real `applicationId` is known). That service is a thin wrapper around
`BackbonePermissionClient`, a Feign client (same outbound M2M authentication as the pre-existing, previously
unused `BackboneClient`) calling backbone-rest's **`POST /api/v1/iam/permissions/check`**.

```mermaid
sequenceDiagram
    actor Caller
    participant Mercury as TemplateServiceImpl
    participant Auth as ApplicationAuthorizationService
    participant Backbone as backbone-rest<br/>PermissionCheckApi
    participant ACL as general.application_role_user

    Caller->>Mercury: request (session-token, applicationId)
    Mercury->>Auth: assertPermission(sessionToken, applicationId, "TEMPLATE_MANAGE")
    Auth->>Backbone: POST /api/v1/iam/permissions/check
    Backbone->>Backbone: resolve uid from sessionToken
    Backbone->>ACL: findByUserAndApplication(uid, applicationId)
    ACL-->>Backbone: role (or none)
    Backbone-->>Auth: {granted, permission, reason}
    alt granted
        Auth-->>Mercury: (returns normally)
        Mercury-->>Caller: 2xx
    else denied, or backbone unreachable/erroring
        Auth-->>Mercury: throws ForbiddenException
        Mercury-->>Caller: 403
    end
```

**Trust model.** Mercury and backbone-rest sign/verify session-tokens with the *same* shared
`APP_TOKEN_SECRET` (both use the `security-oauth` library's `SessionJwtService`), so a token minted by either
service is valid to the other — Mercury forwards the `session-token` it received (the end user's backbone-issued token, sent
directly by the client) as-is in the request body.
Backbone-rest resolves the caller's `uid` from that token and looks up their *single* role for the requested
application; `permission` is granted if it matches that role's name or one of the role's active granted
features (case-insensitive).

**Fails closed.** If backbone-rest is unreachable, errors, or returns a non-granted result,
`ApplicationAuthorizationServiceImpl` throws `ForbiddenException` (`403`) — an identity-service outage is
never treated as an implicit grant.

> **⚙️ Operational requirement:** the permission string checked is `umdc.security.permissions.template-manage`
> (defaults to `TEMPLATE_MANAGE`, overridable via the `TEMPLATE_MANAGE_PERMISSION` env var). A role or feature
> named exactly that must be seeded in backbone-rest's `general.role`/`general.feature` tables and granted to
> the relevant users per application (`general.application_role_user`) — this is backbone-rest/ops data, not
> something this API can seed itself.

---

## 🗂️ Template folder of an application

`GET` and `PUT /api/v1/templates/applications/{applicationId}/folder` read and set the folder of the template storage an application keeps its templates in (`{"folder":"iris"}`; response `{applicationId, folder, registered}`). `GET` needs the template-read permission in the application, `PUT` template-manage (`400` invalid name, `409` the folder belongs to another application). It does not move files. The first template created for an application with no folder and no templates registers one named like the application id, so a new application needs no configuration. See [`docs/tec/en/11-template-application-folders.md`](tec/en/11-template-application-folders.md).

---

## 📝 Create

```
POST /api/v1/templates
session-token: <token>
```

```json
{
  "description": "Welcome email",
  "location": "s3://mercury-templates/welcome.html",
  "fileFormat": "HTML",
  "templateTypeId": "66666666-6666-6666-6666-666666666666",
  "applicationId": "33333333-3333-3333-3333-333333333333",
  "severityTypeId": "77777777-7777-7777-7777-777777777777",
  "subject": "Your code is ${vc}"
}
```

- `subject` is optional (≤500 chars): the e-mail subject of this version of the template — see [E-mail subject](#-e-mail-subject).
- `description`, `location` (≤500 chars), `fileFormat` (≤50 chars) are required and non-blank.
- `templateTypeId`, `applicationId`, `severityTypeId` are required UUIDs; `templateTypeId` and
  `severityTypeId` must reference **existing, active** rows or the request is rejected with `400`.
- The template is always persisted **active**; `id`, `createdAt`, `updatedAt` are server-generated.

**201 Created** — `Location: /api/v1/templates/{id}`, body is a `TemplateDetailResponse`.

```mermaid
sequenceDiagram
    actor Client
    participant API as TemplateApi / TemplateController
    participant Service as TemplateServiceImpl
    participant Auth as ApplicationAuthorizationService
    participant Backbone as backbone-rest
    participant Repo as TemplateRepository / TemplateTypeEntityRepository / SeverityTypeRepository
    participant DB as PostgreSQL

    Client->>API: POST /api/v1/templates (session-token, body)
    API->>Service: createTemplate(request, requesterId, sessionToken)
    Service->>Auth: assertPermission(sessionToken, applicationId, "TEMPLATE_MANAGE")
    Auth->>Backbone: POST /api/v1/iam/permissions/check
    Backbone-->>Auth: granted (else 403 ForbiddenException)
    Service->>Repo: applicationRepository.findById(applicationId)
    Service->>Repo: templateTypeRepository.findById(...) [must be active]
    Service->>Repo: severityTypeRepository.findById(...) [must be active]
    Repo-->>Service: resolved references (else 400 IllegalArgumentException)
    Service->>Repo: templateRepository.save(entity) [active=true]
    Repo->>DB: INSERT mercury.templates
    DB-->>Repo: persisted row
    Repo-->>Service: TemplateEntity
    Service-->>API: TemplateDetailResponse
    API-->>Client: 201 Created + Location
```

See [Authorization: the real `application_role_user` ACL](#-auth--application-scope) for the full
authorization sequence and trust model — the diagram above collapses it to a single `assertPermission` step.

---

## ✏️ Update

```
PUT /api/v1/templates/{id}
session-token: <token>
```

```json
{
  "description": "Updated welcome email",
  "fileFormat": "HTML"
}
```

- All fields are optional; only non-`null` values are applied. Omitted/blank text fields are left unchanged.
- `applicationId` **cannot** be sent — it is intentionally absent from `UpdateTemplateRequest` because it is
  immutable once a template is created.
- `templateTypeId` / `severityTypeId`, when provided, must reference an existing **active** row (`400`
  otherwise) and replace the current classification.
- `subject`: `null`/omitted leaves it unchanged, a blank string removes it, anything else replaces it (trimmed, ≤500 chars).
- `id` and `createdAt` are always preserved; `updatedAt` is refreshed only when at least one field actually
  changes (a request with no effective changes is a no-op — `200 OK`, no write).
- An unknown id, **or an id that is already inactive**, both return `404` — reactivating a deactivated
  template is out of scope (see [state diagram](#-lifecycle)).

**200 OK** — updated `TemplateDetailResponse`.

---

## ✉️ E-mail subject

The subject of an e-mail is part of the template (column `mercury.templates.subject`, migration `V2036`), one per language
version, edited from Iris with the rest of the template. No caller keeps it in its code.

- It may use the message parameters as FreeMarker placeholders: `Your code is ${vc}`. It is rendered with the same hardened
  engine as the bodies (`?new`/`?api` refused) and collapsed to a single line, so a parameter cannot inject a header.
- `POST /api/v1/messages` (email): a `subject` **in the request wins** (compatibility for callers that still send it);
  otherwise the template's subject is used; with neither the request is a `400`
  (`subject is required for email: send it or define it in the template`). A subject that uses a parameter the message did
  not send is a `400` too, answered immediately and nothing is queued.
- Not covered: `POST /api/v1/mail` and campaigns still take the subject in the request.

```mermaid
flowchart TD
    A[POST /api/v1/messages, channel email] --> B{subject in the request?}
    B -- yes --> F[use it]
    B -- no --> C{template defines a subject?}
    C -- no --> E[400 subject is required]
    C -- yes --> D[render with the message params]
    D -- invalid or missing param --> G[400 cannot be rendered]
    D -- ok --> F
    F --> H[queue EmailMessageDocument OPENED]
```

---

## 🗑️ Deactivate (soft-delete)

```
DELETE /api/v1/templates/{id}
session-token: <token>
```

- Sets `active=false` and refreshes `updatedAt`. The row itself, and every `TemplateDefinedEntity` /
  campaign / message record that references it, is left untouched — see
  [Soft-delete & history retention](#-soft-delete--history-retention).
- **Idempotent**: calling it again on an already-inactive template returns `204` without a further write.
- Unknown id → `404`.

**204 No Content**.

---

## 🔎 Get one

```
GET /api/v1/templates/{id}
session-token: <token>
```

Returns the template **only if it is active**. Both an unknown id and an inactive id return the same `404` —
callers cannot distinguish "never existed" from "was deactivated" through this endpoint.

**200 OK** — `TemplateDetailResponse`.

---

## 🔍 Search templates

```
GET /api/v1/templates?applicationId=...&q=welcome&templateTypeId=...&severityTypeId=...&active=true&page=0&size=20&sort=createdAt,desc
session-token: <token>
```

| Param | Required | Notes |
|---|:---:|---|
| `applicationId` | ✅ | Mandatory security/isolation scope. Missing → `400`. |
| `q` | – | Case-insensitive substring match against `description` **or** `location`. |
| `templateTypeId` | – | Exact match. |
| `severityTypeId` | – | Exact match. |
| `active` | – | Exact match. **Omitted defaults to active-only.** |
| `page` | – | Zero-based; default `0`; negative → `400`. |
| `size` | – | Default `20`; bounded `1–100`; out of range → `400`. |
| `sort` | – | `field,direction` (e.g. `createdAt,desc`). Allowed fields: `description`, `location`, `fileFormat`, `createdAt`, `updatedAt`. Unknown field/direction → `400`. A deterministic `id` ascending tie-breaker is **always** appended, so paging never reorders equal-sort-key rows. |

The `applicationId` scope, all filters, and paging/sorting are applied **in the database** via a
`Specification<TemplateEntity>` (`TemplateSpecifications`) + `Pageable` passed to
`TemplateRepository.findAll(spec, pageable)` — never filtered client-side after an unbounded fetch. A result
page never contains rows from another `applicationId`.

**200 OK** — `TemplateSearchResponse`:

```json
{
  "items": [ { "...": "TemplateDetailResponse" } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

> **Technical note (search attributes).** `TemplateSpecifications` builds the search with the JPA *attribute* names, which are the **field** names of `TemplateEntity` (`applicationEntity`, `templateTypeEntity`, `severityTypeEntity`), not the getter names. A wrong name only fails at runtime (`Could not resolve attribute ...`, HTTP 500), so `TemplateSpecificationsTest` checks that each name is a declared field.
>
> ```mermaid
> flowchart LR
>   Q[GET /api/v1/templates] --> S[TemplateSpecifications.search]
>   S -->|applicationEntity.id, templateTypeEntity.id, severityTypeEntity.id| E[(TemplateEntity)]
> ```

## 📋 Reference lists

Small read-only lists that fill the selectors of a client (Iris uses them in the template and campaign forms).

```mermaid
flowchart LR
    F["Iris form"] -->|"types, severities"| L1["Active rows, by name"]
    F -->|"defined?applicationId="| P{"template-read permission<br/>for the application"}
    P -->|"granted"| L2["Active template_defined rows<br/>with their base template"]
    P -->|"denied"| X["403"]
```

| Endpoint | Returns | Access |
|---|---|---|
| `GET /api/v1/templates/types` | `[{id, name, description}]` of the active template types | Bearer `mercury:message:read` + `session-token` |
| `GET /api/v1/templates/severities` | same, for the active severity types | same |
| `GET /api/v1/templates/defined?applicationId=` | `[{id, templateId, description, location, fileFormat, templateTypeName}]`; `id` is what a campaign takes as `templateId` | same, plus the permission `umdc.security.permissions.template-read` for the application |

`template-read` defaults to `TEMPLATE_MANAGE`; set `TEMPLATE_READ_PERMISSION` to a broader role or feature so campaign authors can pick templates without being able to edit them. The list is fail-closed like the rest: a denied or failed check is `403`.

---

## ✍️ Template content

The metadata above says *which* file a template is (`location`); these endpoints read and write the file itself, through `TemplateStorage` (see `docs/architecture/template-storage.md`). `GET` needs the Bearer scope `mercury:message:read`; `PUT` and `POST /preview` need `mercury:message:write`. All need the `session-token` and the template permission on the template's application.

```mermaid
sequenceDiagram
    actor Editor
    participant API as TemplateContentController
    participant S as TemplateContentServiceImpl
    participant FS as TemplateStorage
    Editor->>API: GET /templates/{id}/content
    API-->>Editor: content + ETag "abc"
    Editor->>API: POST /templates/preview (unsaved text, sample params)
    API-->>Editor: output or problems with line and column
    Editor->>API: PUT /templates/{id}/content, If-Match "abc"
    API->>S: save
    S->>S: size limit, system-template check, syntax check
    S->>FS: write(name, bytes, "abc")
    alt the file is still "abc"
        FS-->>Editor: 200 + new ETag
    else someone saved first
        FS-->>Editor: 412 Precondition Failed
    end
```

| Rule | Result |
|---|---|
| `location` is not a file name (for example `s3://…`) | `404`: the template has no editable file |
| File not written yet | `GET` answers `exists=false`, empty content; create it with `If-None-Match: *` |
| `PUT` without `If-Match` or `If-None-Match` | `428`: a save never overwrites blindly |
| `If-Match` no longer matches | `412` |
| FreeMarker cannot read the text | `422` with `problems[{line, column, message}]` (Markdown lines count from the author's first line) |
| Larger than `umdc.templates.max-content-bytes` (256 KB) | `413` |
| Name starts with a `umdc.templates.protected-prefixes` entry (`zb_`, `sk_`, `iris_`, `ds_`) | `system=true` in the `GET`; saving needs `?confirmSystem=true`, otherwise `403` |
| `.md` | Markdown source: FreeMarker fills in the parameters, then the result is read as Markdown and written as HTML. Raw HTML is escaped and `javascript:` links dropped |
| Anything else (`.ftl`, `.ftlh`, `.html`) | FreeMarker/HTML source |

The preview never stores or sends anything. It renders with the same restricted engine as production (`?new` and `?api` are refused, see `docs/security/template-security.md`), stops after `umdc.templates.preview.max-output-chars` characters or `timeout-ms`, and answers `200` with `ok=false` and the problems when the text does not parse or fails to render. `parameters` lists the names the template reads (a reading of the text, not an execution).

### Version history

Every save is recorded in `mercury.template_versions` (apply `V2032__template_versions.sql` by hand first). The first time a template is changed from the API, the content it had is kept as a `BASELINE` version, so the original of a system template can always be restored. A restore saves the old content as a **new** version; nothing is erased. Mercury keeps the latest `umdc.templates.max-versions` (50) per template.

```mermaid
flowchart LR
    S["PUT content"] --> B{"first change and a file already exists?"}
    B -->|"yes"| BL["record BASELINE (the original)"]
    B -->|"no"| W["write the file"]
    BL --> W
    W --> V["record SAVE version"]
    R["POST restore version N, If-Match"] --> W2["write version N's content"]
    W2 --> V2["record RESTORE version"]
```

| Endpoint | Returns |
|---|---|
| `GET /templates/{id}/versions` | `[{version, etag, sizeBytes, reason, createdBy, createdAt}]`, newest first, no content |
| `GET /templates/{id}/versions/{n}` | the same plus `content` |
| `POST /templates/{id}/versions/{n}/restore` | the content after the restore; needs `If-Match` (428 without it, 412 if it changed) and, for a system template, `?confirmSystem=true` |

### Configuration

| Property (environment variable) | Default | Use |
|---|---|---|
| `umdc.templates.max-content-bytes` (`TEMPLATE_MAX_CONTENT_BYTES`) | `262144` | Largest template accepted |
| `umdc.templates.protected-prefixes` (`TEMPLATE_PROTECTED_PREFIXES`) | `zb_,sk_,iris_,ds_` | System templates: saving one needs `confirmSystem=true` |
| `umdc.templates.max-versions` (`TEMPLATE_MAX_VERSIONS`) | `50` | Versions kept per template |
| `umdc.templates.preview.max-output-chars` (`TEMPLATE_PREVIEW_MAX_CHARS`) | `524288` | Longest preview |
| `umdc.templates.preview.timeout-ms` (`TEMPLATE_PREVIEW_TIMEOUT_MS`) | `3000` | Longest a preview may write for |

---

## 🚦 Errors

All errors use the existing `ApiError` envelope (`GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-09-27T12:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Template not found: 8b6a7e2e-...-000000000000",
  "path": "/api/v1/templates/8b6a7e2e-...-000000000000"
}
```

| Status | Cause |
|---|---|
| `400` | Bean-validation failure, malformed UUID, missing `applicationId` on search, out-of-bounds `page`/`size`, unsupported `sort` field/direction, or an unknown/inactive `applicationId`/`templateTypeId`/`severityTypeId` reference on create/update. |
| `401` | Missing/invalid `session-token` (handled upstream by `SessionJwtInterceptor`, before the controller runs). |
| `403` | backbone-rest denied `TEMPLATE_MANAGE` for the caller's `(uid, applicationId)` pair, or the permission check itself failed/backbone was unreachable (fails closed — see [Auth & application scope](#-auth--application-scope)). |
| `404` | Unknown template id, **or** a template id that exists but is inactive (`TemplateNotFoundException`, new in this feature). |
| `500` | Unexpected internal error. |

`TemplateNotFoundException` and the `MissingServletRequestParameterException → 400` mapping were added to
`GlobalExceptionHandler` alongside the existing `CampaignNotFoundException`/`ForbiddenException` handlers.

---

## ♻️ Soft-delete & history retention

- `DELETE` never removes a row — it flips `active` to `false`.
- `TemplateDefinedEntity.template` keeps pointing at the same `TemplateEntity` row after deactivation;
  campaigns and message records that went through a `TemplateDefinedEntity` referencing this template are
  completely unaffected.
- No cascading delete, no rewrite of campaign/message history — this task does not touch
  `TemplateDefinedEntity`, campaign APIs, or message content processing at all.
- Reactivation (`Inactive → Active`) is explicitly **out of scope**; there is no endpoint for it.

## 🔁 Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Active: POST /api/v1/templates (create)
    Active --> Active: PUT /{id} (update metadata)
    Active --> Inactive: DELETE /{id} (active=false)
    Inactive --> Inactive: repeated DELETE /{id} (204, idempotent)
    Inactive --> [*]: retained for TemplateDefinedEntity / campaign / message history
```

Note: `GET /{id}` and `PUT /{id}` both treat `Inactive` the same as "does not exist" (`404`) — there is no
authorized-inactive-read path in this version.

---

## 🗄️ `location` is metadata only

`location` is a free-text pointer (e.g. an S3 URI, a CMS path) describing *where* template content lives. This
API never uploads, downloads, renders, or validates the referenced content — only the pointer string itself
(`≤500` chars) is persisted and validated.

---

## 🧱 Layers touched

| Layer | File |
|---|---|
| API contract | `api/v1/controller/TemplateApi.java` |
| Controller | `api/v1/controller/TemplateController.java` |
| Service | `api/v1/service/TemplateService.java`, `TemplateServiceImpl.java` |
| Partial-update logic | `api/v1/service/TemplateUpdateApplier.java` |
| Search criteria (internal) | `api/v1/service/TemplateSearchCriteria.java` |
| DTOs | `api/v1/to/CreateTemplateRequest.java`, `UpdateTemplateRequest.java`, `TemplateDetailResponse.java`, `TemplateSearchResponse.java` |
| Exception | `api/v1/exception/TemplateNotFoundException.java` |
| Mapper | `mapper/TemplateMapper.java` (fixed the pre-existing hardcoded-`active`/missing-severity mapping bug; added `toEntity`/`toDetailResponse`) |
| Search predicate | `jpa/sql/repository/TemplateSpecifications.java` |
| New lookup repositories | `jpa/sql/repository/SeverityTypeRepository.java` (the application is never looked up: the `general` schema is backbone-rest's) |
| Authorization | `api/v1/service/ApplicationAuthorizationService.java`, `ApplicationAuthorizationServiceImpl.java` |
| Backbone client | `client/BackbonePermissionClient.java`, `client/to/PermissionCheckRequest.java`, `PermissionCheckResponse.java` |
| Reused, unchanged | `jpa/sql/entity/TemplateEntity.java`, `jpa/sql/repository/TemplateRepository.java`, `client/interceptor/BackendFeignClientInterceptor.java` (previously dead code, now exercised) |

No Hibernate DDL changes and **no SQL migration** were needed — every column this API reads or writes
(`description`, `location`, `file_format`, `template_type_id`, `application_id`, `severity_type_id`,
`created_at`, `updated_at`, `active`) already existed in `mercury.templates`.

## 📖 OpenAPI

The hand-maintained spec at [`src/main/resources/api/openapi.yaml`](../src/main/resources/api/openapi.yaml)
now includes all five `templates` paths and their request/response schemas
(`CreateTemplateRequest`, `UpdateTemplateRequest`, `TemplateDetailResponse`, `TemplateSearchResponse`);
[`docs/openapi-client/README.md`](openapi-client/README.md) lists the new `operationId → path` mappings for
client generation.
