# Tarea 5: Sistema de Seguimiento y Monitoreo de Envíos

## Identificador
**MERC-TASK-005**

## Título
Implementación del Sistema de Seguimiento y Monitoreo de Envíos de Mensajes

## Descripción
Implementar un sistema completo para el seguimiento y monitoreo del estado de los mensajes enviados, incluyendo consultas de estado, historial, estadísticas, exportación de logs y capacidad de reintentar envíos fallidos.

## Contexto
Los usuarios necesitan visibilidad completa sobre el estado de los mensajes enviados. Esto incluye conocer si un mensaje fue enviado exitosamente, si falló, estadísticas de tasas de éxito, y capacidad para reintentar mensajes fallidos.

## Objetivos
1. Crear servicio de seguimiento de mensajes
2. Implementar API REST para consultas de estado
3. Desarrollar sistema de estadísticas y métricas
4. Implementar funcionalidad de reintento de mensajes
5. Crear exportación de logs y reportes
6. Implementar filtros avanzados de búsqueda

## Alcance

### Incluye:
- MessageTrackingService para consultas y estadísticas
- API REST completa para seguimiento
- DTOs para filtros y respuestas
- Sistema de reintentos con políticas configurables
- Exportación de logs en múltiples formatos (JSON, CSV)
- Estadísticas agregadas por período, plantilla, estado
- Tests unitarios e integración

### No Incluye:
- Dashboard visual (frontend)
- Alertas en tiempo real (websockets)
- Análisis predictivo
- Integración con sistemas externos de monitoreo

## Detalle Técnico

### 1. DTOs para Seguimiento

```java
public record MessageStatusDTO(
    UUID messageId,
    String templateCode,
    String templateName,
    MessageStatus status,
    String subject,
    String fromAddress,
    Integer recipientCount,
    Integer deliveredCount,
    Integer failedCount,
    Integer bouncedCount,
    LocalDateTime scheduledAt,
    LocalDateTime sentAt,
    LocalDateTime deliveredAt,
    LocalDateTime failedAt,
    String errorMessage,
    Integer retryCount,
    Integer maxRetries,
    Boolean canRetry,
    List<RecipientStatusDTO> recipients,
    List<DeliveryLogDTO> logs
) {}

public record RecipientStatusDTO(
    String address,
    String name,
    RecipientType type,
    DeliveryStatus status,
    LocalDateTime deliveredAt,
    LocalDateTime bouncedAt,
    String errorMessage
) {}

public record DeliveryLogDTO(
    LocalDateTime timestamp,
    LogLevel level,
    String event,
    String details
) {}

public record MessageFilterDTO(
    List<UUID> templateIds,
    List<MessageStatus> statuses,
    List<MessageType> types,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String recipientEmail,
    String subject,
    Boolean failedOnly,
    Boolean pendingRetry
) {}

public record MessageListDTO(
    UUID messageId,
    String templateCode,
    MessageStatus status,
    String subject,
    Integer recipientCount,
    LocalDateTime sentAt,
    String errorMessage
) {}

public record DeliveryStatisticsDTO(
    Long totalMessages,
    Long sentMessages,
    Long deliveredMessages,
    Long failedMessages,
    Long pendingMessages,
    Long cancelledMessages,
    Double successRate,
    Double failureRate,
    Map<String, Long> messagesByTemplate,
    Map<String, Long> messagesByStatus,
    Map<LocalDate, Long> messagesByDate,
    Map<String, Long> failuresByReason,
    AverageMetricsDTO averageMetrics
) {}

public record AverageMetricsDTO(
    Double averageDeliveryTimeMinutes,
    Double averageRetriesPerMessage,
    Double bounceRate
) {}

public record StatisticsFilterDTO(
    LocalDateTime startDate,
    LocalDateTime endDate,
    List<UUID> templateIds,
    List<MessageType> types,
    String groupBy // DAY, WEEK, MONTH, TEMPLATE, STATUS
) {}

public record MessageRetryRequest(
    UUID messageId,
    String reason,
    Boolean forceRetry // Ignorar maxRetries
) {}

public record MessageRetryResponse(
    UUID messageId,
    Boolean retried,
    String message,
    MessageStatus newStatus,
    Integer retryCount
) {}

public record LogExportRequest(
    MessageFilterDTO filter,
    ExportFormat format, // JSON, CSV, XML
    List<String> fields // Campos a incluir
) {}

public enum ExportFormat {
    JSON, CSV, XML, EXCEL
}
```

