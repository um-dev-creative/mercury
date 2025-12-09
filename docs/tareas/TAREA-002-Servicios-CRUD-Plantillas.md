# Tarea 2: Implementación de Servicios CRUD para Gestión de Plantillas

## Identificador
**MERC-TASK-002**

## Título
Implementación de Servicios de Negocio y API REST para Gestión de Plantillas

## Descripción
Implementar la capa de servicios de negocio y los controladores REST para gestionar el ciclo de vida completo de las plantillas dinámicas, incluyendo operaciones CRUD, versionado, y validaciones de negocio.

## Contexto
Con el modelo de datos implementado (Tarea 1), necesitamos exponer funcionalidades para que los usuarios puedan gestionar plantillas a través de una API REST. Esto incluye crear, leer, actualizar y eliminar plantillas, así como gestionar sus versiones y categorías.

## Objetivos
1. Implementar servicios de negocio para gestión de plantillas
2. Crear controladores REST con documentación OpenAPI
3. Implementar DTOs y mappers
4. Implementar validaciones de negocio
5. Crear tests de integración para la API

## Alcance

### Incluye:
- Service `TemplateService` con lógica de negocio
- Service `TemplateCategoryService`
- Service `TemplateVersionService`
- Controllers REST para todas las operaciones
- DTOs (Request/Response objects)
- Mappers (MapStruct)
- Validaciones de negocio y de entrada
- Exception handlers personalizados
- Documentación OpenAPI/Swagger
- Tests de integración

### No Incluye:
- Procesamiento de plantillas FreeMarker (siguiente tarea)
- Envío de mensajes
- Sistema de notificaciones
- Interfaz de usuario

## Detalle Técnico

### 1. DTOs (Data Transfer Objects)

#### TemplateRequestDTO
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateRequestDTO(
    @NotBlank(message = "El código de la plantilla es obligatorio")
    @Size(min = 3, max = 100, message = "El código debe tener entre 3 y 100 caracteres")
    String code,
    
    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(min = 5, max = 200, message = "El nombre debe tener entre 5 y 200 caracteres")
    String name,
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    String description,
    
    UUID categoryId,
    
    @NotNull(message = "El tipo de plantilla es obligatorio")
    TemplateType type,
    
    @NotNull(message = "El formato de plantilla es obligatorio")
    TemplateFormat format,
    
    @NotBlank(message = "El contenido de la plantilla es obligatorio")
    String content,
    
    List<TemplateVariableDTO> variables,
    
    Boolean active
) {}
```

#### TemplateResponseDTO
```java
public record TemplateResponseDTO(
    UUID id,
    String code,
    String name,
    String description,
    TemplateCategoryDTO category,
    TemplateType type,
    TemplateFormat format,
    Boolean active,
    Integer currentVersion,
    String currentContent,
    List<TemplateVariableDTO> variables,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy
) {}
```

#### TemplateVersionDTO
```java
public record TemplateVersionDTO(
    UUID id,
    Integer version,
    String content,
    String changeDescription,
    Boolean current,
    LocalDateTime createdAt,
    String createdBy
) {}
```

#### TemplateCategoryDTO
```java
public record TemplateCategoryDTO(
    UUID id,
    String code,
    String name,
    String description,
    UUID parentId,
    Boolean active
) {}
```

#### TemplateVariableDTO
```java
public record TemplateVariableDTO(
    UUID id,
    String name,
    String description,
    VariableType type,
    Boolean required,
    String defaultValue,
    String validationRegex
) {}
```

### 2. Service Layer

#### TemplateService
```java
@Service
@Transactional
public class TemplateService {
    
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplateCategoryRepository categoryRepository;
    private final TemplateMapper templateMapper;
    
    // Constructor injection
    
