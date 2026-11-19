# 15 · Template definitions (`template_defined`) are written on create

## Problem

`mercury.template_defined` is the table `POST /api/v1/messages` (`TemplateNameResolver`), campaigns (`CampaignServiceImpl`) and `GET /api/v1/messages/templates` read. Nothing wrote it: `TemplateServiceImpl.createTemplate` saved only `mercury.templates`, and `TemplateDefinedService` is read-only. Result: the table stayed empty and every send by template name was a 400 (`not registered for this application`).

## Design

```mermaid
sequenceDiagram
    participant C as Caller (Iris / API)
    participant S as TemplateServiceImpl
    participant R as TemplateDefinitionRegistrar
    participant DB as PostgreSQL
    C->>S: POST /api/v1/templates
    S->>DB: save templates (language derived by @PrePersist)
    S->>R: register(template, requesterId)
    R->>DB: find template_defined by template_id
    alt active row exists
        R-->>S: keep it (idempotent)
    else inactive row exists
        R->>DB: reactivate it
    else none
        R->>DB: find or create frequency_type ON_DEMAND
        R->>DB: insert template_defined (active)
    end
    C->>S: DELETE /api/v1/templates/{id}
    S->>R: deactivate(templateId)
    R->>DB: is_active = false
```

- `TemplateDefinitionRegistrar` (`api/v1/service`): `register` (idempotent, reactivates, never duplicates) and `deactivate`. The frequency is `umdc.template.default-frequency-type` (default `ON_DEMAND`), found by name or created if absent.
- `user_id` is the requester; `application_id` is the template's; `expired_at` stays `NULL`.
- Repositories: `TemplateDefinedRepository.findByTemplateId`, `FrequencyTypeRepository.findFirstByNameIgnoreCaseAndActiveTrue`.
- Migration `V2035__template_defined_backfill.sql`: seeds `ON_DEMAND` and gives every active template without a definition one (`user_id` = nil UUID, the creator was never recorded). Idempotent.
- `createTemplate` is `@Async` and not `@Transactional`: the template is saved first and the definition second; if the definition fails the request fails and a retry is safe (`register` is idempotent).

## Tests

`TemplateDefinitionRegistrarTest` (create, create frequency, idempotent, reactivate, deactivate) and the create/delete cases of `TemplateServiceImplTest`.

## Template language (fix)

A template is reachable by name only if `templates.language` is set (`TemplateNameResolver.usable()` drops the ones without it, so the catalogue came back empty).

```mermaid
flowchart TD
    R["POST/PUT /api/v1/templates<br/>language (optional) + location"] --> E{"language sent?"}
    E -- yes --> S["saved as sent"]
    E -- no --> D["TemplateEntity.deriveLanguage<br/>TemplateIdentity.languageOf(location)"]
    D --> F{"suffix _xx / -xx<br/>before the extension?"}
    F -- yes --> S
    F -- no --> N["language NULL: not sendable by name"]
```

- `TemplateIdentity.languageOf` accepts `_<2-3 letters[-region]>` and `-<2 letters[-region]>`; `nameOf` strips whichever separator was used (`sk-verification-code-es.ftl` -> name `sk-verification-code`, language `es`).
- `CreateTemplateRequest`, `UpdateTemplateRequest` and `TemplateDetailResponse` carry `language`; the old-arity constructors remain.
- Migration `V2042__template_language_backfill.sql` fills `language` of existing rows from `location`. Idempotent.
- Tests: `TemplateIdentityTest`, `TemplateMapperTest`, `TemplateUpdateApplierTest`.
