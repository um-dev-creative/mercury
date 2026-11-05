# Guía de usuario: `403 Caller lacks permission to manage this application` con `415` en el log

🌐 [Read this in English](../en/backbone-m2m-token.md)

## Síntoma

`GET /api/v1/templates` (y cualquier llamada que pregunte a Backbone si quien llama puede gestionar una aplicación) responde `403 Caller lacks permission to manage this application` y el log de Mercury muestra:

```text
Backbone permission check failed ... HttpClientErrorException$UnsupportedMediaType: 415 on POST request for ".../backbone/api/v1/managed-clients/introspect"
```

## Causa

Mercury pide primero a Backbone su propio token máquina a máquina (M2M) y luego llama a Backbone con él. Había dos fallos:

1. La petición se enviaba como **formulario** (`application/x-www-form-urlencoded`, estilo Keycloak). `POST /api/v1/managed-clients/token` de Backbone solo acepta **JSON**, por eso respondía `415`.
2. En QA, `AUTH_URI` + `AUTH_TOKEN_URI` apuntaba a `/api/v1/managed-clients/introspect`, que comprueba un token pero no lo emite.

## Qué cambió en Mercury

La petición ahora es JSON (`clientId`, `clientSecret`, `scopes`) y la respuesta se lee de `accessToken`, como la devuelve Backbone.

## Qué debes configurar en el entorno

| Variable | Valor |
|---|---|
| `AUTH_URI` | Gateway de Backbone, p. ej. `https://api.umdc-qa.tst/backbone` |
| `AUTH_TOKEN_URI` | `/api/v1/managed-clients/token` (**no** `/introspect`) |
| `BACKBONE_CLIENT_ID` / `BACKBONE_CLIENT_SECRET` | Un cliente gestionado **activo** de Backbone y su secreto |
| `BACKBONE_SCOPE` | Scopes de ese cliente, separados por espacios o comas. Backbone rechaza (`400 invalid_scope`) los que el cliente no tiene |

Reinicia Mercury tras cambiarlas (y, en imagen nativa, reconstruye y vuelve a desplegar).
