# Documentación del Proyecto Mercury - Sistema de Gestión de Plantillas Dinámicas

## 📋 Índice de Documentación

Este directorio contiene toda la documentación técnica y funcional para la implementación del Sistema de Gestión de Plantillas Dinámicas en el proyecto Mercury.

## 📚 Documentos Principales

### 1. [REQUERIMIENTOS.md](./REQUERIMIENTOS.md)
**Descripción**: Documento maestro de requerimientos funcionales y no funcionales.

**Contenido:**
- Requerimientos Funcionales (RF-01 a RF-06)
- Requerimientos No Funcionales (RNF-01 a RNF-06)
- Modelo de Datos Principal
- Flujos Principales
- Integraciones Externas
- Fases de Implementación
- Criterios de Éxito
- Riesgos y Mitigaciones

**Audiencia**: Product Owners, Arquitectos, Desarrolladores

### 2. [GUIA-IMPLEMENTACION.md](./GUIA-IMPLEMENTACION.md)
**Descripción**: Guía práctica para la implementación del sistema.

**Contenido:**
- Resumen Ejecutivo
- Arquitectura General
- Orden de Implementación
- Stack Tecnológico
- Fases del Proyecto
- Estimaciones y Recursos
- Guía de Desarrollo
- Troubleshooting Común

**Audiencia**: Desarrolladores, Tech Leads, DevOps

## 📁 Estructura de Directorios

```
docs/
├── REQUERIMIENTOS.md           # Requerimientos del sistema
├── GUIA-IMPLEMENTACION.md      # Guía de implementación
├── README.md                   # Este archivo
├── tareas/                     # Tareas detalladas
│   ├── TAREA-001-Modelo-Datos-Plantillas.md
│   ├── TAREA-002-Servicios-CRUD-Plantillas.md
│   ├── TAREA-003-Integracion-Motor-Plantillas.md
│   ├── TAREA-004-Previsualizacion-Plantillas.md
│   ├── TAREA-005-Seguimiento-Monitoreo.md
│   ├── TAREA-006-Procesamiento-Asincrono.md
│   ├── TAREA-007-Sistema-Webhooks.md          # (Pendiente)
│   ├── TAREA-008-Integracion-Telegram.md       # (Pendiente)
│   ├── TAREA-009-Gestion-Errores.md           # (Pendiente)
│   ├── TAREA-010-Metricas-Monitoreo.md        # (Pendiente)
│   ├── TAREA-011-Tests-Integracion.md         # (Pendiente)
│   └── TAREA-012-Documentacion-Final.md       # (Pendiente)
└── diagramas/                  # Diagramas UML y ER
    ├── casos-uso.puml
    ├── modelo-er.puml
    ├── diagrama-clases.puml
    └── secuencia-envio-email.puml
```

## 🎯 Tareas de Implementación

### Fase 1: Fundamentos (Completadas)
| ID | Tarea | Story Points | Estado |
|----|-------|--------------|--------|
| 001 | Modelo de Datos para Plantillas | 8 | ✅ Documentada |
| 002 | Servicios CRUD para Plantillas | 13 | ✅ Documentada |
| 003 | Integración Motor de Plantillas | 13 | ✅ Documentada |

### Fase 2: Funcionalidades Core (Completadas)
| ID | Tarea | Story Points | Estado |
|----|-------|--------------|--------|
| 004 | Previsualización de Plantillas | 8 | ✅ Documentada |
| 005 | Seguimiento y Monitoreo | 13 | ✅ Documentada |
| 006 | Procesamiento Asíncrono | 13 | ✅ Documentada |

### Fase 3: Integraciones (Pendientes)
| ID | Tarea | Story Points | Estado |
|----|-------|--------------|--------|
| 007 | Sistema de Webhooks | 8 | 📝 Por Documentar |
| 008 | Integración con Telegram | 13 | 📝 Por Documentar |
| 009 | Gestión de Errores | 13 | 📝 Por Documentar |

### Fase 4: Optimización (Pendientes)
| ID | Tarea | Story Points | Estado |
|----|-------|--------------|--------|
| 010 | Métricas y Monitoreo | 8 | 📝 Por Documentar |
| 011 | Tests de Integración E2E | 5 | 📝 Por Documentar |
| 012 | Documentación Final | 8 | 📝 Por Documentar |

