# Tarea 6: Sistema de Procesamiento Asíncrono y Colas de Mensajes

## Identificador
**MERC-TASK-006**

## Título
Implementación de Procesamiento Asíncrono con Sistema de Colas para Envío de Mensajes

## Descripción
Implementar un sistema de procesamiento asíncrono utilizando Spring AMQP con RabbitMQ (o Kafka) para manejar el envío de mensajes de forma escalable, desacoplada y con capacidad de procesamiento en paralelo.

## Contexto
Actualmente, el envío de mensajes es síncrono, lo que puede causar timeouts en la API cuando hay grandes volúmenes de envíos. Se necesita un sistema asíncrono que permita:
- Encolar mensajes para procesamiento posterior
- Procesar mensajes en paralelo con múltiples workers
- Controlar la tasa de envío (rate limiting)
- Manejar fallos con reintentos automáticos
- Escalar horizontalmente agregando más workers

## Objetivos
1. Configurar RabbitMQ para gestión de colas
2. Implementar productores de mensajes para encolar
3. Implementar consumers/workers para procesar
4. Configurar dead letter queues para fallos
5. Implementar rate limiting
6. Crear sistema de prioridades
7. Implementar monitoreo de colas

## Alcance

### Incluye:
- Configuración de RabbitMQ con exchanges y queues
- Producer service para encolar mensajes
- Consumer/Worker para procesar mensajes
- Dead Letter Queue (DLQ) para mensajes fallidos
- Rate limiting configurable
- Sistema de prioridades
- Métricas y monitoreo de colas
- Tests de integración

### No Incluye:
- Interfaz de administración de colas (usar RabbitMQ Management)
- Procesamiento de eventos en tiempo real (streaming)
- Integración con Kafka (alternativa futura)

## Detalle Técnico

### 1. Configuración de RabbitMQ

```java
@Configuration
public class RabbitMQConfig {
    
    // Exchanges
    public static final String MESSAGE_EXCHANGE = "mercury.message.exchange";
    public static final String DLQ_EXCHANGE = "mercury.dlq.exchange";
    
    // Queues
    public static final String EMAIL_QUEUE = "mercury.email.queue";
    public static final String TELEGRAM_QUEUE = "mercury.telegram.queue";
    public static final String EMAIL_DLQ = "mercury.email.dlq";
    public static final String TELEGRAM_DLQ = "mercury.telegram.dlq";
    
    // Routing Keys
    public static final String EMAIL_ROUTING_KEY = "message.email";
    public static final String TELEGRAM_ROUTING_KEY = "message.telegram";
    
    @Bean
    public TopicExchange messageExchange() {
        return ExchangeBuilder
            .topicExchange(MESSAGE_EXCHANGE)
            .durable(true)
            .build();
    }
    
    @Bean
    public DirectExchange dlqExchange() {
        return ExchangeBuilder
            .directExchange(DLQ_EXCHANGE)
            .durable(true)
            .build();
    }
    
    @Bean
    public Queue emailQueue() {
        return QueueBuilder
            .durable(EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "dlq.email")
            .withArgument("x-max-priority", 10) // Soporte de prioridades
            .build();
    }
    
    @Bean
    public Queue telegramQueue() {
        return QueueBuilder
            .durable(TELEGRAM_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "dlq.telegram")
            .withArgument("x-max-priority", 10)
            .build();
    }
    
    @Bean
    public Queue emailDLQ() {
        return QueueBuilder
            .durable(EMAIL_DLQ)
            .build();
    }
    
    @Bean
    public Queue telegramDLQ() {
        return QueueBuilder
            .durable(TELEGRAM_DLQ)
            .build();
    }
    
    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange messageExchange) {
        return BindingBuilder
            .bind(emailQueue)
            .to(messageExchange)
            .with(EMAIL_ROUTING_KEY);
    }
    
    @Bean
    public Binding telegramBinding(Queue telegramQueue, TopicExchange messageExchange) {
        return BindingBuilder
            .bind(telegramQueue)
            .to(messageExchange)
            .with(TELEGRAM_ROUTING_KEY);
    }
    
    @Bean
    public Binding emailDLQBinding(Queue emailDLQ, DirectExchange dlqExchange) {
        return BindingBuilder
            .bind(emailDLQ)
            .to(dlqExchange)
            .with("dlq.email");
    }
    
    @Bean
    public Binding telegramDLQBinding(Queue telegramDLQ, DirectExchange dlqExchange) {
        return BindingBuilder
            .bind(telegramDLQ)
            .to(dlqExchange)
            .with("dlq.telegram");
    }
    
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return new Jackson2JsonMessageConverter(mapper);
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
```

