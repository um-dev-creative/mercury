# Tarea 1: Diseño e Implementación del Modelo de Datos para Plantillas

## Identificador
**MERC-TASK-001**

## Título
Diseño e Implementación del Modelo de Datos para Gestión de Plantillas Dinámicas

## Descripción
Diseñar e implementar el modelo de datos completo para soportar la gestión de plantillas dinámicas, incluyendo versionado, categorización y metadatos. Este modelo será la base para todas las funcionalidades de gestión de plantillas del sistema.

## Contexto
Actualmente, Mercury carga plantillas FreeMarker directamente desde el sistema de archivos sin gestión en base de datos. Se necesita un modelo robusto que permita:
- Almacenar plantillas de forma persistente
- Versionar cambios en plantillas
- Categorizar plantillas para mejor organización
- Almacenar metadatos y configuraciones

## Objetivos
1. Diseñar el modelo entidad-relación para plantillas
2. Crear las entidades JPA correspondientes
3. Implementar repositorios Spring Data
4. Crear migraciones de base de datos
5. Implementar tests unitarios para repositorios

## Alcance

### Incluye:
- Entidad `Template` con todos sus campos
- Entidad `TemplateVersion` para versionado
- Entidad `TemplateCategory` para categorización
- Entidad `TemplateVariable` para definición de variables
- Repositorios JPA para todas las entidades
- Scripts de migración Flyway/Liquibase
- Tests unitarios de repositorios

### No Incluye:
- Servicios de negocio (siguiente tarea)
- API REST (siguiente tarea)
- Interfaz de usuario
- Lógica de procesamiento de plantillas

## Detalle Técnico

### 1. Entidad Template
```java
@Entity
@Table(name = "templates")
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TemplateCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateType type; // EMAIL, TELEGRAM, SMS, WEBHOOK
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateFormat format; // HTML, PLAIN_TEXT, MARKDOWN
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false)
    private Boolean deleted = false;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<TemplateVersion> versions;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<TemplateVariable> variables;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private String createdBy;
    private String updatedBy;
    
    // Getters, setters, equals, hashCode
}
```

### 2. Entidad TemplateVersion
```java
@Entity
@Table(name = "template_versions")
public class TemplateVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;
    
    @Column(nullable = false)
    private Integer version;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 500)
    private String changeDescription;
    
    @Column(nullable = false)
    private Boolean current = false;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    // Getters, setters
}
```

### 3. Entidad TemplateCategory
```java
@Entity
@Table(name = "template_categories")
public class TemplateCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 300)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TemplateCategory parent;
    
    @OneToMany(mappedBy = "parent")
    private List<TemplateCategory> children;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Getters, setters
}
```

### 4. Entidad TemplateVariable
```java
@Entity
@Table(name = "template_variables")
public class TemplateVariable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 300)
    private String description;
    
    @Enumerated(EnumType.STRING)
    private VariableType type; // STRING, NUMBER, DATE, BOOLEAN, OBJECT
    
    @Column(nullable = false)
    private Boolean required = false;
    
    @Column(length = 500)
    private String defaultValue;
    
    @Column(length = 200)
    private String validationRegex;
    
    // Getters, setters
}
```

### 5. Repositorios
```java
public interface TemplateRepository extends JpaRepository<Template, UUID> {
    Optional<Template> findByCode(String code);
    List<Template> findByActiveTrue();
    List<Template> findByCategoryAndActiveTrue(TemplateCategory category);
    Page<Template> findByActiveTrueAndDeletedFalse(Pageable pageable);
}

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {
    List<TemplateVersion> findByTemplateOrderByVersionDesc(Template template);
    Optional<TemplateVersion> findByTemplateAndCurrentTrue(Template template);
}

public interface TemplateCategoryRepository extends JpaRepository<TemplateCategory, UUID> {
    Optional<TemplateCategory> findByCode(String code);
    List<TemplateCategory> findByActiveTrueAndParentNull();
}

public interface TemplateVariableRepository extends JpaRepository<TemplateVariable, UUID> {
    List<TemplateVariable> findByTemplate(Template template);
    List<TemplateVariable> findByTemplateAndRequiredTrue(Template template);
}
```

### 6. Enums
```java
public enum TemplateType {
    EMAIL, TELEGRAM, SMS, WEBHOOK
}

public enum TemplateFormat {
    HTML, PLAIN_TEXT, MARKDOWN
}

public enum VariableType {
    STRING, NUMBER, DATE, BOOLEAN, OBJECT, LIST
}
```

