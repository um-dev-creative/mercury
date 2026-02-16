# Tarea 3: Integración del Motor de Plantillas con Gestión Dinámica

## Identificador
**MERC-TASK-003**

## Título
Refactorización e Integración del Servicio de Email con Plantillas Dinámicas

## Descripción
Refactorizar el servicio actual de envío de emails (EmailServiceImpl) para que utilice el sistema de gestión de plantillas dinámicas implementado en las tareas anteriores, en lugar de cargar plantillas directamente del sistema de archivos.

## Contexto
Actualmente, EmailServiceImpl carga plantillas FreeMarker mediante `freemarkerConfig.getTemplate(mail.templateId())` directamente desde el sistema de archivos. Se necesita modificar este comportamiento para:
- Cargar plantillas desde la base de datos
- Usar el versionado de plantillas
- Validar variables antes del procesamiento
- Mejorar el manejo de errores
- Almacenar historial de mensajes enviados

## Objetivos
1. Refactorizar EmailServiceImpl para usar TemplateService
2. Implementar cargador dinámico de plantillas FreeMarker
3. Crear modelo de datos para historial de mensajes
4. Implementar validación de variables de plantillas
5. Mejorar manejo de errores y logging
6. Crear tests de integración

## Alcance

### Incluye:
- Refactorización de EmailServiceImpl
- Nuevo servicio TemplateProcessorService
- Entidades para historial de mensajes (Message, MessageRecipient, DeliveryLog)
- Validador de variables de plantillas
- FreeMarker template loader personalizado
- Mejoras en manejo de excepciones
- Tests de integración end-to-end

### No Incluye:
- Envío por otros canales (Telegram, SMS)
- Procesamiento asíncrono (siguiente tarea)
- Dashboard de monitoreo
- Sistema de webhooks

## Detalle Técnico

### 1. Modelo de Datos para Mensajes

#### Entidad Message
```java
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;
    
    @Column(name = "template_version", nullable = false)
    private Integer templateVersion;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type; // EMAIL, TELEGRAM, SMS, WEBHOOK
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status; // PENDING, PROCESSING, SENT, DELIVERED, FAILED, CANCELLED
    
    @Column(nullable = false, length = 500)
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Column(name = "from_address", length = 200)
    private String fromAddress;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(length = 1000)
    private String errorMessage;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageRecipient> recipients = new ArrayList<>();
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<DeliveryLog> deliveryLogs = new ArrayList<>();
    
    // Getters, setters
}
```

#### Entidad MessageRecipient
```java
@Entity
@Table(name = "message_recipients")
public class MessageRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipientType type; // TO, CC, BCC
    
    @Column(nullable = false, length = 200)
    private String address;
    
    @Column(length = 200)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status; // PENDING, SENT, DELIVERED, BOUNCED, FAILED
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "bounced_at")
    private LocalDateTime bouncedAt;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    // Getters, setters
}
```

#### Entidad DeliveryLog
```java
@Entity
@Table(name = "delivery_logs")
public class DeliveryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogLevel level; // INFO, WARNING, ERROR
    
    @Column(nullable = false, length = 200)
    private String event;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Getters, setters
}
```

#### Enums
```java
public enum MessageType {
    EMAIL, TELEGRAM, SMS, WEBHOOK
}

public enum MessageStatus {
    PENDING, PROCESSING, SENT, DELIVERED, FAILED, CANCELLED
}

public enum RecipientType {
    TO, CC, BCC
}

public enum DeliveryStatus {
    PENDING, SENT, DELIVERED, BOUNCED, FAILED
}

public enum LogLevel {
    INFO, WARNING, ERROR
}
```

### 2. Template Processor Service

