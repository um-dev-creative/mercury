# Mercury M2M token (`POST /api/v1/auth/token`)

> The alias/password login was removed. See `docs/user-guide/en/m2m-applications.md` (how to use it) and `docs/tec/en/16-m2m-token-from-backbone.md` (how it works).

An application exchanges the Backbone M2M token of its managed client for Mercury's own M2M token.

```bash
curl -X POST "https://<host>:8118/api/v1/auth/token" \
  -H "Authorization: Bearer <BACKBONE_M2M_TOKEN>"
```

```json
{ "token": "<MERCURY_M2M_TOKEN>" }
```

Send that token in the `session-token` header of `/api/v1/messages`, `/api/v1/templates` and the other backend endpoints. It carries the client, the application Backbone ties the client to and its Mercury scopes (`mercury:message:read`, `mercury:message:write`); it only works on that application.

| Status | Meaning |
|---|---|
| 200 | Token issued. |
| 401 | Missing, inactive, expired or revoked Backbone token. |
| 403 | No `mercury:*` scope, or Backbone does not tie the client to an application. |
