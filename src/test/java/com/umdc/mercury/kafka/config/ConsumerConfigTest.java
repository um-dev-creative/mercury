package com.umdc.mercury.kafka.config;

import com.umdc.mercury.kafka.consumer.service.EmailMessageConsumerService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerConfigTest {

    private final KafkaSslProps sslProps = mock(KafkaSslProps.class);
    private final ConsumerConfig config = new ConsumerConfig(sslProps);

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(config, "groupId", "group");
        ReflectionTestUtils.setField(config, "bootstrapServer", "broker");
        ReflectionTestUtils.setField(config, "bootstrapServerPort", "13828");
        ReflectionTestUtils.setField(config, "clientId", "client");
        ReflectionTestUtils.setField(config, "autoStartup", false);
        when(sslProps.build()).thenReturn(Map.of("security.protocol", "SSL"));
    }

    @Test
    void consumerFactoriesCarryBrokerAndTlsSettings() throws IOException {
        Map<String, Object> emailProps = config.emailMessageConsumerFactory().getConfigurationProperties();
        Map<String, Object> notificationProps = config.notificationEventConsumerFactory().getConfigurationProperties();

        assertEquals("broker:13828", emailProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("SSL", emailProps.get("security.protocol"));
        assertEquals("broker:13828", notificationProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("SSL", notificationProps.get("security.protocol"));
    }

    @Test
    void bootstrapServerIsUsedAloneWithoutPort() throws IOException {
        ReflectionTestUtils.setField(config, "bootstrapServerPort", "");

        assertEquals("broker", config.emailMessageConsumerFactory().getConfigurationProperties()
                .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void listenerFactoriesFollowAutoStartup() throws IOException {
        var email = config.emailMessageKafkaListenerContainerFactory(mock(EmailMessageConsumerService.class));
        var notification = config.notificationEventKafkaListenerContainerFactory();

        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(email, "autoStartup"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(notification, "autoStartup"));
    }

    @Test
    void undeserializableRecordIsSkippedWithoutRetrying() throws IOException {
        var factory = config.notificationEventKafkaListenerContainerFactory();
        var handler = (DefaultErrorHandler) ReflectionTestUtils.getField(factory, "commonErrorHandler");
        var exception = new DeserializationException("bad payload", new byte[0], false, new IllegalStateException());

        assertTrue(handler.handleOne(exception, new ConsumerRecord<>("topic", 0, 7L, "key", "value"),
                mock(Consumer.class), mock(MessageListenerContainer.class)));
    }
}