    /**
     * Crea una nueva plantilla con su primera versión
     */
    public TemplateResponseDTO createTemplate(TemplateRequestDTO requestDTO) {
        // Validar que el código no exista
        if (templateRepository.findByCode(requestDTO.code()).isPresent()) {
            throw new DuplicateTemplateException("Ya existe una plantilla con el código: " + requestDTO.code());
        }
        
        // Validar categoría si se especifica
        TemplateCategory category = null;
        if (requestDTO.categoryId() != null) {
            category = categoryRepository.findById(requestDTO.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Categoría no encontrada"));
        }
        
        // Crear plantilla
        Template template = templateMapper.toEntity(requestDTO);
        template.setCategory(category);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        
        // Crear primera versión
        TemplateVersion version = new TemplateVersion();
        version.setTemplate(template);
        version.setVersion(1);
        version.setContent(requestDTO.content());
        version.setCurrent(true);
        version.setCreatedAt(LocalDateTime.now());
        version.setChangeDescription("Versión inicial");
        
        template.getVersions().add(version);
        
        // Guardar
        Template savedTemplate = templateRepository.save(template);
        
        return templateMapper.toDTO(savedTemplate);
    }
    
    /**
     * Actualiza una plantilla existente creando una nueva versión
     */
    public TemplateResponseDTO updateTemplate(UUID id, TemplateRequestDTO requestDTO) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada"));
        
        // Actualizar campos básicos
        template.setName(requestDTO.name());
        template.setDescription(requestDTO.description());
        template.setType(requestDTO.type());
        template.setFormat(requestDTO.format());
        template.setUpdatedAt(LocalDateTime.now());
        
        // Si el contenido cambió, crear nueva versión
        if (!getCurrentContent(template).equals(requestDTO.content())) {
            createNewVersion(template, requestDTO.content(), "Actualización de plantilla");
        }
        
        Template updatedTemplate = templateRepository.save(template);
        return templateMapper.toDTO(updatedTemplate);
    }
    
    /**
     * Obtiene una plantilla por ID
     */
    @Transactional(readOnly = true)
    public TemplateResponseDTO getTemplateById(UUID id) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada"));
        return templateMapper.toDTO(template);
    }
    
    /**
     * Obtiene una plantilla por código
     */
    @Transactional(readOnly = true)
    public TemplateResponseDTO getTemplateByCode(String code) {
        Template template = templateRepository.findByCode(code)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada"));
        return templateMapper.toDTO(template);
    }
    
    /**
     * Lista todas las plantillas activas con paginación
     */
    @Transactional(readOnly = true)
    public Page<TemplateResponseDTO> listTemplates(Pageable pageable) {
        return templateRepository.findByActiveTrueAndDeletedFalse(pageable)
            .map(templateMapper::toDTO);
    }
    
    /**
     * Lista plantillas por categoría
     */
    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> listTemplatesByCategory(UUID categoryId) {
        TemplateCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException("Categoría no encontrada"));
        
        return templateRepository.findByCategoryAndActiveTrue(category).stream()
            .map(templateMapper::toDTO)
            .toList();
    }
    
    /**
     * Elimina una plantilla (soft delete)
     */
    public void deleteTemplate(UUID id) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada"));
        
        template.setDeleted(true);
        template.setActive(false);
        template.setUpdatedAt(LocalDateTime.now());
        
        templateRepository.save(template);
    }
    
    /**
     * Obtiene todas las versiones de una plantilla
     */
    @Transactional(readOnly = true)
    public List<TemplateVersionDTO> getTemplateVersions(UUID templateId) {
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada"));
        
        return versionRepository.findByTemplateOrderByVersionDesc(template).stream()
            .map(templateMapper::versionToDTO)
            .toList();
    }
    
    /**
     * Activa/Desactiva una plantilla
     */
    public TemplateResponseDTO toggleTemplateStatus(UUID id) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada"));
        
        template.setActive(!template.getActive());
        template.setUpdatedAt(LocalDateTime.now());
        
        Template updatedTemplate = templateRepository.save(template);
        return templateMapper.toDTO(updatedTemplate);
    }
    
    // Métodos privados auxiliares
    
    private String getCurrentContent(Template template) {
        return versionRepository.findByTemplateAndCurrentTrue(template)
            .map(TemplateVersion::getContent)
            .orElse("");
    }
    
    private void createNewVersion(Template template, String content, String description) {
        // Marcar versión actual como no actual
        versionRepository.findByTemplateAndCurrentTrue(template)
            .ifPresent(v -> {
                v.setCurrent(false);
                versionRepository.save(v);
            });
        
        // Crear nueva versión
        int nextVersion = template.getVersions().size() + 1;
        TemplateVersion newVersion = new TemplateVersion();
        newVersion.setTemplate(template);
        newVersion.setVersion(nextVersion);
        newVersion.setContent(content);
        newVersion.setCurrent(true);
        newVersion.setCreatedAt(LocalDateTime.now());
        newVersion.setChangeDescription(description);
        
        template.getVersions().add(newVersion);
    }
}
```

#### TemplateCategoryService
```java
@Service
@Transactional
public class TemplateCategoryService {
    
    private final TemplateCategoryRepository categoryRepository;
    private final TemplateCategoryMapper categoryMapper;
    