### 2. Message Tracking Service

```java
@Service
@Transactional(readOnly = true)
public class MessageTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageTrackingService.class);
    
    private final MessageRepository messageRepository;
    private final MessageRecipientRepository recipientRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final EmailServiceImpl emailService;
    private final MessageMapper messageMapper;
    
    /**
     * Obtiene el estado detallado de un mensaje
     */
    public MessageStatusDTO getMessageStatus(UUID messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new MessageNotFoundException("Mensaje no encontrado"));
        
        return messageMapper.toStatusDTO(message);
    }
    
    /**
     * Lista mensajes con filtros y paginación
     */
    public Page<MessageListDTO> listMessages(MessageFilterDTO filter, Pageable pageable) {
        
        Specification<Message> spec = buildSpecification(filter);
        
        return messageRepository.findAll(spec, pageable)
            .map(messageMapper::toListDTO);
    }
    
    /**
     * Obtiene estadísticas de entrega
     */
    public DeliveryStatisticsDTO getDeliveryStatistics(StatisticsFilterDTO filter) {
        
        LocalDateTime startDate = filter.startDate() != null ? 
            filter.startDate() : LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = filter.endDate() != null ? 
            filter.endDate() : LocalDateTime.now();
        
        // Estadísticas básicas
        Long totalMessages = messageRepository.countByCreatedAtBetween(startDate, endDate);
        Long sentMessages = messageRepository.countByStatusAndSentAtBetween(
            MessageStatus.SENT, startDate, endDate);
        Long deliveredMessages = messageRepository.countByStatusAndDeliveredAtBetween(
            MessageStatus.DELIVERED, startDate, endDate);
        Long failedMessages = messageRepository.countByStatusAndFailedAtBetween(
            MessageStatus.FAILED, startDate, endDate);
        Long pendingMessages = messageRepository.countByStatus(MessageStatus.PENDING);
        Long cancelledMessages = messageRepository.countByStatus(MessageStatus.CANCELLED);
        
        // Calcular tasas
        Double successRate = totalMessages > 0 ? 
            (sentMessages + deliveredMessages) * 100.0 / totalMessages : 0.0;
        Double failureRate = totalMessages > 0 ? 
            failedMessages * 100.0 / totalMessages : 0.0;
        
        // Mensajes por plantilla
        Map<String, Long> messagesByTemplate = messageRepository
            .countByTemplateGrouped(startDate, endDate);
        
        // Mensajes por estado
        Map<String, Long> messagesByStatus = messageRepository
            .countByStatusGrouped(startDate, endDate);
        
        // Mensajes por fecha
        Map<LocalDate, Long> messagesByDate = messageRepository
            .countByDateGrouped(startDate, endDate);
        
        // Fallos por razón
        Map<String, Long> failuresByReason = messageRepository
            .countFailuresByReason(startDate, endDate);
        
        // Métricas promedio
        AverageMetricsDTO averageMetrics = calculateAverageMetrics(startDate, endDate);
        
        return new DeliveryStatisticsDTO(
            totalMessages,
            sentMessages,
            deliveredMessages,
            failedMessages,
            pendingMessages,
            cancelledMessages,
            successRate,
            failureRate,
            messagesByTemplate,
            messagesByStatus,
            messagesByDate,
            failuresByReason,
            averageMetrics
        );
    }
    
    /**
     * Reintenta el envío de un mensaje fallido
     */
    @Transactional
    public MessageRetryResponse retryFailedMessage(MessageRetryRequest request) {
        
        Message message = messageRepository.findById(request.messageId())
            .orElseThrow(() -> new MessageNotFoundException("Mensaje no encontrado"));
        
        // Validar que el mensaje puede ser reintentado
        if (message.getStatus() != MessageStatus.FAILED) {
            return new MessageRetryResponse(
                message.getId(),
                false,
                "El mensaje no está en estado FAILED",
                message.getStatus(),
                message.getRetryCount()
            );
        }
        
        if (!request.forceRetry() && message.getRetryCount() >= message.getMaxRetries()) {
            return new MessageRetryResponse(
                message.getId(),
                false,
                "Se alcanzó el máximo de reintentos",
                message.getStatus(),
                message.getRetryCount()
            );
        }
        
        // Incrementar contador de reintentos
        message.setRetryCount(message.getRetryCount() + 1);
        message.setStatus(MessageStatus.PENDING);
        message.setErrorMessage(null);
        message.setUpdatedAt(LocalDateTime.now());
        
        // Agregar log de reintento
        DeliveryLog log = new DeliveryLog();
        log.setMessage(message);
        log.setLevel(LogLevel.INFO);
        log.setEvent("Reintento de envío");
        log.setDetails("Reintento #" + message.getRetryCount() + ": " + request.reason());
        log.setCreatedAt(LocalDateTime.now());
        message.getDeliveryLogs().add(log);
        
        messageRepository.save(message);
        
        // Intentar reenviar
        try {
            SendEmailRequest emailRequest = reconstructEmailRequest(message);
            emailService.sendMail(emailRequest);
            
            return new MessageRetryResponse(
                message.getId(),
                true,
                "Mensaje reenviado exitosamente",
                MessageStatus.SENT,
                message.getRetryCount()
            );
            
        } catch (Exception e) {
            logger.error("Error al reintentar envío de mensaje", e);
            message.setStatus(MessageStatus.FAILED);
            message.setErrorMessage("Reintento fallido: " + e.getMessage());
            messageRepository.save(message);
            
            return new MessageRetryResponse(
                message.getId(),
                false,
                "Reintento fallido: " + e.getMessage(),
                MessageStatus.FAILED,
                message.getRetryCount()
            );
        }
    }
    
    /**
     * Reintenta todos los mensajes fallidos que cumplen con los filtros
     */
    @Transactional
    public BatchRetryResponse retryFailedMessages(MessageFilterDTO filter) {
        
        filter = new MessageFilterDTO(
            filter.templateIds(),
            List.of(MessageStatus.FAILED),
            filter.types(),
            filter.startDate(),
            filter.endDate(),
            filter.recipientEmail(),
            filter.subject(),
            true,
            null
        );
        
        List<Message> failedMessages = messageRepository.findAll(buildSpecification(filter));
        
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Message message : failedMessages) {
            if (message.getRetryCount() < message.getMaxRetries()) {
                MessageRetryRequest retryRequest = new MessageRetryRequest(
                    message.getId(),
                    "Reintento masivo",
                    false
                );
                
                MessageRetryResponse response = retryFailedMessage(retryRequest);
                
                if (response.retried()) {
                    successCount++;
                } else {
                    failedCount++;
                    errors.add(message.getId() + ": " + response.message());
                }
            }
        }
        
        return new BatchRetryResponse(
            failedMessages.size(),
            successCount,
            failedCount,
            errors
        );
    }
    
    /**
     * Exporta logs de mensajes en el formato especificado
     */
    public byte[] exportLogs(LogExportRequest request) {
        
        List<Message> messages = messageRepository.findAll(
            buildSpecification(request.filter())
        );
        
        return switch (request.format()) {
            case JSON -> exportToJson(messages, request.fields());
            case CSV -> exportToCsv(messages, request.fields());
            case XML -> exportToXml(messages, request.fields());
            case EXCEL -> exportToExcel(messages, request.fields());
        };
    }
    
    /**
     * Cancela un mensaje programado
     */
    @Transactional
    public void cancelScheduledMessage(UUID messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new MessageNotFoundException("Mensaje no encontrado"));
        
        if (message.getStatus() != MessageStatus.PENDING) {
            throw new InvalidOperationException(
                "Solo se pueden cancelar mensajes en estado PENDING"
            );
        }
        
        message.setStatus(MessageStatus.CANCELLED);
        message.setUpdatedAt(LocalDateTime.now());
        
        DeliveryLog log = new DeliveryLog();
        log.setMessage(message);
        log.setLevel(LogLevel.INFO);
        log.setEvent("Mensaje cancelado");
        log.setDetails("Mensaje cancelado por usuario");
        log.setCreatedAt(LocalDateTime.now());
        message.getDeliveryLogs().add(log);
        
        messageRepository.save(message);
    }
    
    // ============================================
    // MÉTODOS PRIVADOS
    // ============================================
    
    private Specification<Message> buildSpecification(MessageFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.templateIds() != null && !filter.templateIds().isEmpty()) {
                predicates.add(root.get("template").get("id").in(filter.templateIds()));
            }
            
            if (filter.statuses() != null && !filter.statuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.statuses()));
            }
            
            if (filter.types() != null && !filter.types().isEmpty()) {
                predicates.add(root.get("type").in(filter.types()));
            }
            
            if (filter.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                    root.get("createdAt"), filter.startDate()));
            }
            
            if (filter.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.get("createdAt"), filter.endDate()));
            }
            
            if (filter.subject() != null) {
                predicates.add(cb.like(
                    cb.lower(root.get("subject")), 
                    "%" + filter.subject().toLowerCase() + "%"));
            }
            
            if (filter.recipientEmail() != null) {
                Join<Message, MessageRecipient> recipients = root.join("recipients");
                predicates.add(cb.like(
                    cb.lower(recipients.get("address")), 
                    "%" + filter.recipientEmail().toLowerCase() + "%"));
            }
            
            if (Boolean.TRUE.equals(filter.failedOnly())) {
                predicates.add(cb.equal(root.get("status"), MessageStatus.FAILED));
            }
            
            if (Boolean.TRUE.equals(filter.pendingRetry())) {
                predicates.add(cb.equal(root.get("status"), MessageStatus.FAILED));
                predicates.add(cb.lessThan(
                    root.get("retryCount"), root.get("maxRetries")));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    private AverageMetricsDTO calculateAverageMetrics(
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        
        // Tiempo promedio de entrega (en minutos)
        Double avgDeliveryTime = messageRepository
            .calculateAverageDeliveryTime(startDate, endDate);
        
        // Promedio de reintentos por mensaje
        Double avgRetries = messageRepository
            .calculateAverageRetries(startDate, endDate);
        
        // Tasa de rebote
        Long totalRecipients = recipientRepository
            .countByCreatedAtBetween(startDate, endDate);
        Long bouncedRecipients = recipientRepository
            .countByStatusAndBouncedAtBetween(
                DeliveryStatus.BOUNCED, startDate, endDate);
        
        Double bounceRate = totalRecipients > 0 ? 
            bouncedRecipients * 100.0 / totalRecipients : 0.0;
        
        return new AverageMetricsDTO(
            avgDeliveryTime != null ? avgDeliveryTime : 0.0,
            avgRetries != null ? avgRetries : 0.0,
            bounceRate
        );
    }
    
    private SendEmailRequest reconstructEmailRequest(Message message) {
        List<EmailContact> to = message.getRecipients().stream()
            .filter(r -> r.getType() == RecipientType.TO)
            .map(r -> new EmailContact(r.getAddress(), r.getName()))
            .toList();
        
        List<EmailContact> cc = message.getRecipients().stream()
            .filter(r -> r.getType() == RecipientType.CC)
            .map(r -> new EmailContact(r.getAddress(), r.getName()))
            .toList();
        
        return new SendEmailRequest(
            message.getTemplate().getCode(),
            message.getFromAddress(),
            to,
            cc,
            message.getSubject(),
            message.getBody(),
            null,
            Map.of() // Los parámetros ya fueron procesados
        );
    }
    
    private byte[] exportToJson(List<Message> messages, List<String> fields) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            List<Map<String, Object>> data = messages.stream()
                .map(msg -> extractFields(msg, fields))
                .toList();
            
            return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(data);
                
        } catch (JsonProcessingException e) {
            logger.error("Error al exportar a JSON", e);
            throw new ExportException("Error al exportar logs a JSON", e);
        }
    }
    
    private byte[] exportToCsv(List<Message> messages, List<String> fields) {
        // Implementación de exportación a CSV
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append(String.join(",", fields)).append("\n");
        
        // Rows
        for (Message message : messages) {
            Map<String, Object> data = extractFields(message, fields);
            csv.append(fields.stream()
                .map(field -> escapeCsv(String.valueOf(data.get(field))))
                .collect(Collectors.joining(",")))
                .append("\n");
        }
        
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private byte[] exportToXml(List<Message> messages, List<String> fields) {
        // Implementación básica de exportación a XML
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<messages>\n");
        
        for (Message message : messages) {
            xml.append("  <message>\n");
            Map<String, Object> data = extractFields(message, fields);
            for (String field : fields) {
                xml.append("    <").append(field).append(">")
                   .append(escapeXml(String.valueOf(data.get(field))))
                   .append("</").append(field).append(">\n");
            }
            xml.append("  </message>\n");
        }
        
        xml.append("</messages>");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private byte[] exportToExcel(List<Message> messages, List<String> fields) {
        // Requiere Apache POI
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Messages");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < fields.size(); i++) {
                headerRow.createCell(i).setCellValue(fields.get(i));
            }
            
            // Data rows
            int rowNum = 1;
            for (Message message : messages) {
                Row row = sheet.createRow(rowNum++);
                Map<String, Object> data = extractFields(message, fields);
                for (int i = 0; i < fields.size(); i++) {
                    row.createCell(i).setCellValue(
                        String.valueOf(data.get(fields.get(i))));
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < fields.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error al exportar a Excel", e);
            throw new ExportException("Error al exportar logs a Excel", e);
        }
    }
    
    private Map<String, Object> extractFields(Message message, List<String> fields) {
        Map<String, Object> data = new HashMap<>();
        
        for (String field : fields) {
            switch (field) {
                case "id" -> data.put(field, message.getId());
                case "templateCode" -> data.put(field, message.getTemplate().getCode());
                case "status" -> data.put(field, message.getStatus());
                case "subject" -> data.put(field, message.getSubject());
                case "fromAddress" -> data.put(field, message.getFromAddress());
                case "sentAt" -> data.put(field, message.getSentAt());
                case "errorMessage" -> data.put(field, message.getErrorMessage());
                case "retryCount" -> data.put(field, message.getRetryCount());
                default -> data.put(field, null);
            }
        }
        
        return data;
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String escapeXml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}

record BatchRetryResponse(
    int totalMessages,
    int successCount,
    int failedCount,
    List<String> errors
) {}
```

