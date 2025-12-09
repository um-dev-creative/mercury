# Tarea 4: Sistema de Previsualización de Plantillas

## Identificador
**MERC-TASK-004**

## Título
Implementación del Sistema de Previsualización de Plantillas

## Descripción
Implementar un sistema que permita previsualizar el resultado de una plantilla antes de enviarla, soportando datos de ejemplo y datos reales, con validación de sintaxis y detección de variables faltantes.

## Contexto
Los usuarios necesitan ver cómo se verá una plantilla antes de enviarla a destinatarios reales. Esto reduce errores, permite ajustes rápidos y mejora la experiencia del usuario. El sistema debe soportar diferentes modos de previsualización y formatos de salida.

## Objetivos
1. Crear servicio de previsualización de plantillas
2. Implementar API REST para previsualización
3. Soportar previsualización con datos de ejemplo y reales
4. Implementar visualización en múltiples formatos (HTML renderizado, código fuente)
5. Agregar validación de sintaxis y variables
6. Crear tests unitarios y de integración

## Alcance

### Incluye:
- PreviewService para procesar previsualizaciones
- PreviewController con endpoints REST
- DTOs para request/response de preview
- Validación de sintaxis FreeMarker
- Detección de variables no definidas
- Múltiples formatos de salida
- Tests completos

### No Incluye:
- Editor visual de plantillas
- Previsualización en tiempo real (websockets)
- Historial de previsualizaciones
- Comparación de versiones

## Detalle Técnico

### 1. DTOs para Previsualización

```java
public record TemplatePreviewRequest(
    @NotNull(message = "El ID o código de plantilla es requerido")
    String templateIdentifier, // Puede ser UUID o código de plantilla
    
    @NotNull(message = "Los parámetros son requeridos")
    Map<String, Object> parameters,
    
    PreviewMode mode, // EXAMPLE, REAL_DATA
    
    PreviewFormat format, // HTML, PLAIN_TEXT, SOURCE_CODE, JSON
    
    Boolean validateOnly // Solo validar sin renderizar
) {
    public TemplatePreviewRequest {
        if (mode == null) {
            mode = PreviewMode.EXAMPLE;
        }
        if (format == null) {
            format = PreviewFormat.HTML;
        }
        if (validateOnly == null) {
            validateOnly = false;
        }
    }
}

public record TemplatePreviewResponse(
    UUID templateId,
    String templateCode,
    String templateName,
    Integer version,
    Boolean valid,
    String renderedContent,
    String sourceContent,
    PreviewFormat format,
    List<String> missingVariables,
    List<String> unusedParameters,
    List<ValidationMessage> validationMessages,
    Map<String, Object> metadata,
    LocalDateTime previewedAt
) {}

public record ValidationMessage(
    ValidationLevel level, // INFO, WARNING, ERROR
    String message,
    String field,
    Integer lineNumber
) {}

public record DirectTemplatePreviewRequest(
    @NotBlank(message = "El contenido de la plantilla es requerido")
    String templateContent,
    
    @NotNull(message = "El formato de plantilla es requerido")
    TemplateFormat format,
    
    @NotNull(message = "Los parámetros son requeridos")
    Map<String, Object> parameters,
    
    PreviewFormat outputFormat
) {}

public enum PreviewMode {
    EXAMPLE,     // Usa datos de ejemplo
    REAL_DATA    // Usa datos reales proporcionados
}

public enum PreviewFormat {
    HTML,         // HTML renderizado
    PLAIN_TEXT,   // Texto plano
    SOURCE_CODE,  // Código fuente con sintaxis
    JSON          // JSON con metadata
}

public enum ValidationLevel {
    INFO, WARNING, ERROR
}
```

### 2. Preview Service

