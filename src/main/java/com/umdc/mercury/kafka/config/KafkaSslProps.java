package com.umdc.mercury.kafka.config;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "umdc.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaSslProps {

    @Value("${umdc.kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${umdc.kafka.sasl.mechanism:}")
    private String saslMechanism;

    @Value("${umdc.kafka.sasl.jaas.config:}")
    private String saslJaasConfig;

    @Value("${umdc.kafka.ssl.endpoint.identification.algorithm:https}")
    private String endpointIdentificationAlgorithm;

    // PEM keystore: separate cert and key files (Aiven standard layout)
    @Value("${umdc.kafka.ssl.keystore.certificate.location:classpath:aiven-kafka-7e59598.cert}")
    private Resource keystoreCertLocation;

    @Value("${umdc.kafka.ssl.keystore.key.location:classpath:aiven-kafka-7e59598.key}")
    private Resource keystoreKeyLocation;

    // Non-PEM keystore (PKCS12 / JKS) — used when ssl.keystore.type != PEM
    @Value("${umdc.kafka.ssl.keystore.location:classpath:aiven-kafka.p12}")
    private Resource keystoreLocation;

    @Value("${umdc.kafka.ssl.keystore.password:changeit}")
    private String keystorePassword;

    @Value("${umdc.kafka.ssl.keystore.type:PEM}")
    private String keystoreType;

    @Value("${umdc.kafka.ssl.key.password:${umdc.kafka.ssl.keystore.password:changeit}}")
    private String keyPassword;

    // Truststore — PEM CA cert or JKS/PKCS12
    @Value("${umdc.kafka.ssl.truststore.location:classpath:aiven-kafka-7e59598.pem}")
    private Resource truststoreLocation;

    @Value("${umdc.kafka.ssl.truststore.password:changeit}")
    private String truststorePassword;

    @Value("${umdc.kafka.ssl.truststore.type:PEM}")
    private String truststoreType;

    public Map<String, Object> build() throws IOException {
        Map<String, Object> props = new HashMap<>();

        if (SecurityProtocol.PLAINTEXT.name.equals(securityProtocol)) {
            return props;
        }

        props.put(org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, endpointIdentificationAlgorithm);

        if (!saslMechanism.isBlank()) {
            props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        }
        if (!saslJaasConfig.isBlank()) {
            props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }

        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, keystoreType);
        if ("PEM".equalsIgnoreCase(keystoreType)) {
            // Inline PEM content works in both file-system and JAR deployments.
            // Aiven supplies the client cert and private key as separate files.
            if (keystoreCertLocation.exists()) {
                props.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG,
                        keystoreCertLocation.getContentAsString(StandardCharsets.UTF_8));
            }
            if (keystoreKeyLocation.exists()) {
                props.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG,
                        keystoreKeyLocation.getContentAsString(StandardCharsets.UTF_8));
            }
        } else if (keystoreLocation.exists()) {
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation.getFile().getAbsolutePath());
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);
        }

        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType);
        if ("PEM".equalsIgnoreCase(truststoreType)) {
            if (truststoreLocation.exists()) {
                props.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG,
                        truststoreLocation.getContentAsString(StandardCharsets.UTF_8));
            }
        } else if (truststoreLocation.exists()) {
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation.getFile().getAbsolutePath());
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        }

        return props;
    }
}
