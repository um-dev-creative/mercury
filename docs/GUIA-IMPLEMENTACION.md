# Guía de Implementación - Sistema de Gestión de Plantillas Dinámicas

## Índice
1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura General](#arquitectura-general)
3. [Orden de Implementación](#orden-de-implementación)
4. [Stack Tecnológico](#stack-tecnológico)
5. [Fases del Proyecto](#fases-del-proyecto)
6. [Estimaciones y Recursos](#estimaciones-y-recursos)
7. [Métricas de Éxito](#métricas-de-éxito)
8. [Guía de Desarrollo](#guía-de-desarrollo)

## Resumen Ejecutivo

Este documento proporciona una guía completa para la implementación del Sistema de Gestión de Plantillas Dinámicas en el proyecto Mercury. El sistema permitirá gestionar plantillas de mensajes, enviar notificaciones multi-canal, previsualizar resultados, y monitorear envíos con una arquitectura escalable y robusta.

### Objetivos Principales
- ✅ Gestión completa del ciclo de vida de plantillas
- ✅ Envío multi-canal (Email, Telegram, SMS)
- ✅ Previsualización y validación de plantillas
- ✅ Seguimiento y monitoreo de envíos
- ✅ Procesamiento asíncrono escalable
- ✅ API REST completa y documentada

### Valor de Negocio
- Reducción del tiempo de desarrollo de nuevas notificaciones
- Mayor control y visibilidad sobre envíos
- Escalabilidad para grandes volúmenes
- Reducción de errores mediante validación
- Mejora en la experiencia del usuario final

## Arquitectura General

```
┌─────────────────────────────────────────────────────────────┐
│                    Cliente / Sistema Externo                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      API REST (Spring Boot)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Template    │  │   Preview    │  │   Message    │     │
│  │ Controller   │  │  Controller  │  │  Controller  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Capa de Servicios                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Template    │  │  Message     │  │   Message    │     │
│  │   Service    │  │  Producer    │  │  Tracking    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  PostgreSQL  │  │  RabbitMQ    │  │    Redis     │
│   Database   │  │    Queue     │  │    Cache     │
└──────────────┘  └──────────────┘  └──────────────┘
         │               │
         │               ▼
         │        ┌──────────────┐
         │        │   Message    │
         │        │   Workers    │
         │        └──────────────┘
         │               │
         └───────────────┼───────────────┐
                         │               │
                         ▼               ▼
                  ┌──────────────┐  ┌──────────────┐
                  │ JavaMail /   │  │   Telegram   │
                  │     SMTP     │  │   Bot API    │
                  └──────────────┘  └──────────────┘
```

## Orden de Implementación

### Fase 1: Fundamentos (Semanas 1-2)
**Tareas:**
1. ✅ TAREA-001: Modelo de Datos para Plantillas
2. ✅ TAREA-002: Servicios CRUD para Plantillas
3. ✅ TAREA-003: Integración del Motor de Plantillas

**Entregables:**
- Base de datos con todas las tablas
- API REST para gestión de plantillas
- Sistema de versionado funcional
- Envío de emails usando plantillas de BD

**Criterios de Aceptación:**
- Crear, editar, listar plantillas funciona
- Enviar email con plantilla funciona
- Tests con cobertura > 80%

### Fase 2: Funcionalidades Core (Semanas 3-4)
**Tareas:**
4. ✅ TAREA-004: Previsualización de Plantillas
5. ✅ TAREA-005: Seguimiento y Monitoreo
6. ✅ TAREA-006: Procesamiento Asíncrono

**Entregables:**
- Sistema de previsualización con validación
- Dashboard de seguimiento (backend)
- Sistema de colas con RabbitMQ
- Procesamiento asíncrono de mensajes

**Criterios de Aceptación:**
- Preview de plantillas funciona
- Consultar estado de envíos funciona
- Mensajes se procesan asíncronamente
- Rate limiting implementado

### Fase 3: Integraciones y Notificaciones (Semanas 5-6)
**Tareas:**
7. 🔄 TAREA-007: Sistema de Webhooks
8. 🔄 TAREA-008: Integración con Telegram
9. 🔄 TAREA-009: Gestión de Errores y Notificaciones

**Entregables:**
- Sistema de webhooks para notificaciones
- Envío de mensajes por Telegram
- Dashboard de errores
- Notificaciones a administradores

**Criterios de Aceptación:**
- Webhooks se disparan correctamente
- Envío por Telegram funciona
- Errores se clasifican y notifican

### Fase 4: Optimización y Documentación (Semana 7-8)
**Tareas:**
10. 🔄 TAREA-010: Métricas y Monitoreo
11. 🔄 TAREA-011: Tests de Integración E2E
12. 🔄 TAREA-012: Documentación Final

**Entregables:**
- Dashboard de métricas con Actuator
- Suite completa de tests E2E
- Documentación técnica y de usuario
- Guías de despliegue

**Criterios de Aceptación:**
- Métricas expuestas y funcionando
- Tests E2E pasan
- Documentación completa
- Sistema desplegable en producción

## Stack Tecnológico

### Backend
- **Framework**: Spring Boot 3.4.1
- **Lenguaje**: Java 21
- **Build Tool**: Maven 3.8+

### Base de Datos
- **Principal**: PostgreSQL 14+
- **Caché**: Redis 7+
- **Migraciones**: Flyway

### Mensajería
- **Queue**: RabbitMQ 3.12+
- **Alternativa**: Apache Kafka 3.5+ (futuro)

### Plantillas
- **Motor**: FreeMarker 2.3+

### Notificaciones
- **Email**: JavaMailSender (Spring Boot Mail)
- **Telegram**: Telegram Bot API 8.0.0

### Testing
- **Unitarios**: JUnit 5, Mockito
- **Integración**: Spring Boot Test, Testcontainers
- **Cobertura**: JaCoCo

### Documentación
- **API**: SpringDoc OpenAPI 2.6.0 (Swagger)
- **Diagramas**: PlantUML
- **Código**: JavaDoc

### Monitoreo
- **Métricas**: Spring Actuator, Micrometer
- **Logs**: SLF4J, Logback
- **APM**: (Opcional) Prometheus, Grafana

### Seguridad
- **Autenticación**: OAuth2
- **Autorización**: Spring Security
- **Secretos**: HashiCorp Vault

## Fases del Proyecto

### Duración Total: 8 Semanas
- Fase 1: 2 semanas
- Fase 2: 2 semanas
- Fase 3: 2 semanas
- Fase 4: 2 semanas

### Hitos (Milestones)

#### Milestone 1: MVP Plantillas (Fin Semana 2)
✅ Gestión básica de plantillas
✅ Envío de emails con plantillas

#### Milestone 2: Sistema Completo (Fin Semana 4)
✅ Previsualización
✅ Monitoreo
✅ Procesamiento asíncrono

#### Milestone 3: Integración Multi-Canal (Fin Semana 6)
✅ Webhooks
✅ Telegram
✅ Gestión de errores

#### Milestone 4: Producción Ready (Fin Semana 8)
✅ Tests completos
✅ Documentación
✅ Optimización
✅ Deploy a producción

## Estimaciones y Recursos

### Story Points por Fase

| Fase | Tareas | Story Points | Días Estimados |
|------|--------|--------------|----------------|
| 1    | 3      | 34           | 10-12          |
| 2    | 3      | 34           | 10-12          |
| 3    | 3      | 34           | 10-12          |
| 4    | 3      | 21           | 7-9            |
| **Total** | **12** | **123** | **37-45**     |

### Equipo Recomendado

**Configuración Mínima:**
- 1 Backend Developer Senior (Full-time)
- 1 DevOps Engineer (Part-time, 50%)

**Configuración Óptima:**
- 2 Backend Developers (1 Senior, 1 Mid)
- 1 QA Engineer (Part-time, 50%)
- 1 DevOps Engineer (Part-time, 50%)

### Distribución de Esfuerzo

```
Desarrollo:        60% (74 story points)
Testing:           20% (25 story points)
Documentación:     10% (12 story points)
DevOps/Deploy:     10% (12 story points)
```

## Métricas de Éxito

### Métricas Técnicas

1. **Cobertura de Código**
   - Target: > 80%
   - Crítico: > 90% en servicios core

2. **Performance**
   - API Response Time: < 200ms (p95)
   - Message Processing: > 100 msg/min
   - Template Preview: < 500ms

3. **Disponibilidad**
   - Uptime: > 99.5%
   - Message Loss Rate: < 0.1%

4. **Calidad de Código**
   - PMD Violations: 0 (Priority 1-3)
   - Security Vulnerabilities: 0 (High/Critical)
   - Technical Debt Ratio: < 5%

### Métricas de Negocio

1. **Adopción**
   - Plantillas creadas: > 50 en primer mes
   - APIs calls: > 1000 por día
   - Mensajes enviados: > 10000 por semana

2. **Eficiencia**
   - Tiempo de creación de plantilla: < 5 min
   - Tiempo de previsualización: < 1 min
   - Tiempo de deploy de cambios: < 10 min

3. **Calidad**
   - Tasa de error en envíos: < 1%
   - Tasa de rebote: < 2%
   - Satisfacción de usuario: > 4/5

## Guía de Desarrollo

### Configuración del Ambiente de Desarrollo

#### 1. Prerequisitos
```bash
# Java
java -version  # Debe ser Java 21

# Maven
mvn -version   # Debe ser Maven 3.8+

# Docker
docker --version
docker-compose --version

# Git
git --version
```

#### 2. Clonar Repositorio
```bash
git clone https://github.com/um-dev-creative/mercury.git
cd mercury
```

#### 3. Configurar Base de Datos (Docker)
```bash
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: mercury
      POSTGRES_USER: mercury
      POSTGRES_PASSWORD: mercury123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: mercury
      RABBITMQ_DEFAULT_PASS: mercury123

volumes:
  postgres_data:
```

```bash
# Iniciar servicios
docker-compose up -d
```

#### 4. Configurar Aplicación
```yaml
# src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mercury
    username: mercury
    password: mercury123
  
  rabbitmq:
    host: localhost
    port: 5672
    username: mercury
    password: mercury123
  
  redis:
    host: localhost
    port: 6379

  flyway:
    enabled: true
    locations: classpath:db/migration

mercury:
  email:
    rate-limit: 100
  telegram:
    rate-limit: 30
```

#### 5. Compilar y Ejecutar
```bash
# Compilar
mvn clean install

# Ejecutar tests
mvn test

# Ejecutar aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# O con jar
java -jar target/mercury.jar --spring.profiles.active=dev
```

#### 6. Verificar Instalación
```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# RabbitMQ Management
open http://localhost:15672  # user: mercury, pass: mercury123
```

### Flujo de Desarrollo

#### 1. Crear Rama para Tarea
```bash
git checkout -b feature/MERC-TASK-XXX-descripcion
```

#### 2. Desarrollo con TDD
```java
// 1. Escribir test
@Test
void shouldCreateTemplate() {
    // Given
    TemplateRequestDTO request = ...;
    
    // When
    TemplateResponseDTO response = templateService.createTemplate(request);
    
    // Then
    assertNotNull(response.id());
    assertEquals(request.name(), response.name());
}

// 2. Implementar código mínimo para pasar test

// 3. Refactorizar

// 4. Repetir
```

#### 3. Ejecutar Tests y Linters
```bash
# Tests unitarios
mvn test

# Tests de integración
mvn verify

# Cobertura
mvn jacoco:report
open target/site/jacoco/index.html

# PMD
mvn pmd:check

# Checkstyle (si está configurado)
mvn checkstyle:check
```

#### 4. Commit y Push
```bash
git add .
git commit -m "MERC-TASK-XXX: Descripción del cambio"
git push origin feature/MERC-TASK-XXX-descripcion
```

#### 5. Crear Pull Request
- Título: `MERC-TASK-XXX: Título de la tarea`
- Descripción: Checklist de DoD
- Asignar reviewers
- Linkar issue/tarea

#### 6. Code Review
- Esperar aprobación
- Incorporar feedback
- Re-push cambios

#### 7. Merge
- Squash and merge (preferido)
- O merge commit con historia

### Convenciones de Código

#### Naming
```java
// Clases: PascalCase
public class TemplateService { }

// Métodos: camelCase
public TemplateResponseDTO createTemplate() { }

// Constantes: UPPER_SNAKE_CASE
public static final String DEFAULT_ENCODING = "UTF-8";

// Packages: lowercase
package com.prx.mercury.api.v1.service;
```

#### Estructura de Paquetes
```
com.prx.mercury
├── api.v1
│   ├── controller    # REST Controllers
│   ├── service       # Business Logic
│   └── to            # Transfer Objects (DTOs)
├── domain
│   ├── entity        # JPA Entities
│   ├── enums         # Enumerations
│   └── repository    # Spring Data Repositories
├── config            # Configuration Classes
├── mapper            # MapStruct Mappers
├── exception         # Custom Exceptions
└── util              # Utilities
```

#### JavaDoc
```java
/**
 * Servicio para gestión de plantillas dinámicas.
 * Proporciona operaciones CRUD y funcionalidades de versionado.
 *
 * @author Nombre Desarrollador
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class TemplateService {
    
    /**
     * Crea una nueva plantilla con su primera versión.
     *
     * @param requestDTO datos de la plantilla a crear
     * @return plantilla creada con ID generado
     * @throws DuplicateTemplateException si ya existe una plantilla con el mismo código
     * @throws CategoryNotFoundException si la categoría especificada no existe
     */
    public TemplateResponseDTO createTemplate(TemplateRequestDTO requestDTO) {
        // Implementation
    }
}
```

### Troubleshooting Común

#### Problema: Tests fallan por timeout
**Solución:**
```java
@Test
@Timeout(value = 10, unit = TimeUnit.SECONDS)
void testLongRunning() {
    // Test code
}
```

#### Problema: Base de datos no conecta
**Solución:**
```bash
# Verificar que PostgreSQL está corriendo
docker ps | grep postgres

# Ver logs
docker logs mercury_postgres_1

# Recrear contenedor
docker-compose down
docker-compose up -d postgres
```

#### Problema: RabbitMQ no procesa mensajes
**Solución:**
```bash
# Verificar queues en Management UI
open http://localhost:15672/#/queues

# Ver logs de aplicación
tail -f logs/application.log

# Purgar queue si es necesario
rabbitmqadmin purge queue name=mercury.email.queue
```

#### Problema: Cobertura de tests baja
**Solución:**
```bash
# Identificar código no cubierto
mvn jacoco:report
open target/site/jacoco/index.html

# Agregar tests para métodos no cubiertos
# Priorizar servicios y controladores
```

## Recursos Adicionales

### Documentación
- [Requerimientos Completos](./REQUERIMIENTOS.md)
- [Tareas Detalladas](./tareas/)
- [Diagramas UML](./diagramas/)

### Referencias Externas
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [FreeMarker Manual](https://freemarker.apache.org/docs/)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Herramientas Recomendadas
- **IDE**: IntelliJ IDEA Ultimate
- **DB Client**: DBeaver, pgAdmin
- **API Testing**: Postman, Insomnia
- **Git Client**: SourceTree, GitKraken
- **Diagrams**: PlantUML, Draw.io

## Contactos y Soporte

### Equipo de Desarrollo
- **Tech Lead**: TBD
- **Backend Developers**: TBD
- **DevOps Engineer**: TBD

### Canales de Comunicación
- **Slack**: #mercury-development
- **Email**: mercury-team@prx-dev.com
- **Jira**: MERCURY Board

---

**Última actualización**: 2024-12-09  
**Versión del documento**: 1.0.0  
**Autor**: GitHub Copilot / PRX Dev Team