```java
@Service
public class TemplatePreviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplatePreviewService.class);
    
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplateProcessorService templateProcessor;
    private final TemplateVariableRepository variableRepository;
    
    /**
     * Genera una previsualización de una plantilla existente
     */
    public TemplatePreviewResponse previewTemplate(TemplatePreviewRequest request) {
        
        // Buscar plantilla por ID o código
        Template template = findTemplate(request.templateIdentifier());
        
        if (!template.getActive()) {
            throw new TemplateInactiveException("La plantilla no está activa");
        }
        
        // Obtener versión actual
        TemplateVersion currentVersion = versionRepository.findByTemplateAndCurrentTrue(template)
            .orElseThrow(() -> new TemplateVersionNotFoundException("No hay versión actual"));
        
        // Si solo se requiere validación, validar y retornar
        if (Boolean.TRUE.equals(request.validateOnly())) {
            return validateTemplateOnly(template, currentVersion, request.parameters());
        }
        
        // Preparar parámetros
        Map<String, Object> parameters = prepareParameters(
            template, 
            request.parameters(), 
            request.mode()
        );
        
        // Validar variables
        List<ValidationMessage> validationMessages = new ArrayList<>();
        List<String> missingVariables = validateVariables(template, parameters, validationMessages);
        List<String> unusedParameters = findUnusedParameters(template, parameters);
        
        // Renderizar contenido
        String renderedContent = null;
        boolean valid = missingVariables.isEmpty() && 
                       validationMessages.stream().noneMatch(v -> v.level() == ValidationLevel.ERROR);
        
        if (valid) {
            try {
                renderedContent = templateProcessor.processTemplate(template.getId(), parameters);
                
                // Aplicar formato de salida
                renderedContent = applyOutputFormat(renderedContent, request.format(), template.getFormat());
                
            } catch (TemplateProcessingException e) {
                logger.error("Error al renderizar plantilla", e);
                valid = false;
                validationMessages.add(new ValidationMessage(
                    ValidationLevel.ERROR,
                    "Error al renderizar: " + e.getMessage(),
                    null,
                    null
                ));
            }
        }
        
        // Construir metadata
        Map<String, Object> metadata = buildMetadata(template, currentVersion, parameters);
        
        return new TemplatePreviewResponse(
            template.getId(),
            template.getCode(),
            template.getName(),
            currentVersion.getVersion(),
            valid,
            renderedContent,
            currentVersion.getContent(),
            request.format(),
            missingVariables,
            unusedParameters,
            validationMessages,
            metadata,
            LocalDateTime.now()
        );
    }
    
    /**
     * Previsualiza una plantilla directamente desde contenido (sin guardar en BD)
     */
    public TemplatePreviewResponse previewDirectTemplate(DirectTemplatePreviewRequest request) {
        
        List<ValidationMessage> validationMessages = new ArrayList<>();
        
        // Validar sintaxis
        TemplateValidationResult validation = templateProcessor.validateTemplate(request.templateContent());
        
        if (!validation.valid()) {
            validationMessages.add(new ValidationMessage(
                ValidationLevel.ERROR,
                validation.message(),
                null,
                null
            ));
        }
        
        // Extraer variables de la plantilla
        Set<String> templateVariables = validation.variables();
        
        // Verificar variables faltantes
        List<String> missingVariables = templateVariables.stream()
            .filter(var -> !request.parameters().containsKey(var))
            .toList();
        
        if (!missingVariables.isEmpty()) {
            validationMessages.add(new ValidationMessage(
                ValidationLevel.WARNING,
                "Variables no proporcionadas: " + String.join(", ", missingVariables),
                null,
                null
            ));
        }
        
        // Verificar parámetros no usados
        List<String> unusedParameters = request.parameters().keySet().stream()
            .filter(param -> !templateVariables.contains(param))
            .toList();
        
        if (!unusedParameters.isEmpty()) {
            validationMessages.add(new ValidationMessage(
                ValidationLevel.INFO,
                "Parámetros no utilizados: " + String.join(", ", unusedParameters),
                null,
                null
            ));
        }
        
        // Renderizar contenido
        String renderedContent = null;
        boolean valid = validation.valid() && missingVariables.isEmpty();
        
        if (valid) {
            try {
                renderedContent = templateProcessor.processTemplateContent(
                    request.templateContent(), 
                    request.parameters()
                );
                
                renderedContent = applyOutputFormat(
                    renderedContent, 
                    request.outputFormat() != null ? request.outputFormat() : PreviewFormat.HTML,
                    request.format()
                );
                
            } catch (Exception e) {
                logger.error("Error al renderizar plantilla directa", e);
                valid = false;
                validationMessages.add(new ValidationMessage(
                    ValidationLevel.ERROR,
                    "Error al renderizar: " + e.getMessage(),
                    null,
                    null
                ));
            }
        }
        
        return new TemplatePreviewResponse(
            null,
            "preview-direct",
            "Vista previa directa",
            1,
            valid,
            renderedContent,
            request.templateContent(),
            request.outputFormat() != null ? request.outputFormat() : PreviewFormat.HTML,
            missingVariables,
            unusedParameters,
            validationMessages,
            Map.of("mode", "direct", "timestamp", LocalDateTime.now()),
            LocalDateTime.now()
        );
    }
    
    /**
     * Solo valida una plantilla sin renderizarla
     */
    private TemplatePreviewResponse validateTemplateOnly(
            Template template, 
            TemplateVersion version, 
            Map<String, Object> parameters) {
        
        List<ValidationMessage> validationMessages = new ArrayList<>();
        
        // Validar sintaxis
        TemplateValidationResult validation = templateProcessor.validateTemplate(version.getContent());
        
        if (!validation.valid()) {
            validationMessages.add(new ValidationMessage(
                ValidationLevel.ERROR,
                validation.message(),
                null,
                null
            ));
        }
        
        // Validar variables
        List<String> missingVariables = validateVariables(template, parameters, validationMessages);
        List<String> unusedParameters = findUnusedParameters(template, parameters);
        
        boolean valid = validation.valid() && missingVariables.isEmpty();
        
        return new TemplatePreviewResponse(
            template.getId(),
            template.getCode(),
            template.getName(),
            version.getVersion(),
            valid,
            null,
            version.getContent(),
            PreviewFormat.SOURCE_CODE,
            missingVariables,
            unusedParameters,
            validationMessages,
            Map.of("validationOnly", true),
            LocalDateTime.now()
        );
    }
    
    /**
     * Prepara los parámetros según el modo de previsualización
     */
    private Map<String, Object> prepareParameters(
            Template template, 
            Map<String, Object> providedParams,
            PreviewMode mode) {
        
        Map<String, Object> parameters = new HashMap<>(providedParams);
        
        if (mode == PreviewMode.EXAMPLE) {
            // Agregar valores de ejemplo para variables no proporcionadas
            template.getVariables().forEach(variable -> {
                if (!parameters.containsKey(variable.getName())) {
                    Object exampleValue = generateExampleValue(variable);
                    parameters.put(variable.getName(), exampleValue);
                }
            });
        }
        
        return parameters;
    }
    
    /**
     * Genera un valor de ejemplo según el tipo de variable
     */
    private Object generateExampleValue(TemplateVariable variable) {
        
        // Si tiene valor por defecto, usarlo
        if (variable.getDefaultValue() != null) {
            return convertDefaultValue(variable.getDefaultValue(), variable.getType());
        }
        
        // Generar según tipo
        return switch (variable.getType()) {
            case STRING -> "Ejemplo de " + variable.getName();
            case NUMBER -> 12345;
            case DATE -> LocalDate.now().toString();
            case BOOLEAN -> true;
            case OBJECT -> Map.of("ejemplo", "valor");
            case LIST -> List.of("item1", "item2", "item3");
        };
    }
    
    /**
     * Convierte el valor por defecto al tipo apropiado
     */
    private Object convertDefaultValue(String defaultValue, VariableType type) {
        try {
            return switch (type) {
                case STRING -> defaultValue;
                case NUMBER -> Integer.parseInt(defaultValue);
                case DATE -> LocalDate.parse(defaultValue);
                case BOOLEAN -> Boolean.parseBoolean(defaultValue);
                case OBJECT, LIST -> new ObjectMapper().readValue(defaultValue, Object.class);
            };
        } catch (Exception e) {
            logger.warn("Error al convertir valor por defecto, usando string: {}", defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Valida que las variables requeridas estén presentes
     */
    private List<String> validateVariables(
            Template template, 
            Map<String, Object> parameters,
            List<ValidationMessage> messages) {
        
        List<String> missingVariables = template.getVariables().stream()
            .filter(TemplateVariable::getRequired)
            .map(TemplateVariable::getName)
            .filter(varName -> !parameters.containsKey(varName))
            .toList();
        
        if (!missingVariables.isEmpty()) {
            messages.add(new ValidationMessage(
                ValidationLevel.ERROR,
                "Faltan variables requeridas: " + String.join(", ", missingVariables),
                null,
                null
            ));
        }
        
        return missingVariables;
    }
    
    /**
     * Encuentra parámetros que no se usan en la plantilla
     */
    private List<String> findUnusedParameters(Template template, Map<String, Object> parameters) {
        Set<String> templateVarNames = template.getVariables().stream()
            .map(TemplateVariable::getName)
            .collect(Collectors.toSet());
        
        return parameters.keySet().stream()
            .filter(param -> !templateVarNames.contains(param))
            .toList();
    }
    
    /**
     * Aplica el formato de salida al contenido renderizado
     */
    private String applyOutputFormat(String content, PreviewFormat outputFormat, TemplateFormat templateFormat) {
        return switch (outputFormat) {
            case HTML -> content; // Ya está en HTML
            case PLAIN_TEXT -> stripHtmlTags(content);
            case SOURCE_CODE -> escapeHtml(content);
            case JSON -> convertToJson(content, templateFormat);
        };
    }
    
    private String stripHtmlTags(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]*>", "");
    }
    
    private String escapeHtml(String html) {
        if (html == null) return null;
        return html
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
    
    private String convertToJson(String content, TemplateFormat format) {
        try {
            Map<String, Object> json = Map.of(
                "content", content,
                "format", format.name(),
                "length", content.length(),
                "timestamp", LocalDateTime.now().toString()
            );
            return new ObjectMapper().writeValueAsString(json);
        } catch (Exception e) {
            logger.error("Error al convertir a JSON", e);
            return content;
        }
    }
    
    /**
     * Construye metadata para la respuesta
     */
    private Map<String, Object> buildMetadata(
            Template template, 
            TemplateVersion version,
            Map<String, Object> parameters) {
        
        return Map.of(
            "templateType", template.getType().name(),
            "templateFormat", template.getFormat().name(),
            "version", version.getVersion(),
            "parameterCount", parameters.size(),
            "variableCount", template.getVariables().size(),
            "category", template.getCategory() != null ? template.getCategory().getName() : "Sin categoría"
        );
    }
    
    /**
     * Busca una plantilla por ID o código
     */
    private Template findTemplate(String identifier) {
        // Intentar buscar por UUID
        try {
            UUID uuid = UUID.fromString(identifier);
            return templateRepository.findById(uuid)
                .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada: " + identifier));
        } catch (IllegalArgumentException e) {
            // No es UUID, buscar por código
            return templateRepository.findByCode(identifier)
                .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada: " + identifier));
        }
    }
}
```

