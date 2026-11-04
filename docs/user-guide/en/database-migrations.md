# User guide: database updates

🌐 [Leer esto en Español](../es/migraciones-de-base-de-datos.md)

Mercury now updates its own database tables when it starts. You no longer run the `V20xx__*.sql` scripts by hand.

## What you need to know

- On start, Mercury applies the scripts it has not applied yet, in order, and records them in the table `mercury.flyway_schema_history`.
- The **first** start on a database that already has tables records the existing state and runs the scripts from `V2026` again. This is harmless: they only add what is missing.
- If a script fails, or an applied script was changed, Mercury does not start and the log says which one.
- Mercury's database user must be allowed to create and alter tables in the `mercury` schema.
- The base tables must already exist: Mercury does not create them on an empty schema.

## Turning it off

Set `MERCURY_FLYWAY_ENABLED=false` and Mercury will not touch the tables. Do this where the database user has no permission to alter tables, and run the scripts yourself with an administrator account.

## Checking what was applied

```sql
SELECT version, description, success, installed_on
FROM mercury.flyway_schema_history ORDER BY installed_rank;
```

Technical details: [Flyway migrations](../../tec/en/09-flyway-migrations.md).
