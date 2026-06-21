package com.umdc.mercury.kafka.config;

import com.umdc.mercury.kafka.consumer.service.EmailMessageConsumerService;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import io.jsonwebtoken.lang.Objects;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@ConditionalOnProperty(name = "umdc.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class ConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerConfig.class);

    @Value("${umdc.consumer.mercury.topic}")
    private String mercuryEmailTopic;
    @Value("${umdc.consumer.group-id:mercury-multi-channel}")
    private String groupId;
    @Value("${umdc.bootstrap.server.url}")
    private String bootstrapServer;
    @Value("${umdc.bootstrap.server.port}")
    private String bootstrapServerPort;
    @Value("${umdc.kafka.auto-startup:false}")
    private boolean autoStartup;

    @Value("${umdc.kafka.client-id:u5c06022c8c6@application-user.avns.net}")
    private String clientId;

    private final KafkaSslProps kafkaSslProps;

    public ConsumerConfig(KafkaSslProps kafkaSslProps) {
        this.kafkaSslProps = kafkaSslProps;
    }

    @Bean
    public ConsumerFactory<String, EmailMessageTO> emailMessageConsumerFactory() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                !Objects.isEmpty(bootstrapServerPort) ? bootstrapServer + ":" + bootstrapServerPort : bootstrapServer);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 20971520);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 20971520);
        props.putAll(kafkaSslProps.build());
        ErrorHandlingDeserializer<EmailMessageTO> valueDeserializer =
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>(EmailMessageTO.class));
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailMessageTO> emailMessageKafkaListenerContainerFactory(
            EmailMessageConsumerService emailMessageConsumerService) throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, EmailMessageTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(emailMessageConsumerFactory());
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, NotificationEventTO> notificationEventConsumerFactory() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                !Objects.isEmpty(bootstrapServerPort) ? bootstrapServer + ":" + bootstrapServerPort : bootstrapServer);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 20971520);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 20971520);
        props.putAll(kafkaSslProps.build());
        JsonDeserializer<NotificationEventTO> jsonDeserializer = new JsonDeserializer<>(NotificationEventTO.class);
        jsonDeserializer.setUseTypeHeaders(false);
        ErrorHandlingDeserializer<NotificationEventTO> valueDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEventTO> notificationEventKafkaListenerContainerFactory()
            throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEventTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationEventConsumerFactory());
        factory.setAutoStartup(autoStartup);
        factory.setCommonErrorHandler(notificationEventErrorHandler());
        return factory;
    }

    private DefaultErrorHandler notificationEventErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (consumerRecord, ex) -> logger.warn("Skipping un-deserializable consumerRecord from topic={}, partition={}, offset={}: {}",
                        consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(), ex.getMessage()),
                new FixedBackOff(0L, 0L)
        );
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }
}
