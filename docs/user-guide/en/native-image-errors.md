# User guide: HTTP 500 with a jjwt error in the native image

🌐 [Leer esto en Español](../es/errores-imagen-nativa.md)

## Symptom

Mercury runs as a native image (the container of the QA environment) and a call that carries a session token, for example `GET /api/v1/campaigns/application/{applicationId}`, answers:

```json
{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred"}
```

and the container log shows `Unable to instantiate class [io.jsonwebtoken.impl.DefaultJwtBuilder$Supplier]` and `NoSuchMethodException`.

## Cause and fix

The native image left out a class that the token library (jjwt) loads by name. It is fixed in the code (`ThirdPartyNativeRuntimeHints`), so **the container image must be built again and redeployed**: restarting the old image does not help. The same version on a normal JVM never had the problem.

## After the redeploy

Repeat the call. If the same error names another `io.jsonwebtoken` class, report it with the log line: the list of registered classes is in `docs/architecture/graalvm-native-image.md` (gap 4).
