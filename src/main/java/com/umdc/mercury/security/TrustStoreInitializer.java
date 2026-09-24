package com.umdc.mercury.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TrustStoreInitializer.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 09-23-2026
 * @since 11
 */
public final class TrustStoreInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustStoreInitializer.class);

    private static final String CERTS_DIR = "certs/mercury";
    private static final String CACERTS_PASSWORD = "changeit";

    private TrustStoreInitializer() {
    }

    public static void importLocalCertsIntoDefaultTrustStore() {
        importLocalCertsIntoDefaultTrustStore(CERTS_DIR);
    }

    /**
     * Package-private overload so tests can point at an isolated temp directory
     * instead of the real {@link #CERTS_DIR} used at runtime.
     */
    @SuppressWarnings("java:S6437")
    // CACERTS_PASSWORD is the JDK's well-known default cacerts password ("changeit"), not a
    // real secret - it only protects a temp copy of the trust store this process owns.
    static void importLocalCertsIntoDefaultTrustStore(String certsDirPath) {
        File certsDir = new File(certsDirPath);
        File[] crtFiles = certsDir.listFiles((dir, name) -> name.endsWith(".crt"));
        if (crtFiles == null || crtFiles.length == 0) {
            return;
        }
        try {
            KeyStore trustStore = loadJdkDefaultCacerts();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            for (File crtFile : crtFiles) {
                String alias = crtFile.getName().replaceFirst("\\.crt$", "");
                if (trustStore.containsAlias(alias)) {
                    continue;
                }
                try (InputStream in = new FileInputStream(crtFile)) {
                    Certificate certificate = certificateFactory.generateCertificate(in);
                    trustStore.setCertificateEntry(alias, certificate);
                }
            }
            Path mergedTrustStore = createOwnerOnlyTempFile("mercury-cacerts", ".p12");
            File mergedTrustStoreFile = mergedTrustStore.toFile();
            mergedTrustStoreFile.deleteOnExit();
            try (var out = new FileOutputStream(mergedTrustStoreFile)) {
                trustStore.store(out, CACERTS_PASSWORD.toCharArray());
            }
            System.setProperty("javax.net.ssl.trustStore", mergedTrustStore.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStorePassword", CACERTS_PASSWORD);
        }
        catch (Exception e) {
            LOGGER.warn("Could not merge {} certs into the JVM default trust store; "
                    + "TLS calls to PRX-internal hosts may fail with PKIX errors", certsDirPath, e);
        }
    }

    /**
     * {@link Files#createTempFile} writes into the shared, publicly writable system temp
     * directory. Passing owner-only POSIX permissions as a creation attribute (rather than
     * calling {@code setReadable}/{@code setWritable} afterwards) locks the file down
     * atomically, closing the window during which it would otherwise sit world-readable.
     */
    private static Path createOwnerOnlyTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix, ownerOnlyPermissions());
    }

    private static FileAttribute<?>[] ownerOnlyPermissions() {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return new FileAttribute<?>[0];
        }
        Set<PosixFilePermission> ownerOnly = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(ownerOnly)};
    }

    @SuppressWarnings("java:S6437")
    // CACERTS_PASSWORD is the JDK's well-known default cacerts password ("changeit"), not a
    // real secret - it's only needed to open the JDK's own read-only cacerts file.
    private static KeyStore loadJdkDefaultCacerts() throws GeneralSecurityException, IOException {
        String javaHome = System.getProperty("java.home");
        File cacerts = new File(javaHome, "lib/security/cacerts");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(cacerts)) {
            keyStore.load(in, CACERTS_PASSWORD.toCharArray());
        }
        return keyStore;
    }
}
