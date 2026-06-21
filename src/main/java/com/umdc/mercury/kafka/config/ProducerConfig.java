package com.umdc.mercury.kafka.config;

import io.jsonwebtoken.lang.Objects;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
@ConditionalOnProperty(name = "umdc.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class ProducerConfig {

    @Value("${umdc.bootstrap.server.url}")
    private String bootstrapServer;
    @Value("${umdc.bootstrap.server.port}")
    private String bootstrapServerPort;

    @Value("${umdc.kafka.client-id:u5c06022c8c6}")
    private String clientId;

    private final KafkaSslProps kafkaSslProps;

    public ProducerConfig(KafkaSslProps kafkaSslProps) {
        this.kafkaSslProps = kafkaSslProps;
    }

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG,
                !Objects.isEmpty(bootstrapServerPort) ? bootstrapServer + ":" + bootstrapServerPort : bootstrapServer);
        props.put(CLIENT_ID_CONFIG, clientId);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put("spring.json.add.type.headers", true);
        props.put("spring.json.trusted.packages", "*");
        props.putAll(kafkaSslProps.build());
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }
}
