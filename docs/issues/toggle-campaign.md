# Issue: Enable/Disable campaign by ID (PATCH /api/v1/campaigns/{id}/toggle)

## Short description

Add an endpoint to enable or disable (pause/resume) a campaign by its UUID. This supports operational control to temporarily stop or resume message dispatch.

## Business value

- Enables soft operational control to pause/resume campaigns without deleting or recreating them.
- Useful for emergency stops, campaign throttling, or temporary suspensions.

## Acceptance criteria (measurable & testable)

1. Endpoint
   - Method: PATCH
   - Path: `/api/v1/campaigns/{id}/toggle`
   - operationId: `toggleCampaign`

2. Parameters
   - Path: `id` (UUID) required.
   - Query: `enabled` boolean required — `true` to enable/resume, `false` to disable/pause.
   - Header: `session-token` required for authentication; caller must be authorized (owner or appropriate role).

3. Response
   - 204 No Content on successful toggle.
   - 400 Bad Request for invalid parameters.
   - 401 Unauthorized for missing/invalid token.
   - 403 Forbidden if caller lacks permission.
   - 404 Not Found if campaign not found or soft-deleted.
   - 422 Unprocessable Entity if the state change violates business rules (e.g., enabling without required config).

4. Behavior
   - Toggling to `enabled=false` should prevent further dispatch of messages (future sends); depending on implementation, it may or may not interrupt in-flight sends — this must be documented and implemented consistently.
   - Toggling to `enabled=true` resumes normal campaign processing.
   - Persist enabled/disabled state on the campaign entity (field: `enabled` or `active`).

## OpenAPI fragment (to add to `src/main/resources/api/openapi.yaml`)

```yaml
  /api/v1/campaigns/{id}/toggle:
    patch:
      tags:
        - campaigns
      summary: Enable or disable a campaign
      description: Enable or disable (pause/resume) a campaign by id.
      operationId: toggleCampaign
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: enabled
          in: query
          required: true
          schema:
            type: boolean
        - name: session-token
          in: header
          required: true
          schema:
            type: string
      responses:
        '204':
          description: Campaign toggled successfully.
        '400':
          description: Invalid parameters.
        '401':
          description: Invalid or missing authentication token.
        '403':
          description: Caller lacks permission.
        '404':
          description: Campaign not found.
        '422':
          description: Business rule violation.
```

## Example (curl)

```bash
# Disable campaign
curl -X PATCH "https://api.example.com/api/v1/campaigns/00000000-0000-0000-0000-000000000101/toggle?enabled=false" \
  -H "session-token: <token>"
# Expected: 204 No Content

# Enable campaign
curl -X PATCH "https://api.example.com/api/v1/campaigns/00000000-0000-0000-0000-000000000101/toggle?enabled=true" \
  -H "session-token: <token>"
# Expected: 204 No Content
```

## DB / persistence notes

- Ensure campaign entity has a boolean `enabled` or `active` property; if not present, add it and provide DB migration.
- Toggle must be persisted and used by downstream dispatch/scheduler logic to decide whether to send messages.

## Tests to add

- Unit tests for service toggle logic: valid toggles, invalid toggles (e.g., enabling with missing template), permission checks.
- Controller MockMvc tests for 204 response and error cases.
- Integration test that verifies toggling prevents or resumes message dispatch (if integration test environment can simulate dispatch).

## Logging & auditing

- Log the toggle action (INFO) with userId, campaignId, previous state, new state.
- Include audit metadata for later investigations.

## Reviewers / stakeholders

- API owner: @api-owner
- Service owner: @service-owner
- QA: @qa

## Estimated effort

- 0.5–1 day to add controller+OpenAPI contract and simple toggle persistence
- 0.5–1 day for service logic and tests
- 0.5 day for integration tests (if necessary)