### 3. Message Tracking Controller

```java
@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Message Tracking", description = "API para seguimiento y monitoreo de mensajes")
@CrossOrigin(origins = "*")
public class MessageTrackingController {
    
    private final MessageTrackingService trackingService;
    
    @GetMapping("/{id}")
    @Operation(summary = "Obtener estado de mensaje")
    public ResponseEntity<MessageStatusDTO> getMessageStatus(@PathVariable UUID id) {
        MessageStatusDTO status = trackingService.getMessageStatus(id);
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Buscar mensajes con filtros")
    public ResponseEntity<Page<MessageListDTO>> searchMessages(
            @RequestBody MessageFilterDTO filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        Page<MessageListDTO> messages = trackingService.listMessages(filter, pageable);
        return ResponseEntity.ok(messages);
    }
    
    @PostMapping("/statistics")
    @Operation(summary = "Obtener estadísticas de entrega")
    public ResponseEntity<DeliveryStatisticsDTO> getStatistics(
            @RequestBody StatisticsFilterDTO filter) {
        
        DeliveryStatisticsDTO stats = trackingService.getDeliveryStatistics(filter);
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/{id}/retry")
    @Operation(summary = "Reintentar envío de mensaje fallido")
    public ResponseEntity<MessageRetryResponse> retryMessage(
            @PathVariable UUID id,
            @RequestBody MessageRetryRequest request) {
        
        MessageRetryResponse response = trackingService.retryFailedMessage(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/retry-batch")
    @Operation(summary = "Reintentar múltiples mensajes fallidos")
    public ResponseEntity<BatchRetryResponse> retryMessages(
            @RequestBody MessageFilterDTO filter) {
        
        BatchRetryResponse response = trackingService.retryFailedMessages(filter);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/export")
    @Operation(summary = "Exportar logs de mensajes")
    public ResponseEntity<byte[]> exportLogs(@RequestBody LogExportRequest request) {
        
        byte[] data = trackingService.exportLogs(request);
        
        String filename = "messages-export-" + LocalDateTime.now() + 
                         "." + request.format().name().toLowerCase();
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(getMediaType(request.format()))
            .body(data);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar mensaje programado")
    public ResponseEntity<Void> cancelMessage(@PathVariable UUID id) {
        trackingService.cancelScheduledMessage(id);
        return ResponseEntity.noContent().build();
    }
    
    private MediaType getMediaType(ExportFormat format) {
        return switch (format) {
            case JSON -> MediaType.APPLICATION_JSON;
            case CSV -> MediaType.parseMediaType("text/csv");
            case XML -> MediaType.APPLICATION_XML;
            case EXCEL -> MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
    }
}
```

