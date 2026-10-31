package com.umdc.mercury.kafka.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProducerConfigTest {

    private final KafkaSslProps sslProps = mock(KafkaSslProps.class);
    private final ProducerConfig config = new ProducerConfig(sslProps);

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(config, "bootstrapServer", "broker");
        ReflectionTestUtils.setField(config, "bootstrapServerPort", "13828");
        ReflectionTestUtils.setField(config, "clientId", "client");
        when(sslProps.build()).thenReturn(Map.of("security.protocol", "SSL"));
    }

    @Test
    void producerCarriesBrokerAndTlsSettings() throws IOException {
        Map<String, Object> props = config.kafkaProducerFactory().getConfigurationProperties();

        assertEquals("broker:13828", props.get("bootstrap.servers"));
        assertEquals("client", props.get("client.id"));
        assertEquals("SSL", props.get("security.protocol"));
    }

    @Test
    void bootstrapServerIsUsedAloneWithoutPort() throws IOException {
        ReflectionTestUtils.setField(config, "bootstrapServerPort", "");

        assertEquals("broker", config.kafkaProducerFactory().getConfigurationProperties().get("bootstrap.servers"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void templateUsesTheGivenFactory() {
        ProducerFactory<String, Object> factory = mock(ProducerFactory.class);

        assertSame(factory, config.kafkaTemplate(factory).getProducerFactory());
    }
}