    public TemplateCategoryDTO createCategory(TemplateCategoryDTO dto) {
        if (categoryRepository.findByCode(dto.code()).isPresent()) {
            throw new DuplicateCategoryException("Ya existe una categoría con el código: " + dto.code());
        }
        
        TemplateCategory category = categoryMapper.toEntity(dto);
        
        if (dto.parentId() != null) {
            TemplateCategory parent = categoryRepository.findById(dto.parentId())
                .orElseThrow(() -> new CategoryNotFoundException("Categoría padre no encontrada"));
            category.setParent(parent);
        }
        
        TemplateCategory saved = categoryRepository.save(category);
        return categoryMapper.toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateCategoryDTO> listRootCategories() {
        return categoryRepository.findByActiveTrueAndParentNull().stream()
            .map(categoryMapper::toDTO)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public TemplateCategoryDTO getCategoryById(UUID id) {
        TemplateCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException("Categoría no encontrada"));
        return categoryMapper.toDTO(category);
    }
    
    public TemplateCategoryDTO updateCategory(UUID id, TemplateCategoryDTO dto) {
        TemplateCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException("Categoría no encontrada"));
        
        category.setName(dto.name());
        category.setDescription(dto.description());
        category.setActive(dto.active());
        
        TemplateCategory updated = categoryRepository.save(category);
        return categoryMapper.toDTO(updated);
    }
    
    public void deleteCategory(UUID id) {
        TemplateCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException("Categoría no encontrada"));
        
        if (!category.getChildren().isEmpty()) {
            throw new CategoryHasChildrenException("No se puede eliminar una categoría con subcategorías");
        }
        
        category.setActive(false);
        categoryRepository.save(category);
    }
}
```

### 3. REST Controllers

#### TemplateController
```java
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "API para gestión de plantillas dinámicas")
@CrossOrigin(origins = "*")
public class TemplateController {
    
    private final TemplateService templateService;
    
