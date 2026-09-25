# 📚 Technical Documentation — Mercury

🌐 [Leer esto en Español](../es/README.md)

> **Mercury** is a multi-channel messaging microservice — built with **Spring Boot 4.1.0 + Java 25 (LTS)**, it exposes a REST API, queues per-channel messages (Email/SMS/Telegram/WhatsApp/Push) via **Kafka**, delivers emails over SMTP with FreeMarker templates, and persists across **PostgreSQL** (durable) and **MongoDB** (transient), with configuration and secrets centralized in **Spring Cloud Config + HashiCorp Vault**.
>
> This folder documents how it's built under the hood: for anyone joining development, auditing an architecture decision, or trying to understand why something works the way it does.

---

## Documentation map

| # | Document | Content |
|:-:|---|---|
| 🧱 | [**01 · Technology Stack**](01-technology-stack.md) | Every dependency with its exact version (`pom.xml`), and why it's there |
| 🏛️ | [**02 · Architecture**](02-architecture.md) | The system's layers, the per-channel message flow, API security |
| 🗄️ | [**03 · Data Model**](03-data-model.md) | Full ER diagram of PostgreSQL's 14 tables + MongoDB's polymorphic document hierarchy |
| 📦 | [**04 · Component Catalog**](04-components.md) | Package map, the 15 real REST endpoints, each messaging channel's real status, and verified *known gaps* |
| 🔁 | [**05 · Sequence Diagrams**](05-sequence-diagrams.md) | The most representative flows: startup (Vault → Config Server), campaign creation, email lifecycle, session validation |
| ☁️ | [**06 · Environment Configuration**](06-environment-configuration.md) | Local development, test/QA (Docker), production — certificates, secrets, property precedence, real troubleshooting |
| 🚀 | [**07 · Development Guide**](07-development-guide.md) | Setup, build commands, branch strategy, how to add a new channel, PR checklist |

---

## Where to start

- **Never seen the project before?** Start with [Technology Stack](01-technology-stack.md) and follow with [Architecture](02-architecture.md) — 10 minutes and you'll have the full map.
- **Setting up your local environment?** [06 · Environment Configuration](06-environment-configuration.md) has the full step-by-step, including the real errors already debugged.
- **About to touch a specific flow?** Look up your operation in [Sequence Diagrams](05-sequence-diagrams.md) before touching code.
- **Need to know what endpoint/service exists for something?** [Component Catalog](04-components.md) is the quick reference.
- **Adding or changing a table/collection?** [Data Model](03-data-model.md) has the full schema.
- **Opening your first PR?** [Development Guide](07-development-guide.md) has the checklist and branch conventions.
- **Something "won't override" or fails with a certificate error?** [06 · Environment Configuration § Troubleshooting](06-environment-configuration.md#-real-troubleshooting) — it's almost certainly an already-documented case.

---

## Conventions used in this documentation

- All diagrams are in **[Mermaid](https://mermaid.js.org/)** — natively rendered by GitHub, GitLab, VS Code (with the "Markdown Preview Mermaid Support" extension), and most modern Markdown viewers.
- Examples cite **real paths and names** from the repository — everything shown here can be read directly from the source file.
- `[!NOTE]` / `[!TIP]` / `[!IMPORTANT]` / `[!WARNING]` / `[!CAUTION]` boxes use GitHub-flavored Markdown *alert* syntax.
- The *Known Gaps* section ([04](04-components.md#-known-gaps)) and the *Troubleshooting* section ([06](06-environment-configuration.md#-real-troubleshooting)) document **verified facts**, not assumptions — many came from real incidents debugged in this repository.

---

## 🕓 Version history of this document

| Version | Date | Changes |
|---|---|---|
| 1.0 | 2026-09-23 | Initial version — generated from an exhaustive read of the real source code and the configuration/deployment incidents debugged on the `fix/docker-image-and-compose` branch |

---

*Generated from an exhaustive read of the real source code — entities, endpoints, messaging channels, bootstrap configuration — and from real runs verified against a live Vault, Config Server, Docker, and PostgreSQL, not from prior documentation or assumptions.*