## Dependencias
- **Dependencias de Tareas:**
  - MERC-TASK-001 (Modelo de Datos) - DEBE estar completada
  - MERC-TASK-003 (Integración Motor) - DEBE estar completada

- **Dependencias Técnicas:**
  - Spring Data JPA (Specifications)
  - Jackson (para JSON)
  - Apache POI (para Excel)
  - Commons CSV (opcional)

## Criterios de Aceptación

1. **CA-01**: Consultas de estado funcionan
   - ✅ Obtener estado detallado de mensaje
   - ✅ Lista de mensajes con filtros
   - ✅ Búsqueda por múltiples criterios
   - ✅ Paginación funciona correctamente

2. **CA-02**: Estadísticas calculadas correctamente
   - ✅ Totales por estado
   - ✅ Tasas de éxito y fallo
   - ✅ Agrupación por plantilla
   - ✅ Agrupación por fecha
   - ✅ Métricas promedio

3. **CA-03**: Sistema de reintentos funciona
   - ✅ Reintento individual de mensajes
   - ✅ Reintento masivo con filtros
   - ✅ Respeta límite de reintentos
   - ✅ Registra logs de reintentos
   - ✅ Opción de forzar reintento

4. **CA-04**: Exportación de logs funciona
   - ✅ Exportación a JSON
   - ✅ Exportación a CSV
   - ✅ Exportación a XML
   - ✅ Exportación a Excel
   - ✅ Selección de campos