    @PostMapping
    @Operation(summary = "Crear nueva plantilla", description = "Crea una nueva plantilla con su primera versión")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Plantilla creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe una plantilla con ese código")
    })
    public ResponseEntity<TemplateResponseDTO> createTemplate(
            @Valid @RequestBody TemplateRequestDTO requestDTO) {
        TemplateResponseDTO response = templateService.createTemplate(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obtener plantilla por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plantilla encontrada"),
        @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    public ResponseEntity<TemplateResponseDTO> getTemplateById(@PathVariable UUID id) {
        TemplateResponseDTO response = templateService.getTemplateById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/code/{code}")
    @Operation(summary = "Obtener plantilla por código")
    public ResponseEntity<TemplateResponseDTO> getTemplateByCode(@PathVariable String code) {
        TemplateResponseDTO response = templateService.getTemplateByCode(code);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Listar plantillas", description = "Lista todas las plantillas activas con paginación")
    public ResponseEntity<Page<TemplateResponseDTO>> listTemplates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TemplateResponseDTO> response = templateService.listTemplates(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Listar plantillas por categoría")
    public ResponseEntity<List<TemplateResponseDTO>> listTemplatesByCategory(@PathVariable UUID categoryId) {
        List<TemplateResponseDTO> response = templateService.listTemplatesByCategory(categoryId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar plantilla", description = "Actualiza una plantilla y crea una nueva versión si el contenido cambió")
    public ResponseEntity<TemplateResponseDTO> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateRequestDTO requestDTO) {
        TemplateResponseDTO response = templateService.updateTemplate(id, requestDTO);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar plantilla", description = "Realiza un borrado lógico de la plantilla")
    @ApiResponse(responseCode = "204", description = "Plantilla eliminada exitosamente")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/versions")
    @Operation(summary = "Obtener versiones de plantilla")
    public ResponseEntity<List<TemplateVersionDTO>> getTemplateVersions(@PathVariable UUID id) {
        List<TemplateVersionDTO> response = templateService.getTemplateVersions(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Activar/Desactivar plantilla")
    public ResponseEntity<TemplateResponseDTO> toggleTemplateStatus(@PathVariable UUID id) {
        TemplateResponseDTO response = templateService.toggleTemplateStatus(id);
        return ResponseEntity.ok(response);
    }
}
```

### 4. Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFound(TemplateNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(DuplicateTemplateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTemplate(DuplicateTemplateException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Error de validación",
            errors,
            LocalDateTime.now()
        );
        
        return ResponseEntity.badRequest().body(response);
    }
}

record ErrorResponse(int status, String message, LocalDateTime timestamp) {}

record ValidationErrorResponse(
    int status, 
    String message, 
    Map<String, String> errors, 
    LocalDateTime timestamp
) {}
```

### 5. MapStruct Mapper

```java
@Mapper(componentModel = "spring")
public interface TemplateMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "variables", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Template toEntity(TemplateRequestDTO dto);
    
    @Mapping(source = "category", target = "category")
    @Mapping(target = "currentVersion", expression = "java(getCurrentVersion(template))")
    @Mapping(target = "currentContent", expression = "java(getCurrentContent(template))")
    TemplateResponseDTO toDTO(Template template);
    
    TemplateVersionDTO versionToDTO(TemplateVersion version);
    
    default Integer getCurrentVersion(Template template) {
        return template.getVersions().stream()
            .filter(TemplateVersion::getCurrent)
            .findFirst()
            .map(TemplateVersion::getVersion)
            .orElse(null);
    }
    
    default String getCurrentContent(Template template) {
        return template.getVersions().stream()
            .filter(TemplateVersion::getCurrent)
            .findFirst()
            .map(TemplateVersion::getContent)
            .orElse(null);
    }
}
```

## Dependencias
- **Dependencias de Tareas:**
  - MERC-TASK-001 (Modelo de Datos) - DEBE estar completada

- **Dependencias Técnicas:**
  - Spring Boot Web
  - Spring Data JPA
  - Spring Validation
  - MapStruct 1.5.5
  - SpringDoc OpenAPI 2.6.0

## Criterios de Aceptación

1. **CA-01**: Servicios de negocio implementados correctamente
   - ✅ TemplateService con todas las operaciones CRUD
   - ✅ TemplateCategoryService funcional
   - ✅ Validaciones de negocio implementadas
   - ✅ Manejo de transacciones apropiado
   - ✅ Versionado automático de plantillas

2. **CA-02**: API REST completamente funcional
   - ✅ Todos los endpoints responden correctamente
   - ✅ Validación de entrada funciona
   - ✅ Códigos de estado HTTP apropiados
   - ✅ Paginación implementada
   - ✅ Filtros funcionan correctamente

3. **CA-03**: DTOs y Mappers funcionan correctamente
   - ✅ Conversión entidad-DTO sin errores
   - ✅ Validaciones en DTOs funcionan
   - ✅ MapStruct genera código correcto
   - ✅ Sin mappings manuales innecesarios

4. **CA-04**: Exception handling implementado
   - ✅ Excepciones de negocio manejadas
   - ✅ Respuestas de error estructuradas
   - ✅ Mensajes descriptivos en español
   - ✅ Stack traces no expuestos en producción

5. **CA-05**: Documentación OpenAPI completa
   - ✅ Swagger UI accesible
   - ✅ Todos los endpoints documentados
   - ✅ Ejemplos de request/response
   - ✅ Descripciones claras

6. **CA-06**: Tests de integración pasan
   - ✅ Tests para cada endpoint
   - ✅ Tests de validación
   - ✅ Tests de manejo de errores
   - ✅ Cobertura > 80%

## Definition of Done (DoD)

- [x] Código compilado sin errores ni warnings
- [x] Todos los tests (unitarios e integración) pasan
- [x] Cobertura de tests > 80%
- [x] Code review aprobado
- [x] Sin violaciones PMD/Checkstyle
- [x] JavaDoc completo
- [x] Documentación OpenAPI generada
- [x] Swagger UI funcional y accesible
- [x] API testeada con Postman/Insomnia
- [x] Ejemplos de uso documentados
- [x] Logs apropiados implementados
- [x] Cambios commiteados y pusheados
- [x] Pull Request creado y aprobado

## Estimación
- **Complejidad**: Alta
- **Story Points**: 13
- **Tiempo Estimado**: 20-24 horas
- **Desarrolladores Requeridos**: 1 Backend Developer

## Riesgos
1. **Riesgo**: Complejidad de versionado de plantillas
   - **Mitigación**: Diseño simple con flag "current"

2. **Riesgo**: Performance con paginación
   - **Mitigación**: Índices apropiados ya definidos en Tarea 1

3. **Riesgo**: Validaciones complejas
   - **Mitigación**: Usar Bean Validation estándar

## Notas Adicionales
- Considerar agregar caché para plantillas frecuentes
- Implementar audit logging para cambios
- Agregar filtros avanzados en el futuro
- Considerar búsqueda full-text en próximas iteraciones

## Referencias
- Spring REST Docs: https://spring.io/projects/spring-restdocs
- MapStruct Guide: https://mapstruct.org/documentation/stable/reference/html/
- SpringDoc OpenAPI: https://springdoc.org/
- Bean Validation: https://beanvalidation.org/2.0/spec/