### 2. DTOs para Mensajes de Cola

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageQueuePayload(
    UUID messageId,
    UUID templateId,
    String templateCode,
    MessageType type,
    Map<String, Object> parameters,
    SendEmailRequest emailRequest,
    TelegramMessageRequest telegramRequest,
    Integer priority,
    LocalDateTime scheduledAt,
    Integer attemptCount,
    String correlationId
) implements Serializable {
    
    public static MessageQueuePayload forEmail(
            UUID messageId,
            SendEmailRequest request,
            Integer priority) {
        
        return new MessageQueuePayload(
            messageId,
            null,
            request.templateId(),
            MessageType.EMAIL,
            request.params(),
            request,
            null,
            priority != null ? priority : 5,
            request.sendDate(),
            0,
            UUID.randomUUID().toString()
        );
    }
    
    public static MessageQueuePayload forTelegram(
            UUID messageId,
            TelegramMessageRequest request,
            Integer priority) {
        
        return new MessageQueuePayload(
            messageId,
            null,
            request.templateCode(),
            MessageType.TELEGRAM,
            request.parameters(),
            null,
            request,
            priority != null ? priority : 5,
            request.scheduledAt(),
            0,
            UUID.randomUUID().toString()
        );
    }
}

public record TelegramMessageRequest(
    String templateCode,
    String chatId,
    Map<String, Object> parameters,
    LocalDateTime scheduledAt,
    Boolean preview
) {}
```

### 3. Message Producer Service

```java
@Service
public class MessageProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);
    
    private final RabbitTemplate rabbitTemplate;
    private final MessageRepository messageRepository;
    
    @Value("${mercury.queue.max-priority:10}")
    private Integer maxPriority;
    
    /**
     * Encola un mensaje de email para procesamiento asíncrono
     */
    public UUID enqueueEmailMessage(SendEmailRequest request, Integer priority) {
        
        // Crear registro de mensaje
        Message message = createMessageRecord(request);
        message.setStatus(MessageStatus.PENDING);
        message = messageRepository.save(message);
        
        // Crear payload para la cola
        MessageQueuePayload payload = MessageQueuePayload.forEmail(
            message.getId(),
            request,
            priority
        );
        
        // Enviar a cola
        sendToQueue(
            RabbitMQConfig.MESSAGE_EXCHANGE,
            RabbitMQConfig.EMAIL_ROUTING_KEY,
            payload,
            priority
        );
        
        logger.info("Email message enqueued: {} with priority {}", 
            message.getId(), priority);
        
        return message.getId();
    }
    
    /**
     * Encola un mensaje de Telegram para procesamiento asíncrono
     */
    public UUID enqueueTelegramMessage(TelegramMessageRequest request, Integer priority) {
        
        Message message = createTelegramMessageRecord(request);
        message.setStatus(MessageStatus.PENDING);
        message = messageRepository.save(message);
        
        MessageQueuePayload payload = MessageQueuePayload.forTelegram(
            message.getId(),
            request,
            priority
        );
        
        sendToQueue(
            RabbitMQConfig.MESSAGE_EXCHANGE,
            RabbitMQConfig.TELEGRAM_ROUTING_KEY,
            payload,
            priority
        );
        
        logger.info("Telegram message enqueued: {} with priority {}", 
            message.getId(), priority);
        
        return message.getId();
    }
    
    /**
     * Encola múltiples mensajes en batch
     */
    public List<UUID> enqueueBatch(List<SendEmailRequest> requests, Integer priority) {
        
        List<UUID> messageIds = new ArrayList<>();
        
        for (SendEmailRequest request : requests) {
            try {
                UUID messageId = enqueueEmailMessage(request, priority);
                messageIds.add(messageId);
            } catch (Exception e) {
                logger.error("Error enqueueing message in batch", e);
            }
        }
        
        logger.info("Batch enqueued: {} messages", messageIds.size());
        
        return messageIds;
    }
    
    /**
     * Programa un mensaje para envío futuro
     */
    public UUID scheduleMessage(SendEmailRequest request, LocalDateTime scheduledAt, Integer priority) {
        
        Message message = createMessageRecord(request);
        message.setStatus(MessageStatus.PENDING);
        message.setScheduledAt(scheduledAt);
        message = messageRepository.save(message);
        
        MessageQueuePayload payload = MessageQueuePayload.forEmail(
            message.getId(),
            request,
            priority
        );
        
        // Calcular delay hasta la fecha programada
        long delayMillis = Duration.between(LocalDateTime.now(), scheduledAt).toMillis();
        
        if (delayMillis > 0) {
            // Usar delayed message plugin de RabbitMQ o implementar con TTL
            sendDelayedToQueue(
                RabbitMQConfig.MESSAGE_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                payload,
                priority,
                delayMillis
            );
        } else {
            // Enviar inmediatamente
            sendToQueue(
                RabbitMQConfig.MESSAGE_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                payload,
                priority
            );
        }
        
        logger.info("Message scheduled for {} with ID {}", scheduledAt, message.getId());
        
        return message.getId();
    }
    
    // ============================================
    // MÉTODOS PRIVADOS
    // ============================================
    
    private void sendToQueue(
            String exchange,
            String routingKey,
            MessageQueuePayload payload,
            Integer priority) {
        
        rabbitTemplate.convertAndSend(
            exchange,
            routingKey,
            payload,
            message -> {
                message.getMessageProperties().setPriority(
                    Math.min(priority != null ? priority : 5, maxPriority)
                );
                message.getMessageProperties().setCorrelationId(payload.correlationId());
                message.getMessageProperties().setContentType("application/json");
                return message;
            }
        );
    }
    
    private void sendDelayedToQueue(
            String exchange,
            String routingKey,
            MessageQueuePayload payload,
            Integer priority,
            long delayMillis) {
        
        rabbitTemplate.convertAndSend(
            exchange,
            routingKey,
            payload,
            message -> {
                message.getMessageProperties().setPriority(
                    Math.min(priority != null ? priority : 5, maxPriority)
                );
                message.getMessageProperties().setCorrelationId(payload.correlationId());
                message.getMessageProperties().setContentType("application/json");
                message.getMessageProperties().setDelay(Math.toIntExact(delayMillis));
                return message;
            }
        );
    }
    
    private Message createMessageRecord(SendEmailRequest request) {
        // Similar a la implementación en EmailServiceImpl
        Message message = new Message();
        message.setType(MessageType.EMAIL);
        message.setSubject(request.subject());
        message.setFromAddress(request.from());
        message.setScheduledAt(request.sendDate());
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        
        // Agregar destinatarios
        request.to().forEach(contact -> {
            MessageRecipient recipient = new MessageRecipient();
            recipient.setMessage(message);
            recipient.setType(RecipientType.TO);
            recipient.setAddress(contact.email());
            recipient.setName(contact.name());
            recipient.setStatus(DeliveryStatus.PENDING);
            message.getRecipients().add(recipient);
        });
        
        return message;
    }
    
    private Message createTelegramMessageRecord(TelegramMessageRequest request) {
        Message message = new Message();
        message.setType(MessageType.TELEGRAM);
        message.setScheduledAt(request.scheduledAt());
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        
        MessageRecipient recipient = new MessageRecipient();
        recipient.setMessage(message);
        recipient.setType(RecipientType.TO);
        recipient.setAddress(request.chatId());
        recipient.setStatus(DeliveryStatus.PENDING);
        message.getRecipients().add(recipient);
        
        return message;
    }
}
```

### 4. Message Consumer/Worker

```java
@Component
public class EmailMessageConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailMessageConsumer.class);
    
    private final EmailServiceImpl emailService;
    private final MessageRepository messageRepository;
    private final RateLimiterService rateLimiterService;
    
    @Value("${mercury.email.rate-limit:100}")
    private Integer rateLimit; // Mensajes por minuto
    
    @RabbitListener(
        queues = RabbitMQConfig.EMAIL_QUEUE,
        concurrency = "5-10", // Min-Max workers
        ackMode = "MANUAL"
    )
    public void processEmailMessage(
            MessageQueuePayload payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        
        logger.info("Processing email message: {} (attempt {})", 
            payload.messageId(), payload.attemptCount());
        
        try {
            // Aplicar rate limiting
            rateLimiterService.acquirePermit("email");
            
            // Actualizar estado del mensaje
            Message message = messageRepository.findById(payload.messageId())
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));
            
            message.setStatus(MessageStatus.PROCESSING);
            message.setUpdatedAt(LocalDateTime.now());
            messageRepository.save(message);
            
            // Procesar mensaje
            ResponseEntity<SendEmailResponse> response = 
                emailService.sendMail(payload.emailRequest());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // Éxito - ACK
                channel.basicAck(deliveryTag, false);
                logger.info("Email message processed successfully: {}", payload.messageId());
            } else {
                // Error - NACK con requeue
                handleFailure(payload, channel, deliveryTag, 
                    "HTTP error: " + response.getStatusCode());
            }
            
        } catch (TemplateProcessingException e) {
            logger.error("Template processing error", e);
            // Error no recuperable - enviar a DLQ
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                logger.error("Error sending NACK", ioException);
            }
            
        } catch (Exception e) {
            logger.error("Error processing email message", e);
            // Error recuperable - reintentar
            handleFailure(payload, channel, deliveryTag, e.getMessage());
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ)
    public void processEmailDLQ(MessageQueuePayload payload) {
        logger.error("Message in DLQ: {} after {} attempts", 
            payload.messageId(), payload.attemptCount());
        
        // Actualizar mensaje como fallido definitivamente
        messageRepository.findById(payload.messageId()).ifPresent(message -> {
            message.setStatus(MessageStatus.FAILED);
            message.setErrorMessage("Máximo de reintentos alcanzado");
            message.setFailedAt(LocalDateTime.now());
            message.setUpdatedAt(LocalDateTime.now());
            
            DeliveryLog log = new DeliveryLog();
            log.setMessage(message);
            log.setLevel(LogLevel.ERROR);
            log.setEvent("Mensaje enviado a DLQ");
            log.setDetails("Falló después de " + payload.attemptCount() + " intentos");
            log.setCreatedAt(LocalDateTime.now());
            message.getDeliveryLogs().add(log);
            
            messageRepository.save(message);
        });
        
        // Notificar a administradores
        notifyAdministrators(payload);
    }
    
    private void handleFailure(
            MessageQueuePayload payload,
            Channel channel,
            long deliveryTag,
            String errorMessage) {
        
        try {
            int maxAttempts = 3;
            
            if (payload.attemptCount() < maxAttempts) {
                // Reintentar con backoff
                logger.warn("Requeuing message {} (attempt {})", 
                    payload.messageId(), payload.attemptCount() + 1);
                
                // NACK con requeue
                channel.basicNack(deliveryTag, false, true);
                
                // Actualizar contador de intentos en BD
                updateAttemptCount(payload.messageId(), payload.attemptCount() + 1, errorMessage);
                
            } else {
                // Enviar a DLQ
                logger.error("Max attempts reached for message {}, sending to DLQ", 
                    payload.messageId());
                channel.basicNack(deliveryTag, false, false);
            }
            
        } catch (IOException e) {
            logger.error("Error handling failure", e);
        }
    }
    
    private void updateAttemptCount(UUID messageId, int attemptCount, String errorMessage) {
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setRetryCount(attemptCount);
            message.setErrorMessage(errorMessage);
            message.setUpdatedAt(LocalDateTime.now());
            
            DeliveryLog log = new DeliveryLog();
            log.setMessage(message);
            log.setLevel(LogLevel.WARNING);
            log.setEvent("Reintento de envío");
            log.setDetails("Intento #" + attemptCount + ": " + errorMessage);
            log.setCreatedAt(LocalDateTime.now());
            message.getDeliveryLogs().add(log);
            
            messageRepository.save(message);
        });
    }
    
    private void notifyAdministrators(MessageQueuePayload payload) {
        // Implementar notificación a administradores
        logger.error("ADMIN NOTIFICATION: Message {} failed permanently", payload.messageId());
    }
}
```

### 5. Rate Limiter Service

```java
@Service
public class RateLimiterService {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Value("${mercury.email.rate-limit:100}")
    private Integer emailRateLimit;
    
