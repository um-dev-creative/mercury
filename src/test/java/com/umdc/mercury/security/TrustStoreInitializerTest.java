package com.umdc.mercury.security;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustStoreInitializerTest {

    private static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_TYPE_PROPERTY = "javax.net.ssl.trustStoreType";
    private static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
    private static final String CACERTS_PASSWORD = "changeit";

    private static byte[] selfSignedCertificatePem;

    @TempDir
    private Path tempDir;

    private String trustStorePropertyBeforeTest;

    @BeforeAll
    static void generateSelfSignedCertificate() throws Exception {
        Path tempDirForKeytool = Files.createTempDirectory("truststore-initializer-test");
        Path keystore = tempDirForKeytool.resolve("keystore.p12");
        String javaHome = System.getProperty("java.home");
        File keytool = new File(javaHome, "bin/keytool");

        exec(keytool.getAbsolutePath(), "-genkeypair", "-alias", "test-cert", "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "1", "-dname", "CN=trust-store-initializer-test", "-keystore", keystore.toString(),
                "-storetype", "PKCS12", "-storepass", CACERTS_PASSWORD, "-keypass", CACERTS_PASSWORD);

        Path exportedCert = tempDirForKeytool.resolve("cert.crt");
        exec(keytool.getAbsolutePath(), "-exportcert", "-alias", "test-cert", "-keystore", keystore.toString(),
                "-storepass", CACERTS_PASSWORD, "-rfc", "-file", exportedCert.toString());

        selfSignedCertificatePem = Files.readAllBytes(exportedCert);
        Files.delete(keystore);
        Files.delete(exportedCert);
        Files.delete(tempDirForKeytool);
    }

    private static void exec(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(finished, "keytool command timed out: " + String.join(" ", command));
        assertEquals(0, process.exitValue(), "keytool command failed: " + String.join(" ", command));
    }

    @BeforeEach
    void captureExistingTrustStoreProperty() {
        trustStorePropertyBeforeTest = System.getProperty(TRUST_STORE_PROPERTY);
    }

    @AfterEach
    void restoreTrustStoreProperties() {
        System.clearProperty(TRUST_STORE_PROPERTY);
        System.clearProperty(TRUST_STORE_TYPE_PROPERTY);
        System.clearProperty(TRUST_STORE_PASSWORD_PROPERTY);
        if (trustStorePropertyBeforeTest != null) {
            System.setProperty(TRUST_STORE_PROPERTY, trustStorePropertyBeforeTest);
        }
    }

    @Test
    void missingDirectory_doesNotSetTrustStoreProperties() {
        Path missingDir = tempDir.resolve("does-not-exist");

        TrustStoreInitializer.importLocalCertsIntoDefaultTrustStore(missingDir.toString());

        assertNull(System.getProperty(TRUST_STORE_PROPERTY));
    }

    @Test
    void directoryWithoutCrtFiles_doesNotSetTrustStoreProperties() throws Exception {
        Files.writeString(tempDir.resolve("readme.txt"), "not a certificate");

        TrustStoreInitializer.importLocalCertsIntoDefaultTrustStore(tempDir.toString());

        assertNull(System.getProperty(TRUST_STORE_PROPERTY));
    }

    @Test
    void validCrtFile_mergesIntoCopyOfDefaultTrustStoreAndSetsSystemProperties() throws Exception {
        String alias = "prx-internal-ca";
        Files.write(tempDir.resolve(alias + ".crt"), selfSignedCertificatePem);

        TrustStoreInitializer.importLocalCertsIntoDefaultTrustStore(tempDir.toString());

        String mergedTrustStorePath = System.getProperty(TRUST_STORE_PROPERTY);
        assertTrue(mergedTrustStorePath != null && !mergedTrustStorePath.isBlank());
        assertEquals("PKCS12", System.getProperty(TRUST_STORE_TYPE_PROPERTY));
        assertEquals(CACERTS_PASSWORD, System.getProperty(TRUST_STORE_PASSWORD_PROPERTY));

        KeyStore mergedTrustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(mergedTrustStorePath)) {
            mergedTrustStore.load(in, CACERTS_PASSWORD.toCharArray());
        }
        assertTrue(mergedTrustStore.containsAlias(alias));

        Files.deleteIfExists(Path.of(mergedTrustStorePath));
    }

    @Test
    void invalidCrtFile_exceptionIsCaughtAndPropertiesAreNotSet() throws Exception {
        Files.writeString(tempDir.resolve("broken.crt"), "this is not a valid certificate");

        assertDoesNotThrow(() -> TrustStoreInitializer.importLocalCertsIntoDefaultTrustStore(tempDir.toString()));

        assertNull(System.getProperty(TRUST_STORE_PROPERTY));
        assertNull(System.getProperty(TRUST_STORE_TYPE_PROPERTY));
    }

    @Test
    void publicEntryPoint_delegatesToConfiguredCertsDirectoryWithoutThrowing() {
        // Assigned to an explicitly-typed Executable first: passing the method reference directly
        // to assertDoesNotThrow is ambiguous between its Executable and ThrowingSupplier<T> overloads.
        Executable importCerts = TrustStoreInitializer::importLocalCertsIntoDefaultTrustStore;
        assertDoesNotThrow(importCerts);
    }
}
