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

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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

    /**
     * Reads the platform's default trusted CAs via {@link TrustManagerFactory} instead of
     * opening {@code $JAVA_HOME/lib/security/cacerts} directly. The file-path approach silently
     * fails under GraalVM native-image — there is no JRE installation on disk at runtime, so
     * {@code java.home} doesn't point at a real {@code lib/security/cacerts} file — which meant
     * PRX-internal certs never actually got merged in native mode, and any code path that fell
     * back to the JVM default trust store (rather than an explicit, app-configured one) failed
     * TLS handshakes with PKIX errors. This API returns the JDK's real default trust anchors on
     * a regular JVM and GraalVM's build-time-baked default trust anchors under native-image,
     * identically, with no environment-specific branching needed.
     */
    private static KeyStore loadJdkDefaultCacerts() throws GeneralSecurityException, IOException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        int index = 0;
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                for (Certificate certificate : x509TrustManager.getAcceptedIssuers()) {
                    keyStore.setCertificateEntry("default-" + index++, certificate);
                }
            }
        }
        return keyStore;
    }
}