    @Value("${mercury.telegram.rate-limit:30}")
    private Integer telegramRateLimit;
    
    @PostConstruct
    public void init() {
        // Crear rate limiters por canal
        limiters.put("email", RateLimiter.create(emailRateLimit / 60.0)); // Por segundo
        limiters.put("telegram", RateLimiter.create(telegramRateLimit / 60.0));
    }
    
    public void acquirePermit(String channel) {
        RateLimiter limiter = limiters.get(channel);
        if (limiter != null) {
            limiter.acquire();
        }
    }
    
    public boolean tryAcquirePermit(String channel, long timeout, TimeUnit unit) {
        RateLimiter limiter = limiters.get(channel);
        if (limiter != null) {
            return limiter.tryAcquire(timeout, unit);
        }
        return true;
    }
}
```

### 6. Refactorización del MailController

```java
@RestController
@RequestMapping("/api/v1/mail")
@CrossOrigin(origins = "*")
public class MailController {
    
    private final MessageProducerService producerService;
    
    @PostMapping("/send")
    @Operation(summary = "Enviar email asíncronamente")
    public ResponseEntity<MessageQueueResponse> sendAsync(
            @Valid @RequestBody SendEmailRequest request,
            @RequestParam(required = false) Integer priority) {
        
        UUID messageId = producerService.enqueueEmailMessage(request, priority);
        
        MessageQueueResponse response = new MessageQueueResponse(
            messageId,
            MessageStatus.PENDING,
            "Mensaje encolado para procesamiento"
        );
        
        return ResponseEntity.accepted().body(response);
    }
    
