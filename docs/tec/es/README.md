# 📚 Documentación Técnica — Mercury

🌐 [Read this in English](../en/README.md)

> **Mercury** es un microservicio de mensajería multicanal — construido con **Spring Boot 4.1.0 + Java 25 (LTS)**, expone una API REST, encola mensajes por canal (Email/SMS/Telegram/WhatsApp/Push) vía **Kafka**, entrega emails por SMTP con plantillas FreeMarker, y persiste en **PostgreSQL** (durable) y **MongoDB** (transitorio), con configuración y secretos centralizados en **Spring Cloud Config + HashiCorp Vault**.
>
> Esta carpeta documenta cómo está construido por dentro: para quien necesite sumarse al desarrollo, auditar una decisión de arquitectura, o entender por qué algo funciona como funciona.

---

## Mapa de la documentación

| # | Documento | Contenido |
|:-:|---|---|
| 🧱 | [**01 · Stack Tecnológico**](01-stack-tecnologico.md) | Cada dependencia con su versión exacta (`pom.xml`), y por qué está ahí |
| 🏛️ | [**02 · Arquitectura**](02-arquitectura.md) | Las capas del sistema, el flujo de mensajes canal por canal, seguridad de la API |
| 🗄️ | [**03 · Modelo de Datos**](03-modelo-datos.md) | Diagrama ER completo de las 14 tablas de PostgreSQL + la jerarquía polimórfica de documentos MongoDB |
| 📦 | [**04 · Catálogo de Componentes**](04-componentes.md) | Mapa de paquetes, los 15 endpoints REST reales, el estado real de cada canal de mensajería, y los *known gaps* verificados |
| 🔁 | [**05 · Diagramas de Secuencia**](05-diagramas-secuencia.md) | Los flujos más representativos: arranque (Vault → Config Server), creación de campaña, ciclo de vida del email, validación de sesión |
| ☁️ | [**06 · Configuración de Entornos**](06-configuracion-entornos.md) | Desarrollo local, prueba/QA (Docker), producción — certificados, secretos, precedencia de propiedades, troubleshooting real |
| 🚀 | [**07 · Guía de Desarrollo**](07-guia-desarrollo.md) | Setup, comandos de build, estrategia de ramas, cómo agregar un canal nuevo, checklist de PR |

---

## Por dónde empezar

- **¿Nunca viste el proyecto?** Arrancá por [Stack Tecnológico](01-stack-tecnologico.md) y seguí con [Arquitectura](02-arquitectura.md) — en 10 minutos tenés el mapa completo.
- **¿Vas a configurar tu entorno local?** [06 · Configuración de Entornos](06-configuracion-entornos.md) tiene el paso a paso completo, incluyendo los errores reales que ya se depuraron.
- **¿Vas a tocar un flujo concreto?** Buscá tu operación en [Diagramas de Secuencia](05-diagramas-secuencia.md) antes de tocar código.
- **¿Necesitás saber qué endpoint/servicio existe para algo?** [Catálogo de Componentes](04-componentes.md) es la referencia rápida.
- **¿Vas a agregar o cambiar una tabla/colección?** [Modelo de Datos](03-modelo-datos.md) tiene el esquema completo.
- **¿Vas a abrir tu primer PR?** [Guía de Desarrollo](07-guia-desarrollo.md) tiene el checklist y las convenciones de rama.
- **¿Algo "no se overridea" o falla con un error de certificados?** [06 · Configuración de Entornos § Troubleshooting](06-configuracion-entornos.md#-troubleshooting-real) — es casi seguro un caso ya documentado.

---

## Convenciones de esta documentación

- Todos los diagramas están en **[Mermaid](https://mermaid.js.org/)** — se renderizan nativamente en GitHub, GitLab, VS Code (con la extensión "Markdown Preview Mermaid Support") y la mayoría de los visores de Markdown modernos.
- Los ejemplos citan **rutas y nombres reales** del repositorio — todo lo que aparece acá se puede ir a leer directamente al archivo fuente.
- Los recuadros `[!NOTE]` / `[!TIP]` / `[!IMPORTANT]` / `[!WARNING]` / `[!CAUTION]` usan la sintaxis de *alerts* de GitHub-flavored Markdown.
- La sección de *Known Gaps* ([04](04-componentes.md#-known-gaps)) y la de *Troubleshooting* ([06](06-configuracion-entornos.md#-troubleshooting-real)) documentan **hechos verificados**, no suposiciones — muchos surgieron de incidentes reales depurados en este repositorio.

---

## 🕓 Historial de versiones de este documento

| Versión | Fecha | Cambios |
|---|---|---|
| 1.0 | 2026-09-23 | Versión inicial — generada a partir de una lectura exhaustiva del código fuente real y de los incidentes de configuración/despliegue depurados en la rama `fix/docker-image-and-compose` |

---

*Generado a partir de una lectura exhaustiva del código fuente real — entidades, endpoints, canales de mensajería, configuración de arranque — y de ejecuciones reales verificadas contra Vault, Config Server, Docker y PostgreSQL en vivo, no de documentación previa ni de supuestos.*