### 3. Preview Controller

```java
@RestController
@RequestMapping("/api/v1/templates/preview")
@Tag(name = "Template Preview", description = "API para previsualización de plantillas")
@CrossOrigin(origins = "*")
public class TemplatePreviewController {
    
    private final TemplatePreviewService previewService;
    
    public TemplatePreviewController(TemplatePreviewService previewService) {
        this.previewService = previewService;
    }
    
    @PostMapping
    @Operation(
        summary = "Previsualizar plantilla", 
        description = "Genera una previsualización de una plantilla existente con los parámetros proporcionados"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Previsualización generada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    public ResponseEntity<TemplatePreviewResponse> previewTemplate(
            @Valid @RequestBody TemplatePreviewRequest request) {
        
        TemplatePreviewResponse response = previewService.previewTemplate(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/direct")
    @Operation(
        summary = "Previsualizar plantilla directa",
        description = "Previsualiza contenido de plantilla sin necesidad de guardarla en la base de datos"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Previsualización generada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Contenido de plantilla inválido")
    })
    public ResponseEntity<TemplatePreviewResponse> previewDirectTemplate(
            @Valid @RequestBody DirectTemplatePreviewRequest request) {
        
        TemplatePreviewResponse response = previewService.previewDirectTemplate(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/validate")
    @Operation(
        summary = "Validar plantilla",
        description = "Valida la sintaxis y variables de una plantilla sin renderizarla"
    )
    public ResponseEntity<TemplatePreviewResponse> validateTemplate(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> parameters) {
        
        TemplatePreviewRequest request = new TemplatePreviewRequest(
            id.toString(),
            parameters,
            PreviewMode.REAL_DATA,
            PreviewFormat.SOURCE_CODE,
            true
        );
        
        TemplatePreviewResponse response = previewService.previewTemplate(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/example")
    @Operation(
        summary = "Previsualizar con datos de ejemplo",
        description = "Genera una previsualización usando datos de ejemplo para todas las variables"
    )
    public ResponseEntity<TemplatePreviewResponse> previewWithExampleData(@PathVariable UUID id) {
        
        TemplatePreviewRequest request = new TemplatePreviewRequest(
            id.toString(),
            Map.of(), // Parámetros vacíos, se usarán valores de ejemplo
            PreviewMode.EXAMPLE,
            PreviewFormat.HTML,
            false
        );
        
        TemplatePreviewResponse response = previewService.previewTemplate(request);
        return ResponseEntity.ok(response);
    }
}
```