    @PostMapping("/send-batch")
    @Operation(summary = "Enviar múltiples emails")
    public ResponseEntity<BatchQueueResponse> sendBatch(
            @Valid @RequestBody BatchEmailRequest request) {
        
        List<UUID> messageIds = producerService.enqueueBatch(
            request.messages(),
            request.priority()
        );
        
        BatchQueueResponse response = new BatchQueueResponse(
            messageIds,
            messageIds.size(),
            "Mensajes encolados para procesamiento"
        );
        
        return ResponseEntity.accepted().body(response);
    }
    
    @PostMapping("/schedule")
    @Operation(summary = "Programar envío de email")
    public ResponseEntity<MessageQueueResponse> schedule(
            @Valid @RequestBody ScheduledEmailRequest request) {
        
        UUID messageId = producerService.scheduleMessage(
            request.emailRequest(),
            request.scheduledAt(),
            request.priority()
        );
        
        MessageQueueResponse response = new MessageQueueResponse(
            messageId,
            MessageStatus.PENDING,
            "Mensaje programado para " + request.scheduledAt()
        );
        
        return ResponseEntity.accepted().body(response);
    }
}

record MessageQueueResponse(UUID messageId, MessageStatus status, String message) {}
record BatchQueueResponse(List<UUID> messageIds, Integer count, String message) {}
record BatchEmailRequest(List<SendEmailRequest> messages, Integer priority) {}
record ScheduledEmailRequest(SendEmailRequest emailRequest, LocalDateTime scheduledAt, Integer priority) {}
```

## Dependencias
- **Dependencias de Tareas:**
  - MERC-TASK-003 (Integración Motor) - DEBE estar completada

- **Dependencias Técnicas:**
  - Spring AMQP 3.0+
  - RabbitMQ Server 3.12+
  - Guava (para RateLimiter)

- **Configuración:**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        concurrency: 5
        max-concurrency: 10
        prefetch: 1
        acknowledge-mode: manual
        retry:
          enabled: true
          initial-interval: 2000
          max-attempts: 3
          multiplier: 2
```