### 7. Migración de Base de Datos (Flyway)
```sql
-- V1__create_template_tables.sql

CREATE TABLE template_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    parent_id UUID REFERENCES template_categories(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    category_id UUID REFERENCES template_categories(id),
    type VARCHAR(20) NOT NULL,
    format VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE template_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    change_description VARCHAR(500),
    current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    UNIQUE(template_id, version)
);

CREATE TABLE template_variables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    type VARCHAR(20),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(500),
    validation_regex VARCHAR(200),
    UNIQUE(template_id, name)
);

CREATE INDEX idx_templates_code ON templates(code);
CREATE INDEX idx_templates_category ON templates(category_id);
CREATE INDEX idx_templates_active ON templates(active, deleted);
CREATE INDEX idx_template_versions_template ON template_versions(template_id);
CREATE INDEX idx_template_versions_current ON template_versions(template_id, current);
CREATE INDEX idx_template_variables_template ON template_variables(template_id);
CREATE INDEX idx_template_categories_parent ON template_categories(parent_id);
```

## Dependencias
- **Dependencias Técnicas:**
  - Spring Boot 3.4.1
  - Spring Data JPA
  - PostgreSQL driver (42.7.4)
  - Flyway o Liquibase para migraciones
  - Lombok (opcional, para reducir boilerplate)

- **Dependencias de Tareas:**
  - Ninguna (esta es la primera tarea)

## Criterios de Aceptación

1. **CA-01**: Todas las entidades JPA están creadas con anotaciones apropiadas
   - ✅ Entidad Template completa
   - ✅ Entidad TemplateVersion completa
   - ✅ Entidad TemplateCategory completa
   - ✅ Entidad TemplateVariable completa
   - ✅ Relaciones bidireccionales configuradas correctamente
   - ✅ Índices de base de datos definidos

2. **CA-02**: Los repositorios Spring Data están implementados
   - ✅ TemplateRepository con métodos personalizados
   - ✅ TemplateVersionRepository con queries de versionado
   - ✅ TemplateCategoryRepository con jerarquía
   - ✅ TemplateVariableRepository con filtros
   - ✅ Métodos de búsqueda siguen convenciones de nomenclatura

3. **CA-03**: Scripts de migración de base de datos funcionan correctamente
   - ✅ Script crea todas las tablas
   - ✅ Script crea todos los índices
   - ✅ Script crea todas las foreign keys
   - ✅ Script es idempotente
   - ✅ Script tiene rollback definido

4. **CA-04**: Tests unitarios de repositorios están implementados
   - ✅ Tests para cada operación CRUD
   - ✅ Tests para queries personalizadas
   - ✅ Tests para restricciones de integridad
   - ✅ Tests usan base de datos en memoria (H2) o testcontainers
   - ✅ Cobertura de código > 80%

5. **CA-05**: Documentación técnica está completa
   - ✅ JavaDoc en todas las clases
   - ✅ Diagrama ER actualizado
   - ✅ README con instrucciones de setup de BD
   - ✅ Ejemplos de uso de repositorios

## Definition of Done (DoD)

- [x] Código compilado sin errores ni warnings
- [x] Todos los tests unitarios pasan (green)
- [x] Cobertura de tests > 80% en repositorios
- [x] Código revisado y aprobado (code review)
- [x] Sin violaciones de PMD/Checkstyle
- [x] JavaDoc completo en clases públicas
- [x] Scripts de migración ejecutados exitosamente
- [x] Tablas creadas en base de datos de desarrollo
- [x] Índices verificados en base de datos
- [x] Documentación actualizada en /docs
- [x] Diagrama ER actualizado
- [x] Cambios commiteados y pusheados
- [x] Pull Request creado y aprobado

## Estimación
- **Complejidad**: Media
- **Story Points**: 8
- **Tiempo Estimado**: 12-16 horas
- **Desarrolladores Requeridos**: 1 Backend Developer

## Riesgos
1. **Riesgo**: Cambios en el modelo durante implementación
   - **Mitigación**: Revisión de diseño antes de implementar

2. **Riesgo**: Incompatibilidad con versión de PostgreSQL
   - **Mitigación**: Testear con versión específica (14+)

3. **Riesgo**: Performance de queries con grandes volúmenes
   - **Mitigación**: Implementar índices apropiados desde el inicio

## Notas Adicionales
- Considerar agregar auditoría con Spring Data JPA (@EntityListeners)
- Evaluar uso de @Embedded para campos de auditoría
- Documentar decisión de usar UUID vs Long para IDs
- Configurar estrategia de generación de UUID apropiada
- Considerar soft delete en todas las entidades principales

## Referencias
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Flyway: https://flywaydb.org/documentation/
- PostgreSQL UUID: https://www.postgresql.org/docs/current/datatype-uuid.html
- JPA Best Practices: https://thorben-janssen.com/tips-to-boost-your-hibernate-performance/
