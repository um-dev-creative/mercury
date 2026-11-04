# User guide: startup banner

🌐 [Leer esto en Español](../es/banner-de-arranque.md)

When Mercury starts, the first lines of its log show a banner. It tells you which build is running without opening the code.

```
<ASCII art "Mercury">
mercury::<version>
Powered by Spring Boot <version> | Spring Cloud <version> | UMDC libraries <version>
```

| Part | Meaning |
|---|---|
| `mercury::<version>` | Application name and version of the build |
| Spring Boot | Framework version in use |
| Spring Cloud | Version of the release train that provides Vault and Config Server |
| UMDC libraries | Version of the private UMDC libraries (`commons`, `commons-services`, `security-oauth`, `persistence`) |

The banner uses the same format as backbone-rest, so you can compare the versions of both services at a glance when you debug an integration.

## When it helps

- Confirming that a deployment really runs the build you expect.
- Checking that Mercury and backbone-rest use the same UMDC libraries version.
- Attaching the line to a bug report.

If a version shows `n/a`, the application was started from an IDE build without Maven; run `mvn compile` (or start it with Maven) to see the real versions. If a version shows as `${...}` or as `@...@`, the build is broken: report it. The versions are not configurable at runtime.