## Criterios de Aceptación

1. **CA-01**: Configuración de RabbitMQ completa
   - ✅ Exchanges creados
   - ✅ Queues creadas con DLQ
   - ✅ Bindings configurados
   - ✅ Prioridades habilitadas

2. **CA-02**: Producer service funciona
   - ✅ Encola mensajes correctamente
   - ✅ Soporta prioridades
   - ✅ Envío batch funciona
   - ✅ Programación de mensajes funciona

3. **CA-03**: Consumer/Worker funciona
   - ✅ Procesa mensajes de la cola
   - ✅ Múltiples workers en paralelo
   - ✅ ACK/NACK apropiados
   - ✅ Reintentos con backoff

4. **CA-04**: DLQ funciona correctamente
   - ✅ Mensajes fallidos van a DLQ
   - ✅ Procesa mensajes de DLQ
   - ✅ Notifica administradores
   - ✅ Actualiza estado en BD

5. **CA-05**: Rate limiting funciona
   - ✅ Respeta límites configurados
   - ✅ Por canal independiente
   - ✅ No bloquea sistema

6. **CA-06**: API refactorizada
   - ✅ Endpoints asíncronos
   - ✅ Respuestas inmediatas (202 Accepted)
   - ✅ Documentación actualizada

7. **CA-07**: Tests completos
   - ✅ Tests unitarios
   - ✅ Tests de integración con Testcontainers
   - ✅ Tests de concurrencia
   - ✅ Cobertura > 80%