## Dependencias
- **Dependencias de Tareas:**
  - MERC-TASK-001 (Modelo de Datos) - DEBE estar completada
  - MERC-TASK-002 (Servicios CRUD) - DEBE estar completada
  - MERC-TASK-003 (Integración Motor) - DEBE estar completada

- **Dependencias Técnicas:**
  - FreeMarker
  - Jackson (para conversión JSON)
  - Apache Commons Lang (para utilidades de string)

## Criterios de Aceptación

1. **CA-01**: Previsualización básica funciona
   - ✅ Previsualizar plantilla por ID
   - ✅ Previsualizar plantilla por código
   - ✅ Previsualizar con parámetros proporcionados
   - ✅ Respuesta incluye contenido renderizado

2. **CA-02**: Modos de previsualización funcionan
   - ✅ Modo EXAMPLE genera valores de ejemplo
   - ✅ Modo REAL_DATA usa solo valores proporcionados
   - ✅ Valores de ejemplo son apropiados por tipo

3. **CA-03**: Formatos de salida funcionan
   - ✅ HTML devuelve contenido renderizado
   - ✅ PLAIN_TEXT elimina tags HTML
   - ✅ SOURCE_CODE escapa HTML
   - ✅ JSON estructura correctamente

4. **CA-04**: Validación funciona correctamente
   - ✅ Detecta variables faltantes
   - ✅ Detecta errores de sintaxis
   - ✅ Identifica parámetros no usados
   - ✅ Niveles de validación apropiados (INFO, WARNING, ERROR)

