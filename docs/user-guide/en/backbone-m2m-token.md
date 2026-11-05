# User guide: `403 Caller lacks permission to manage this application` with `415` in the log

🌐 [Leer esto en Español](../es/token-m2m-backbone.md)

## Symptom

`GET /api/v1/templates` (and any call that asks Backbone whether the caller may manage an application) answers `403 Caller lacks permission to manage this application`, and Mercury's log shows:

```text
Backbone permission check failed ... HttpClientErrorException$UnsupportedMediaType: 415 on POST request for ".../backbone/api/v1/managed-clients/introspect"
```

## Cause

Mercury first asks Backbone for its own machine-to-machine (M2M) token and then calls Backbone with it. Two things were wrong:

1. The request was sent as a **form** (`application/x-www-form-urlencoded`, the Keycloak style). Backbone's `POST /api/v1/managed-clients/token` only accepts **JSON**, so it answered `415`.
2. In QA, `AUTH_URI` + `AUTH_TOKEN_URI` pointed at `/api/v1/managed-clients/introspect`, which checks a token and does not issue one.

## What changed in Mercury

The request is now JSON (`clientId`, `clientSecret`, `scopes`) and the answer is read from `accessToken`, as Backbone returns it.

## What you must set in the environment

| Variable | Value |
|---|---|
| `BACKBONE_BASE_URL` | Backbone gateway, e.g. `https://api.umdc-qa.tst/backbone` (already used by the other Backbone calls). The token endpoint `/api/v1/managed-clients/token` is fixed, as in ZeroBound; `AUTH_URI` / `AUTH_TOKEN_URI` are no longer used for it |
| `BACKBONE_CLIENT_ID` / `BACKBONE_CLIENT_SECRET` | An **active** managed client of Backbone and its secret |
| `BACKBONE_SCOPE` | Scopes of that client, separated by spaces or commas. Backbone refuses (`400 invalid_scope`) scopes the client does not have |

Restart (and, for the native image, rebuild and redeploy) Mercury after changing them.
