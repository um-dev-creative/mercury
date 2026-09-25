# 🧱 01 · Stack Tecnológico

🌐 [Read this in English](../en/01-technology-stack.md) · ⬅️ [Volver al índice](README.md)

> Cada pieza del stack de Mercury, con su versión exacta tal como está fijada en `pom.xml`, y por qué está ahí.

---

## ⚙️ Runtime

| Componente | Versión | Notas |
|---|---|---|
| ☕ Java | **21** (LTS) | `<java.version>21</java.version>` en `pom.xml` |
| 🍃 Spring Boot | **4.1.0** | `spring-boot-starter-parent` |
| ☁️ Spring Cloud | **2025.1.2** | BOM (`spring-cloud-dependencies`) — fija las versiones de Config/Vault/OpenFeign |
| 📦 Empaquetado | Jar ejecutable (`spring-boot-maven-plugin` repackage) | `BOOT-INF/classes` + `BOOT-INF/lib` |
| 🐳 Imagen base (Docker) | `amazoncorretto:21.0.11-alpine3.23` | Alpine — imagen mínima, usuario no-root (`jvapps`) |

---

## 🌐 Capa web / API

| Dependencia | Uso |
|---|---|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat |
| `springdoc-openapi-starter-webmvc-ui` | Sirve Swagger UI + `/v3/api-docs` a partir del contrato `src/main/resources/api/openapi.yaml` |
| `springdoc-openapi-maven-plugin` | Genera interfaces `*Api` desde el contrato en build-time (contract-first) |
| `spring-boot-starter-validation` | Bean Validation (`@Valid`, `@NotNull`, etc.) en los records de `api/v1/to` |
| `spring-boot-starter-actuator` | `/actuator/health` y demás endpoints de gestión |

> [!NOTE]
> El contrato OpenAPI es la **fuente de verdad**: los controladores implementan interfaces `*Api` generadas/anotadas a partir de `openapi.yaml`, no al revés. Ver [04 · Catálogo de Componentes](04-componentes.md).

---

## 🗄️ Persistencia

| Dependencia | Motor | Uso en Mercury |
|---|---|---|
| `spring-boot-starter-data-jpa` + `postgresql` | PostgreSQL | Campañas, métricas, plantillas, registros de mensajes, usuarios, códigos de verificación — datos **durables** |
| `spring-boot-starter-data-mongodb` | MongoDB | Documentos de mensajes **transitorios** (`messages` — polimórfico Sms/Telegram/WhatsApp/Push — y la cola de email) |
| `mapstruct` + `mapstruct-processor` | — | Mapeo Entity ↔ TO en compile-time (sin reflexión en runtime) |

DDL en modo `none` (`hibernate.ddl-auto: none`) — el esquema de PostgreSQL se gestiona fuera de Hibernate; Mercury nunca lo migra por su cuenta.

---

## 📨 Mensajería

| Dependencia | Uso |
|---|---|
| `spring-kafka` | Productor (publica un mensaje por destinatario al topic del canal) y consumidores (`@KafkaListener`) |
| `spring-boot-starter-mail` (Jakarta Mail / SMTP) | Único canal con entrega real hoy: Email vía SMTP |
| `telegrambots-client`, `telegrambots-longpolling`, `telegrambots-webhook`, `telegrambots-extensions` | Dependencias del Bot API de Telegram — **presentes en el `pom.xml` pero aún no usadas por ningún `ChannelService`** en esta rama (ver [04 · Componentes](04-componentes.md#-known-gaps)) |
| `spring-boot-starter-freemarker` | Motor de plantillas para el cuerpo de los emails |

### Serialización Kafka

- Productor: `JacksonJsonSerializer` (Spring Kafka 4.x) — **no** el `JsonSerializer` deprecado.
- Consumidor: `ErrorHandlingDeserializer` envolviendo `JacksonJsonDeserializer` — un mensaje malformado no tumba el listener.

> [!IMPORTANT]
> Jackson 3 (`tools.jackson.*`, usado por `JacksonJsonSerializer`) y Jackson 2 (`com.fasterxml.jackson.*`, usado por el resto de Spring) **coexisten** en este proyecto. No son intercambiables — cuidado al importar.

---

## ☁️ Configuración externa y secretos

| Dependencia | Uso |
|---|---|
| `spring-cloud-starter-config` | Cliente de Spring Cloud Config — trae `mercury-{profile}.yml` desde un repo Git vía el Config Server |
| `spring-cloud-starter-vault-config` | Cliente de HashiCorp Vault — trae secretos (`KV v2`) por entorno |

Ver [06 · Configuración de Entornos](06-configuracion-entornos.md) para el flujo completo de arranque (Vault → Config Server → aplicación).

---

## 🔐 Seguridad

| Dependencia | Uso |
|---|---|
| `spring-cloud-starter-openfeign` | `BackboneClient` — valida tokens de sesión contra el servicio Backbone (OAuth2) |
| `security-oauth` (librería privada PRX) | Utilidades OAuth2 compartidas |
| JWT propio (`com.umdc.mercury.security`) | Generación de tokens de sesión de la aplicación (`SessionJwtServiceImpl`) |

---

## 🧪 Calidad y build

| Herramienta | Umbral / configuración |
|---|---|
| `maven-pmd-plugin` | Corre en fase `test`; **0 violaciones** rompe el build (`ruleset.xml` en la raíz) |
| `jacoco-maven-plugin` | **70 % línea** (BUNDLE) y **50 % rama** (PACKAGE), aplicado en fase `verify` |
| `junit-jupiter` + `mockito-core` | Tests unitarios |
| `spring-restdocs-mockmvc` | Documentación de API generada desde tests de integración |
| `jmh-core` / `jmh-generator-annprocess` | Microbenchmarks (perfil `benchmark`) |
| `rewrite-maven-plugin` + `rewrite-spring` | Refactors automatizados (usado en las migraciones a Spring Boot 4) |

```bash
mvn -B -V -e clean verify          # build completo con tests + gate de cobertura
mvn -Pcoverage clean test          # genera XML de cobertura para SonarCloud
mvn -Pbenchmark clean test         # corre los benchmarks JMH
```

---

## 🏢 Dependencias privadas PRX

| Dependencia | Repositorio |
|---|---|
| `prx-commons`, `commons-services`, `security-oauth` | `https://repo.repsy.io/mvn/lmata/prx` — requieren credenciales en `~/.m2/settings.xml` |

---

## 🐳 Infraestructura de contenedores

| Herramienta | Rol |
|---|---|
| Docker (multi-stage no — imagen simple sobre jar pre-construido) | Empaqueta el jar + certificados baked-in (`certs/mercury/`) |
| `docker-entrypoint.sh` | Importa `*.crt` al truststore de la JVM al arrancar el contenedor |
| Docker Compose | Orquesta `mercury` (y, en el compose completo de desarrollo, `config-server`/`vault-server`) |

Ver [06 · Configuración de Entornos](06-configuracion-entornos.md) para el detalle completo de build/despliegue.

---

*Generado a partir de una lectura exhaustiva de `pom.xml`, `bootstrap.yml` y el código fuente real — no de documentación previa ni de supuestos.*