5. **CA-05**: Previsualización directa funciona
   - ✅ Acepta contenido de plantilla inline
   - ✅ No requiere guardar en BD
   - ✅ Valida y renderiza correctamente

6. **CA-06**: API REST completa
   - ✅ Todos los endpoints funcionan
   - ✅ Documentación OpenAPI completa
   - ✅ Validación de entrada
   - ✅ Manejo de errores

7. **CA-07**: Tests completos
   - ✅ Tests unitarios de servicio
   - ✅ Tests de integración de controller
   - ✅ Tests de validación
   - ✅ Cobertura > 80%

## Definition of Done (DoD)

- [x] Código compilado sin errores
- [x] Todos los tests pasan
- [x] Cobertura > 80%
- [x] Code review aprobado
- [x] Sin violaciones PMD
- [x] JavaDoc completo
- [x] API documentada en Swagger
- [x] Tests de integración funcionan
- [x] Probado manualmente con Postman
- [x] Ejemplos de uso documentados
- [x] Logs apropiados
- [x] Performance aceptable (<500ms)
- [x] Cambios commiteados y pusheados
- [x] Pull Request aprobado

## Estimación
- **Complejidad**: Media-Alta
- **Story Points**: 8
- **Tiempo Estimado**: 12-16 horas
- **Desarrolladores Requeridos**: 1 Backend Developer

## Riesgos
1. **Riesgo**: Complejidad de validación de FreeMarker
   - **Mitigación**: Usar validador simple basado en regex

2. **Riesgo**: Performance con plantillas grandes
   - **Mitigación**: Timeout y límite de tamaño

3. **Riesgo**: Exposición de datos sensibles en preview
   - **Mitigación**: Sanitizar datos sensibles, usar datos de ejemplo por defecto

## Notas Adicionales
- Considerar agregar límite de tamaño para plantillas
- Implementar timeout para renderizado
- Evaluar sanitización de HTML en preview
- Documentar mejores prácticas de uso

## Referencias
- FreeMarker Template Testing: https://freemarker.apache.org/docs/pgui_misc_logging.html
- HTML Sanitization: https://jsoup.org/