```java
@Service
public class TemplateProcessorService {
    
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final Configuration freemarkerConfig;
    
    /**
     * Procesa una plantilla con los parámetros proporcionados
     */
    public String processTemplate(UUID templateId, Map<String, Object> params) 
            throws TemplateProcessingException {
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada: " + templateId));
        
        if (!template.getActive()) {
            throw new TemplateInactiveException("La plantilla no está activa: " + template.getCode());
        }
        
        // Obtener versión actual
        TemplateVersion currentVersion = versionRepository.findByTemplateAndCurrentTrue(template)
            .orElseThrow(() -> new TemplateVersionNotFoundException("No hay versión actual para la plantilla"));
        
        // Validar variables requeridas
        validateRequiredVariables(template, params);
        
        // Procesar plantilla
        try {
            return processTemplateContent(currentVersion.getContent(), params);
        } catch (Exception e) {
            throw new TemplateProcessingException("Error al procesar plantilla: " + e.getMessage(), e);
        }
    }
    
    /**
     * Procesa una plantilla por código
     */
    public String processTemplateByCode(String templateCode, Map<String, Object> params) 
            throws TemplateProcessingException {
        
        Template template = templateRepository.findByCode(templateCode)
            .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada: " + templateCode));
        
        return processTemplate(template.getId(), params);
    }
    
    /**
     * Valida que todas las variables requeridas estén presentes
     */
    private void validateRequiredVariables(Template template, Map<String, Object> params) {
        List<String> missingVariables = template.getVariables().stream()
            .filter(TemplateVariable::getRequired)
            .map(TemplateVariable::getName)
            .filter(varName -> !params.containsKey(varName))
            .toList();
        
        if (!missingVariables.isEmpty()) {
            throw new MissingVariableException(
                "Faltan variables requeridas: " + String.join(", ", missingVariables)
            );
        }
    }
    
    /**
     * Procesa el contenido de la plantilla con FreeMarker
     */
    private String processTemplateContent(String content, Map<String, Object> params) 
            throws IOException, TemplateException {
        
        // Crear template temporal de FreeMarker
        freemarker.template.Template fmTemplate = new freemarker.template.Template(
            "temp",
            new StringReader(content),
            freemarkerConfig
        );
        
        // Procesar con los parámetros
        StringWriter writer = new StringWriter();
        fmTemplate.process(params, writer);
        
        return writer.toString();
    }
    
    /**
     * Valida la sintaxis de una plantilla sin procesarla
     */
    public TemplateValidationResult validateTemplate(String content) {
        try {
            freemarker.template.Template fmTemplate = new freemarker.template.Template(
                "validation",
                new StringReader(content),
                freemarkerConfig
            );
            
            // Intentar obtener variables
            Set<String> variables = extractVariables(content);
            
            return new TemplateValidationResult(true, "Plantilla válida", variables);
            
        } catch (Exception e) {
            return new TemplateValidationResult(
                false, 
                "Error de sintaxis: " + e.getMessage(),
                Collections.emptySet()
            );
        }
    }
    
    /**
     * Extrae las variables de una plantilla FreeMarker
     */
    private Set<String> extractVariables(String content) {
        Set<String> variables = new HashSet<>();
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String var = matcher.group(1);
            // Extraer solo el nombre de la variable (antes del primer punto o corchete)
            String varName = var.split("[.\\[]")[0];
            variables.add(varName);
        }
        
        return variables;
    }
}

record TemplateValidationResult(boolean valid, String message, Set<String> variables) {}
```

### 3. Refactorización de EmailServiceImpl

