# Guía de usuario: actualización de la base de datos

🌐 [Read this in English](../en/database-migrations.md)

Mercury ahora actualiza solo las tablas de su base de datos al arrancar. Ya no tienes que ejecutar a mano los scripts `V20xx__*.sql`.

## Lo que necesitas saber

- Al arrancar, Mercury aplica en orden los scripts que aún no se han aplicado y los registra en la tabla `mercury.flyway_schema_history`.
- El **primer** arranque sobre una base que ya tiene tablas registra el estado existente y vuelve a ejecutar los scripts desde `V2026`. No hace daño: solo añaden lo que falta.
- Si un script falla, o se cambió uno ya aplicado, Mercury no arranca y el log indica cuál.
- El usuario de base de datos de Mercury debe poder crear y alterar tablas en el esquema `mercury`.
- Las tablas base ya deben existir: Mercury no las crea en un esquema vacío.

## Desactivarlo

Pon `MERCURY_FLYWAY_ENABLED=false` y Mercury no tocará las tablas. Hazlo donde el usuario de la base no tenga permiso para alterar tablas, y ejecuta tú los scripts con una cuenta administradora.

## Comprobar qué se aplicó

```sql
SELECT version, description, success, installed_on
FROM mercury.flyway_schema_history ORDER BY installed_rank;
```

Detalles técnicos: [Migraciones con Flyway](../../tec/es/09-migraciones-flyway.md).
