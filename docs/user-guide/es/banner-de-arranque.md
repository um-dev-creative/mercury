# Guía de usuario: banner de arranque

🌐 [Read this in English](../en/startup-banner.md)

Al arrancar, las primeras líneas del log de Mercury muestran un banner. Indica qué build se está ejecutando sin abrir el código.

```
<arte ASCII "Mercury">
mercury::<versión>
Powered by Spring Boot <versión> | Spring Cloud <versión> | UMDC libraries <versión>
```

| Parte | Significado |
|---|---|
| `mercury::<versión>` | Nombre y versión de la aplicación |
| Spring Boot | Versión del framework |
| Spring Cloud | Versión del conjunto de librerías que aporta Vault y Config Server |
| UMDC libraries | Versión de las librerías privadas de UMDC (`commons`, `commons-services`, `security-oauth`, `persistence`) |

Tiene el mismo formato que el de backbone-rest, así que al depurar una integración puedes comparar las versiones de ambos servicios de un vistazo.

## Para qué sirve

- Confirmar que un despliegue ejecuta el build esperado.
- Comprobar que Mercury y backbone-rest usan la misma versión de las librerías UMDC.
- Adjuntar la línea a un reporte de error.

Si una versión aparece como `${...}` o `@...@`, el build está mal: repórtalo. Las versiones no se configuran en tiempo de ejecución.
