package com.prx.mercury.kafka.config;

import com.prx.mercury.kafka.consumer.EmailMessageConsumerService;
import com.prx.mercury.kafka.to.EmailMessageTO;
import io.jsonwebtoken.lang.Objects;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class ConsumerConfig {

    @Value("${prx.consumer.mercury.topic}")
    private String mercuryEmailTopic;
    @Value("${prx.bootstrap.server.url}")
    private String bootstrapServer;
    @Value("${prx.bootstrap.server.port}")
    private String bootstrapServerPort;

    @Bean
    public ConsumerFactory<String, EmailMessageTO> emailMessageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                !Objects.isEmpty(bootstrapServerPort) ? bootstrapServer+":"+bootstrapServerPort : bootstrapServer);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, mercuryEmailTopic);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 20971520);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 20971520);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(EmailMessageTO.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailMessageTO> emailMessageKafkaListenerContainerFactory(EmailMessageConsumerService emailMessageConsumerService) {
        ConcurrentKafkaListenerContainerFactory<String, EmailMessageTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(emailMessageConsumerFactory());
        return factory;
    }

}
