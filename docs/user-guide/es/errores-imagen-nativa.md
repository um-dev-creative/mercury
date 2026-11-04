# Guía de usuario: error HTTP 500 de jjwt en la imagen nativa

🌐 [Read this in English](../en/native-image-errors.md)

## Síntoma

Mercury corre como imagen nativa (el contenedor del ambiente QA) y una llamada con token de sesión, por ejemplo `GET /api/v1/campaigns/application/{applicationId}`, responde:

```json
{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred"}
```

y el log del contenedor muestra `Unable to instantiate class [io.jsonwebtoken.impl.DefaultJwtBuilder$Supplier]` y `NoSuchMethodException`.

## Causa y solución

La imagen nativa dejó fuera una clase que la librería de tokens (jjwt) carga por nombre. Está corregido en el código (`ThirdPartyNativeRuntimeHints`), así que **hay que construir de nuevo la imagen del contenedor y redesplegarla**: reiniciar la imagen anterior no sirve. La misma versión en una JVM normal nunca tuvo el problema.

## Después del redespliegue

Repite la llamada. Si el mismo error nombra otra clase de `io.jsonwebtoken`, repórtalo con la línea del log: la lista de clases registradas está en `docs/architecture/graalvm-native-image.md` (hueco 4).
