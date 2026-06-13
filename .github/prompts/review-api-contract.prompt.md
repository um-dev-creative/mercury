---
name: Review API Contract
description: >
  Review a Mercury *Api interface change for OpenAPI correctness, HTTP semantics,
  status code completeness, and operationId consistency.
mode: ask
agent: api-reviewer
tools: [read_file, grep_search, codebase_search]
---

## Input Variables

- `${apiInterface}` — file path (e.g., `src/main/java/com/prx/mercury/api/v1/controller/CampaignApi.java`)
- `${changedMethods}` — list of method names that changed or were added

## Steps

1. **Read the interface file**: `${apiInterface}`
2. **For each changed method**, verify:
   - `@Operation` present with `summary`, `description`, `operationId`
   - `operationId` format: `{verb}{Resource}` camelCase (e.g., `toggleCampaign`)
   - `@ApiResponses` covers: 200/201, 400, 403, 404 (if `{id}` in path), 422 (if channel), 500
   - HTTP method matches semantic (POST=create, PUT=replace, PATCH=partial, DELETE=no body)
3. **Confirm no annotations exist on the controller class**:
   ```bash
   grep -n "@Operation\|@ApiResponse\|@Tag" src/main/java/com/prx/mercury/api/v1/controller/*Controller.java
   ```
   Must return empty.
4. **Check operationId uniqueness** across all `*Api.java` files.

## Output

```
API Contract Review — ${apiInterface}

Changed methods: ${changedMethods}

BLOCKING:
- [method:line] Issue

MINOR:
- [method:line] Suggestion

HTTP Semantics: PASS / ISSUES
Status Codes: PASS / MISSING [list]
operationId Format: PASS / ISSUES
Annotation Placement: PASS / FAIL
Overall: APPROVED / REQUEST_CHANGES
```
