package com.umdc.mercury.kafka.config;

import com.umdc.mercury.kafka.consumer.service.EmailMessageConsumerService;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import io.jsonwebtoken.lang.Objects;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@ConditionalOnProperty(name = "umdc.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class ConsumerConfig {

    @Value("${umdc.consumer.mercury.topic}")
    private String mercuryEmailTopic;
    @Value("${umdc.bootstrap.server.url}")
    private String bootstrapServer;
    @Value("${umdc.bootstrap.server.port}")
    private String bootstrapServerPort;
    @Value("${umdc.kafka.auto-startup:false}")
    private boolean autoStartup;

    private final KafkaSslProps kafkaSslProps;

    public ConsumerConfig(KafkaSslProps kafkaSslProps) {
        this.kafkaSslProps = kafkaSslProps;
    }

    @Bean
    public ConsumerFactory<String, EmailMessageTO> emailMessageConsumerFactory() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                !Objects.isEmpty(bootstrapServerPort) ? bootstrapServer + ":" + bootstrapServerPort : bootstrapServer);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, mercuryEmailTopic);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 20971520);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 20971520);
        props.putAll(kafkaSslProps.build());
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(EmailMessageTO.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailMessageTO> emailMessageKafkaListenerContainerFactory(
            EmailMessageConsumerService emailMessageConsumerService) throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, EmailMessageTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(emailMessageConsumerFactory());
        factory.setAutoStartup(autoStartup);
        return factory;
    }
}