```java
@Service
public class EmailServiceImpl implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    private final JavaMailSender javaMailSender;
    private final TemplateProcessorService templateProcessor;
    private final MessageRepository messageRepository;
    private final MessageRecipientRepository recipientRepository;
    
    @Value("${mercury.mail.from.default}")
    private String defaultFromAddress;
    
    @Override
    @Transactional
    public ResponseEntity<SendEmailResponse> sendMail(SendEmailRequest request) {
        
        // Crear registro de mensaje
        Message message = createMessage(request);
        message = messageRepository.save(message);
        
        try {
            // Procesar plantilla
            String processedBody = templateProcessor.processTemplateByCode(
                request.templateId(),
                request.params()
            );
            
            // Actualizar estado
            message.setStatus(MessageStatus.PROCESSING);
            message.setBody(processedBody);
            message.setUpdatedAt(LocalDateTime.now());
            messageRepository.save(message);
            
            // Crear y enviar email
            MimeMessage mimeMessage = createMimeMessage(request, processedBody);
            javaMailSender.send(mimeMessage);
            
            // Actualizar estado a enviado
            message.setStatus(MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            message.setUpdatedAt(LocalDateTime.now());
            
            // Actualizar destinatarios
            message.getRecipients().forEach(recipient -> {
                recipient.setStatus(DeliveryStatus.SENT);
            });
            
            messageRepository.save(message);
            
            // Agregar log de éxito
            addDeliveryLog(message, LogLevel.INFO, "Email enviado exitosamente", null);
            
            logger.info("Email enviado exitosamente. Message ID: {}", message.getId());
            
            return ResponseEntity.ok(createResponse(message));
            
        } catch (TemplateProcessingException e) {
            logger.error("Error al procesar plantilla", e);
            handleMessageFailure(message, "Error al procesar plantilla: " + e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(message, e));
            
        } catch (MessagingException e) {
            logger.error("Error al enviar email", e);
            handleMessageFailure(message, "Error al enviar email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(message, e));
        }
    }
    
    private Message createMessage(SendEmailRequest request) {
        Message message = new Message();
        message.setType(MessageType.EMAIL);
        message.setStatus(MessageStatus.PENDING);
        message.setSubject(request.subject());
        message.setFromAddress(request.from() != null ? request.from() : defaultFromAddress);
        message.setScheduledAt(request.sendDate());
        message.setRetryCount(0);
        message.setMaxRetries(3);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        
        // Agregar destinatarios TO
        request.to().forEach(contact -> {
            MessageRecipient recipient = new MessageRecipient();
            recipient.setMessage(message);
            recipient.setType(RecipientType.TO);
            recipient.setAddress(contact.email());
            recipient.setName(contact.name());
            recipient.setStatus(DeliveryStatus.PENDING);
            message.getRecipients().add(recipient);
        });
        
        // Agregar destinatarios CC
        if (request.cc() != null && !request.cc().isEmpty()) {
            request.cc().forEach(contact -> {
                MessageRecipient recipient = new MessageRecipient();
                recipient.setMessage(message);
                recipient.setType(RecipientType.CC);
                recipient.setAddress(contact.email());
                recipient.setName(contact.name());
                recipient.setStatus(DeliveryStatus.PENDING);
                message.getRecipients().add(recipient);
            });
        }
        
        return message;
    }
    
    private MimeMessage createMimeMessage(SendEmailRequest request, String processedBody) 
            throws MessagingException {
        
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
            mimeMessage, 
            MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
            StandardCharsets.UTF_8.name()
        );
        
        helper.setFrom(request.from() != null ? request.from() : defaultFromAddress);
        helper.setSubject(request.subject());
        helper.setText(processedBody, true);
        
        // Destinatarios TO
        String[] toAddresses = request.to().stream()
            .map(EmailContact::email)
            .toArray(String[]::new);
        helper.setTo(toAddresses);
        
        // Destinatarios CC
        if (request.cc() != null && !request.cc().isEmpty()) {
            String[] ccAddresses = request.cc().stream()
                .map(EmailContact::email)
                .toArray(String[]::new);
            helper.setCc(ccAddresses);
        }
        
        return mimeMessage;
    }
    
    private void handleMessageFailure(Message message, String errorMessage) {
        message.setStatus(MessageStatus.FAILED);
        message.setFailedAt(LocalDateTime.now());
        message.setErrorMessage(errorMessage);
        message.setUpdatedAt(LocalDateTime.now());
        
        message.getRecipients().forEach(recipient -> {
            recipient.setStatus(DeliveryStatus.FAILED);
            recipient.setErrorMessage(errorMessage);
        });
        
        messageRepository.save(message);
        addDeliveryLog(message, LogLevel.ERROR, "Error al enviar mensaje", errorMessage);
    }
    
    private void addDeliveryLog(Message message, LogLevel level, String event, String details) {
        DeliveryLog log = new DeliveryLog();
        log.setMessage(message);
        log.setLevel(level);
        log.setEvent(event);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        
        message.getDeliveryLogs().add(log);
        messageRepository.save(message);
    }
    
    private SendEmailResponse createResponse(Message message) {
        return new SendEmailResponse(
            message.getId(),
            message.getStatus().name(),
            message.getBody()
        );
    }
    
    private SendEmailResponse createErrorResponse(Message message, Exception e) {
        return new SendEmailResponse(
            message.getId(),
            message.getStatus().name(),
            "Error: " + e.getMessage()
        );
    }
}
```

### 4. Scripts de Migración de Base de Datos