5. **CA-05**: Cancelación de mensajes funciona
   - ✅ Cancelar mensajes PENDING
   - ✅ No permite cancelar mensajes enviados
   - ✅ Registra log de cancelación

6. **CA-06**: API REST completa
   - ✅ Todos los endpoints funcionan
   - ✅ Documentación OpenAPI
   - ✅ Validación de entrada
   - ✅ Manejo de errores

7. **CA-07**: Tests completos
   - ✅ Tests unitarios de servicio
   - ✅ Tests de integración
   - ✅ Tests de filtros
   - ✅ Tests de exportación
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
- [x] Performance testeado (queries optimizadas)
- [x] Índices de BD apropiados
- [x] Logs apropiados
- [x] Exportaciones testeadas
- [x] Cambios commiteados y pusheados
- [x] Pull Request aprobado

## Estimación
- **Complejidad**: Alta
- **Story Points**: 13
- **Tiempo Estimado**: 20-24 horas
- **Desarrolladores Requeridos**: 1 Backend Developer

## Riesgos
1. **Riesgo**: Performance con grandes volúmenes de datos
   - **Mitigación**: Índices apropiados, paginación, límites de consulta

2. **Riesgo**: Complejidad de filtros dinámicos
   - **Mitigación**: Usar JPA Specifications

3. **Riesgo**: Exportaciones de archivos grandes
   - **Mitigación**: Límites de registros, streaming para archivos grandes

## Notas Adicionales
- Considerar agregar caché para estadísticas
- Implementar paginación en exportaciones grandes
- Agregar límites de tiempo para consultas
- Documentar mejores prácticas de consulta

## Referencias
- Spring Data JPA Specifications: https://spring.io/blog/2011/04/26/advanced-spring-data-jpa-specifications-and-querydsl/
- Apache POI: https://poi.apache.org/
- CSV Export: https://commons.apache.org/proper/commons-csv/