**Total Story Points**: 123  
**Tareas Documentadas**: 6/12 (50%)  
**Progreso**: Fase 1 y 2 completas

## 🗺️ Diagramas

### Diagramas Disponibles

#### 1. [casos-uso.puml](./diagramas/casos-uso.puml)
**Tipo**: Diagrama de Casos de Uso  
**Descripción**: Muestra todos los casos de uso del sistema y sus actores  
**Formato**: PlantUML  

**Casos de Uso Principales:**
- Gestión de Plantillas (8 casos)
- Previsualización (4 casos)
- Envío de Mensajes (5 casos)
- Seguimiento y Monitoreo (5 casos)
- Gestión de Errores (4 casos)
- Notificaciones y Webhooks (3 casos)

#### 2. [modelo-er.puml](./diagramas/modelo-er.puml)
**Tipo**: Diagrama Entidad-Relación  
**Descripción**: Modelo completo de base de datos con todas las tablas  
**Formato**: PlantUML  

**Entidades Principales:**
- template_categories
- templates
- template_versions
- template_variables
- messages
- message_recipients
- delivery_logs
- notification_channels
- notification_configs
- error_logs
- webhooks
- webhook_deliveries

#### 3. [diagrama-clases.puml](./diagramas/diagrama-clases.puml)
**Tipo**: Diagrama de Clases  
**Descripción**: Arquitectura de clases del sistema  
**Formato**: PlantUML  

**Paquetes:**
- domain.entity (Entidades JPA)
- domain.enums (Enumeraciones)
- api.v1.to (DTOs)
- api.v1.service (Servicios)
- api.v1.controller (Controladores)
- repository (Repositorios)
- mapper (Mappers)

#### 4. [secuencia-envio-email.puml](./diagramas/secuencia-envio-email.puml)
**Tipo**: Diagrama de Secuencia  
**Descripción**: Flujo completo de envío de email con plantilla  
**Formato**: PlantUML  

**Flujo:**
1. Recepción de solicitud
2. Creación de registro
3. Procesamiento de plantilla
4. Envío de email
5. Actualización de estado
6. Logs y auditoría

### Visualización de Diagramas

#### Opción 1: Plugin de IDE
```bash
# IntelliJ IDEA
# Instalar plugin: PlantUML Integration
# Abrir archivo .puml y ver preview
```

#### Opción 2: VS Code
```bash
# Instalar extensión: PlantUML
# Command + Shift + P -> PlantUML: Preview
```

#### Opción 3: Online
```bash
# Visitar: http://www.plantuml.com/plantuml/uml/
# Copiar contenido del archivo .puml
```

#### Opción 4: CLI
```bash
# Instalar PlantUML
brew install plantuml  # macOS
sudo apt install plantuml  # Ubuntu

# Generar imágenes
cd docs/diagramas
plantuml casos-uso.puml
plantuml modelo-er.puml
plantuml diagrama-clases.puml
plantuml secuencia-envio-email.puml

# Se generarán archivos PNG en el mismo directorio
```

## 📖 Cómo Usar Esta Documentación

### Para Product Owners / Stakeholders
1. Leer [REQUERIMIENTOS.md](./REQUERIMIENTOS.md) para entender el alcance
2. Revisar las estimaciones en [GUIA-IMPLEMENTACION.md](./GUIA-IMPLEMENTACION.md)
3. Revisar diagrama de casos de uso para validar funcionalidades

### Para Arquitectos
1. Revisar [REQUERIMIENTOS.md](./REQUERIMIENTOS.md) - Sección de Arquitectura
2. Estudiar [modelo-er.puml](./diagramas/modelo-er.puml) para el modelo de datos
3. Revisar [diagrama-clases.puml](./diagramas/diagrama-clases.puml) para la arquitectura
4. Validar decisiones técnicas en [GUIA-IMPLEMENTACION.md](./GUIA-IMPLEMENTACION.md)

### Para Desarrolladores
1. Leer [GUIA-IMPLEMENTACION.md](./GUIA-IMPLEMENTACION.md) completa
2. Revisar tarea asignada en [tareas/](./tareas/)
3. Consultar diagramas relevantes
4. Seguir guía de desarrollo en GUIA-IMPLEMENTACION.md
5. Implementar siguiendo criterios de aceptación y DoD