```sql
-- V2__create_message_tables.sql

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES templates(id),
    template_version INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT,
    from_address VARCHAR(200),
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message VARCHAR(1000),
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

CREATE TABLE message_recipients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    type VARCHAR(10) NOT NULL,
    address VARCHAR(200) NOT NULL,
    name VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    delivered_at TIMESTAMP,
    bounced_at TIMESTAMP,
    error_message VARCHAR(500)
);

CREATE TABLE delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    level VARCHAR(20) NOT NULL,
    event VARCHAR(200) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_template ON messages(template_id);
CREATE INDEX idx_messages_status ON messages(status);
CREATE INDEX idx_messages_type ON messages(type);
CREATE INDEX idx_messages_created ON messages(created_at);
CREATE INDEX idx_message_recipients_message ON message_recipients(message_id);
CREATE INDEX idx_message_recipients_address ON message_recipients(address);
CREATE INDEX idx_delivery_logs_message ON delivery_logs(message_id);
CREATE INDEX idx_delivery_logs_level ON delivery_logs(level);
```

## Dependencias
- **Dependencias de Tareas:**
  - MERC-TASK-001 (Modelo de Datos) - DEBE estar completada
  - MERC-TASK-002 (Servicios CRUD) - DEBE estar completada

- **Dependencias Técnicas:**
  - FreeMarker 3.3.2
  - Spring Boot Mail

## Criterios de Aceptación

1. **CA-01**: EmailServiceImpl refactorizado correctamente
   - ✅ Usa TemplateProcessorService para procesar plantillas
   - ✅ Carga plantillas desde base de datos
   - ✅ Crea registros de Message para cada envío
   - ✅ Actualiza estados correctamente
   - ✅ Maneja errores apropiadamente

2. **CA-02**: TemplateProcessorService funciona correctamente
   - ✅ Procesa plantillas FreeMarker desde string
   - ✅ Valida variables requeridas
   - ✅ Valida sintaxis de plantillas
   - ✅ Extrae variables de plantillas
   - ✅ Maneja errores de procesamiento

3. **CA-03**: Modelo de datos de mensajes implementado
   - ✅ Entidades Message, MessageRecipient, DeliveryLog creadas
   - ✅ Relaciones configuradas correctamente
   - ✅ Índices apropiados en base de datos
   - ✅ Repositorios funcionan correctamente

4. **CA-04**: Validación de variables funciona
   - ✅ Detecta variables faltantes
   - ✅ Lanza excepción descriptiva
   - ✅ Valida variables opcionales vs requeridas

5. **CA-05**: Logging y auditoría implementados
   - ✅ DeliveryLog registra eventos importantes
   - ✅ Logs con niveles apropiados (INFO, WARNING, ERROR)
   - ✅ Información suficiente para troubleshooting
   - ✅ No expone información sensible

6. **CA-06**: Tests de integración pasan
   - ✅ Test end-to-end de envío de email
   - ✅ Test de procesamiento de plantillas
   - ✅ Test de validación de variables
   - ✅ Test de manejo de errores
   - ✅ Cobertura > 80%

## Definition of Done (DoD)

- [x] Código compilado sin errores
- [x] Todos los tests pasan
- [x] Cobertura > 80%
- [x] Code review aprobado
- [x] Sin violaciones PMD
- [x] JavaDoc completo
- [x] Migraciones de BD ejecutadas
- [x] Tests de integración end-to-end funcionan
- [x] Plantillas antiguas migradas o marcadas obsoletas
- [x] Documentación actualizada
- [x] Logs verificados en ambiente de desarrollo
- [x] Performance testeado (no regresión)
- [x] Cambios commiteados y pusheados
- [x] Pull Request aprobado

## Estimación
- **Complejidad**: Alta
- **Story Points**: 13
- **Tiempo Estimado**: 20-24 horas
- **Desarrolladores Requeridos**: 1 Backend Developer

## Riesgos
1. **Riesgo**: Migración de plantillas existentes
   - **Mitigación**: Script de migración + período de coexistencia

2. **Riesgo**: Performance al cargar desde BD vs filesystem
   - **Mitigación**: Implementar caché de plantillas

3. **Riesgo**: Complejidad de FreeMarker desde strings
   - **Mitigación**: Usar StringReader (ya probado en FreeMarker)

## Notas Adicionales
- Considerar implementar caché de plantillas procesadas
- Evaluar necesidad de sandbox para plantillas FreeMarker
- Documentar límites de complejidad de plantillas
- Considerar timeout para procesamiento de plantillas

## Referencias
- FreeMarker Programmer's Guide: https://freemarker.apache.org/docs/pgui.html
- Spring JavaMailSender: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/mail/javamail/JavaMailSender.html
- FreeMarker Security: https://freemarker.apache.org/docs/app_faq.html#faq_template_uploading_security
