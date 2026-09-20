# Issue: Update campaign by ID (PUT /api/v1/campaigns/{id})

## Short description

Add an endpoint to update an existing campaign by its UUID. The endpoint should accept a partial or full update payload for mutable fields and return the updated `CampaignDetailResponse`.

## Business value

- Allow clients and operators to correct or adjust campaign details after creation (e.g., name, scheduled time, template parameters).
- Support operational workflows (rescheduling, editing recipients) without needing to create a new campaign.

## Acceptance criteria (measurable & testable)

1. Endpoint
   - Method: PUT
   - Path: `/api/v1/campaigns/{id}`
   - operationId: `updateCampaign`

2. Request
   - Body: `UpdateCampaignRequest` JSON (see schema below). The request must be validated.
   - Header: `session-token` required for authentication; caller must be authorized (owner or appropriate role).

3. Response
   - 200 OK: returns the updated `CampaignDetailResponse` JSON on success.
   - 400 Bad Request: validation errors (invalid UUID, invalid fields).
   - 401 Unauthorized: missing/invalid `session-token`.
   - 403 Forbidden: caller lacks permission to update this campaign.
   - 404 Not Found: campaign not found or already soft-deleted.
   - 422 Unprocessable Entity: business rule violation (e.g., change not allowed in this state).

4. Behavior
   - Only mutable fields should be updated (name, templateId, recipients, templateParams, scheduledAt, status, applicationId). Fields omitted remain unchanged.
   - Changes must be persisted and reflected in subsequent GET responses.
   - When updating recipients, ensure deduplication/validation rules are applied.

## OpenAPI fragment (to add to `src/main/resources/api/openapi.yaml`)

```yaml
  /api/v1/campaigns/{id}:
    put:
      tags:
        - campaigns
      summary: Update campaign by ID
      description: Update mutable fields of a campaign identified by UUID.
      operationId: updateCampaign
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
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateCampaignRequest'
      responses:
        '200':
          description: Campaign updated successfully.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CampaignDetailResponse'
        '400':
          description: Invalid request payload.
        '401':
          description: Invalid or missing authentication token.
        '403':
          description: Caller lacks permission.
        '404':
          description: Campaign not found.
        '422':
          description: Business rule violation.
```

## Suggested schema (add to `components/schemas`)

```yaml
  UpdateCampaignRequest:
    type: object
    properties:
      name:
        type: string
      templateId:
        type: string
        format: uuid
      recipients:
        type: array
        items:
          $ref: '#/components/schemas/RecipientTO'
      templateParams:
        type: object
        additionalProperties: true
      scheduledAt:
        type: string
        format: date-time
      status:
        type: string
      applicationId:
        type: string
        format: uuid
    example:
      name: "Updated Campaign Name"
      scheduledAt: "2026-03-20T13:00:00Z"
      status: "SCHEDULED"
```

## Example (curl)

```bash
curl -X PUT "https://api.example.com/api/v1/campaigns/00000000-0000-0000-0000-000000000101" \
  -H "Content-Type: application/json" \
  -H "session-token: <token>" \
  -d '{
    "name": "Updated Campaign Name",
    "scheduledAt": "2026-03-20T13:00:00Z",
    "status": "SCHEDULED"
  }'
# Expected: 200 OK with CampaignDetailResponse JSON
```

## DB / persistence notes

- No change required if current model supports mutable fields. If not, migrate JPA entity mapping accordingly.
- Ensure repository queries exclude soft-deleted campaigns by default.

## Tests to add

- Unit tests for service: partial update, full update, invalid updates (e.g., unknown template id), permission checks.
- Controller tests (MockMvc) for happy path and error responses.
- Integration test: create -> update -> get cycle to verify persistence.

## Logging & auditing

- Log update operations (INFO) with userId, campaignId, changed fields.
- Record previous and new values in audit logs where appropriate.

## Reviewers / stakeholders

- API owner: @api-owner
- Service owner: @service-owner
- QA: @qa

## Estimated effort

- 1–2 days for API + controller + contract updates
- 1–2 days for service/repository + DB migration (if required)
- 0.5–1 day for tests and docs