## Definition of Done (DoD)

- [x] Código compilado sin errores
- [x] Todos los tests pasan
- [x] Cobertura > 80%
- [x] Code review aprobado
- [x] Sin violaciones PMD
- [x] JavaDoc completo
- [x] RabbitMQ configurado en desarrollo
- [x] Tests con Testcontainers funcionan
- [x] Documentación de despliegue
- [x] Configuración para diferentes ambientes
- [x] Monitoreo de colas configurado
- [x] Cambios commiteados y pusheados
- [x] Pull Request aprobado

## Estimación
- **Complejidad**: Alta
- **Story Points**: 13
- **Tiempo Estimado**: 20-24 horas
- **Desarrolladores Requeridos**: 1 Backend Developer

## Riesgos
1. **Riesgo**: Complejidad de configuración de RabbitMQ
   - **Mitigación**: Usar Docker Compose para desarrollo

2. **Riesgo**: Pérdida de mensajes
   - **Mitigación**: Queues durables, mensajes persistentes

3. **Riesgo**: Sobrecarga del sistema
   - **Mitigación**: Rate limiting, máximo de workers

## Notas Adicionales
- Configurar clustering de RabbitMQ en producción
- Implementar monitoreo con Prometheus
- Documentar troubleshooting común
- Considerar migración a Kafka en el futuro

## Referencias
- Spring AMQP: https://spring.io/projects/spring-amqp
- RabbitMQ Tutorials: https://www.rabbitmq.com/getstarted.html
- Rate Limiting: https://github.com/google/guava/wiki/CachesExplained
- Testcontainers RabbitMQ: https://www.testcontainers.org/modules/rabbitmq/
