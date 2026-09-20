# Issue: Soft-delete campaign by ID (DELETE /api/v1/campaigns/{id})

## Short description

Implement a soft-delete endpoint to mark a campaign as deleted instead of physically removing it from the database. Soft-deleted campaigns should be excluded from normal query results and retained for auditing and recovery.

## Business value

- Prevent accidental permanent deletion and enable audit and recovery workflows.
- Comply with retention and audit requirements while allowing removal from active UI lists and APIs.

## Acceptance criteria (measurable & testable)

1. Endpoint
   - Method: DELETE
   - Path: `/api/v1/campaigns/{id}`
   - operationId: `deleteCampaign`

2. Parameters
   - Path: `id` (UUID) required.
   - Header: `session-token` required for authentication; caller must be authorized (owner/admin role).

3. Response
   - 204 No Content on successful soft-delete.
   - 401 Unauthorized for missing/invalid token.
   - 403 Forbidden if caller lacks permission.
   - 404 Not Found if campaign not found or already soft-deleted.

4. Behavior
   - Soft-delete should mark the campaign as deleted (e.g., `deleted=true` and `deletedAt=timestamp`) or set `status=DELETED` as per chosen convention.
   - Soft-deleted campaigns must be excluded from list and get endpoints (unless a special `includeDeleted=true` query is added in future).
   - The operation should be idempotent: deleting an already-deleted campaign returns 404.

## OpenAPI fragment (to add to `src/main/resources/api/openapi.yaml`)

```yaml
  /api/v1/campaigns/{id}:
    delete:
      tags:
        - campaigns
      summary: Soft-delete campaign by ID
      description: Soft delete a campaign (mark as deleted; data retained for audits).
      operationId: deleteCampaign
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: session-token
          in: header
          required: true
          schema:
            type: string
      responses:
        '204':
          description: Campaign soft-deleted successfully.
        '401':
          description: Invalid or missing authentication token.
        '403':
          description: Caller lacks permission.
        '404':
          description: Campaign not found.
```

## DB / persistence notes

- Preferred approach: add columns `deleted` (boolean) and `deletedAt` (timestamp) to the campaign entity.
- Alternative approach: use a `status` enum with value `DELETED` (must ensure this doesn't conflict with semantics elsewhere).
- If adding columns, create a DB migration script and update JPA entity and queries to exclude `deleted=true` rows by default.

## Tests to add

- Unit tests for service: soft-delete sets `deleted`/`deletedAt` correctly; idempotency checks.
- Repository tests: ensure find methods exclude deleted items.
- Controller MockMvc tests: 204 for successful delete, 404 for missing, 403/401 for auth cases.
- Integration test: create -> delete -> get (404) -> verify DB row exists with `deleted=true`.

## Logging & auditing

- Audit the soft-delete action at INFO level with userId, campaignId and timestamp.
- Record previous state and marker that the record was soft-deleted.

## Reviewers / stakeholders

- API owner: @api-owner
- DB owner: @db-admin
- Security reviewer: @security
- QA: @qa

## Estimated effort

- 0.5–1 day for DB migration + entity mapping
- 0.5–1 day for controller/service/repo changes
- 0.5 day for tests and docs