### Para QA Engineers
1. Revisar criterios de aceptación en cada tarea
2. Diseñar casos de prueba basados en casos de uso
3. Validar flujos en diagramas de secuencia
4. Verificar DoD antes de aprobar PR

### Para DevOps
1. Revisar sección de Stack Tecnológico en GUIA-IMPLEMENTACION
2. Configurar infraestructura según requisitos
3. Implementar CI/CD siguiendo guías
4. Configurar monitoreo según métricas de éxito

## 🔄 Proceso de Actualización

### Actualizar Documentación
```bash
# 1. Hacer cambios en archivo correspondiente
vim docs/tareas/TAREA-XXX.md

# 2. Actualizar fecha de modificación
# Agregar al final del archivo:
# ---
# **Última actualización**: YYYY-MM-DD
# **Versión**: x.y.z

# 3. Commit con mensaje descriptivo
git add docs/
git commit -m "docs: actualizar TAREA-XXX con nuevos requisitos"

# 4. Push y crear PR
git push origin feature/update-docs
```

### Crear Nueva Tarea
```bash
# 1. Copiar plantilla de tarea existente
cp docs/tareas/TAREA-001-Modelo-Datos-Plantillas.md \
   docs/tareas/TAREA-XXX-Nueva-Tarea.md

# 2. Editar según template
# 3. Actualizar este README.md agregando la nueva tarea
# 4. Commit y push
```

### Crear Nuevo Diagrama
```bash
# 1. Crear archivo .puml
vim docs/diagramas/nuevo-diagrama.puml

# 2. Agregar contenido PlantUML
# 3. Generar imagen de preview
plantuml docs/diagramas/nuevo-diagrama.puml

# 4. Agregar referencia en este README
# 5. Commit y push
```

## ✅ Checklist de Completitud

### Documentación General
- [x] REQUERIMIENTOS.md completo
- [x] GUIA-IMPLEMENTACION.md completa
- [x] README.md del directorio docs

### Tareas - Fase 1
- [x] TAREA-001: Modelo de Datos
- [x] TAREA-002: Servicios CRUD
- [x] TAREA-003: Integración Motor

### Tareas - Fase 2
- [x] TAREA-004: Previsualización
- [x] TAREA-005: Seguimiento
- [x] TAREA-006: Procesamiento Asíncrono

### Tareas - Fase 3 (Pendientes)
- [ ] TAREA-007: Webhooks
- [ ] TAREA-008: Telegram
- [ ] TAREA-009: Gestión Errores

### Tareas - Fase 4 (Pendientes)
- [ ] TAREA-010: Métricas
- [ ] TAREA-011: Tests E2E
- [ ] TAREA-012: Documentación Final

### Diagramas
- [x] Casos de Uso
- [x] Modelo ER
- [x] Diagrama de Clases
- [x] Secuencia - Envío Email
- [ ] Secuencia - Procesamiento Asíncrono
- [ ] Secuencia - Webhooks
- [ ] Diagrama de Despliegue
- [ ] Diagrama de Componentes

## 📞 Soporte

### Preguntas sobre Documentación
- **Canal**: #mercury-docs en Slack
- **Email**: docs@prx-dev.com

### Reportar Errores en Documentación
- **Crear Issue**: [GitHub Issues](https://github.com/um-dev-creative/mercury/issues)
- **Template**: Usar "Documentation Error" template

### Sugerir Mejoras
- **Canal**: #mercury-improvement en Slack
- **Crear RFC**: docs/rfcs/RFC-XXX-titulo.md

## 📜 Licencia

Copyright (c) 2024 PRX Dev Innova. Todos los derechos reservados.

---

**Última actualización**: 2024-12-09  
**Versión**: 1.0.0  
**Mantenedores**: GitHub Copilot, PRX Dev Team

---

## 🚀 Quick Start para Nuevos Desarrolladores

```bash
# 1. Clonar repositorio
git clone https://github.com/um-dev-creative/mercury.git
cd mercury

# 2. Leer documentación básica
cat docs/README.md
cat docs/GUIA-IMPLEMENTACION.md

# 3. Configurar ambiente
docker-compose up -d
mvn clean install

# 4. Elegir tarea
# Ver: docs/tareas/

# 5. Crear rama
git checkout -b feature/MERC-TASK-XXX-descripcion

# 6. Desarrollar siguiendo la guía
# 7. Hacer commit y PR
```

¡Bienvenido al equipo Mercury! 🎉
