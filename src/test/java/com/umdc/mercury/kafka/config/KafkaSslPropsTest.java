package com.umdc.mercury.kafka.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaSslPropsTest {

    @TempDir
    Path dir;

    private KafkaSslProps props(String protocol, String cert, String key, String ca) {
        KafkaSslProps props = new KafkaSslProps();
        ReflectionTestUtils.setField(props, "securityProtocol", protocol);
        ReflectionTestUtils.setField(props, "saslMechanism", "");
        ReflectionTestUtils.setField(props, "saslJaasConfig", "");
        ReflectionTestUtils.setField(props, "endpointIdentificationAlgorithm", "https");
        ReflectionTestUtils.setField(props, "keystoreType", "PEM");
        ReflectionTestUtils.setField(props, "truststoreType", "PEM");
        ReflectionTestUtils.setField(props, "keystoreCertLocation", new FileSystemResource(dir.resolve(cert)));
        ReflectionTestUtils.setField(props, "keystoreKeyLocation", new FileSystemResource(dir.resolve(key)));
        ReflectionTestUtils.setField(props, "truststoreLocation", new FileSystemResource(dir.resolve(ca)));
        return props;
    }

    @Test
    void plaintextAddsNoTlsSettings() throws IOException {
        assertTrue(props("PLAINTEXT", "c", "k", "ca").build().isEmpty());
    }

    @Test
    void sslWithPemFilesLoadsClientCertificateKeyAndCa() throws IOException {
        Files.writeString(dir.resolve("c"), "cert");
        Files.writeString(dir.resolve("k"), "key");
        Files.writeString(dir.resolve("ca"), "authority");

        Map<String, Object> built = props("SSL", "c", "k", "ca").build();

        assertEquals("SSL", built.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals("cert", built.get(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG));
        assertEquals("key", built.get(SslConfigs.SSL_KEYSTORE_KEY_CONFIG));
        assertEquals("authority", built.get(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG));
    }

    @Test
    void sslWithMissingPemFilesLeavesThemOut() throws IOException {
        Map<String, Object> built = props("SSL", "missing-c", "missing-k", "missing-ca").build();

        assertEquals("SSL", built.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertFalse(built.containsKey(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG));
        assertFalse(built.containsKey(SslConfigs.SSL_KEYSTORE_KEY_CONFIG));
        assertFalse(built.containsKey(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG));
    }

    @Test
    void sslWithPkcs12UsesFileLocationsAndPasswords() throws IOException {
        Path keystore = Files.writeString(dir.resolve("keystore.p12"), "keystore");
        Path truststore = Files.writeString(dir.resolve("truststore.jks"), "truststore");
        KafkaSslProps props = props("SSL", "c", "k", "truststore.jks");
        ReflectionTestUtils.setField(props, "keystoreType", "PKCS12");
        ReflectionTestUtils.setField(props, "truststoreType", "JKS");
        ReflectionTestUtils.setField(props, "keystoreLocation", new FileSystemResource(keystore));
        ReflectionTestUtils.setField(props, "keystorePassword", "keystore-password");
        ReflectionTestUtils.setField(props, "keyPassword", "key-password");
        ReflectionTestUtils.setField(props, "truststorePassword", "truststore-password");

        Map<String, Object> built = props.build();

        assertEquals(keystore.toAbsolutePath().toString(), built.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG));
        assertEquals(truststore.toAbsolutePath().toString(), built.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
        assertEquals("truststore-password", built.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
    }

    @Test
    void resourceInsideAJarIsCopiedToATemporaryFile() throws IOException {
        KafkaSslProps props = props("SSL", "c", "k", "ca");
        ReflectionTestUtils.setField(props, "keystoreType", "PKCS12");
        ReflectionTestUtils.setField(props, "keystoreLocation", new ByteArrayResource("keystore".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "keystore.p12";
            }
        });
        ReflectionTestUtils.setField(props, "keystorePassword", "keystore-password");
        ReflectionTestUtils.setField(props, "keyPassword", "key-password");

        String location = (String) props.build().get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG);

        assertEquals("keystore", Files.readString(Path.of(location)));
    }

    @Test
    void saslSettingsArePassedThrough() throws IOException {
        KafkaSslProps props = props("SASL_SSL", "c", "k", "ca");
        ReflectionTestUtils.setField(props, "saslMechanism", "SCRAM-SHA-256");
        ReflectionTestUtils.setField(props, "saslJaasConfig", "jaas");

        Map<String, Object> built = props.build();

        assertEquals("SCRAM-SHA-256", built.get(SaslConfigs.SASL_MECHANISM));
        assertEquals("jaas", built.get(SaslConfigs.SASL_JAAS_CONFIG));
    }
}
