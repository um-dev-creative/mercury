package com.umdc.mercury.template.storage;

import com.umdc.mercury.config.FreeMarkerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs the storage against a real S3-compatible service. Off by default; to run it start MinIO and set
 * {@code MERCURY_IT_S3_ENDPOINT} (for example {@code http://localhost:9000}), {@code MERCURY_IT_S3_ACCESS_KEY} and
 * {@code MERCURY_IT_S3_SECRET_KEY}:
 * <pre>docker run -p 9000:9000 -e MINIO_ROOT_USER=minio -e MINIO_ROOT_PASSWORD=minio12345 minio/minio server /data</pre>
 */
@EnabledIfEnvironmentVariable(named = "MERCURY_IT_S3_ENDPOINT", matches = ".+")
class S3TemplateStorageIT {

    private static final String BUCKET = "mercury-templates-it";
    private static S3Client client;
    private final S3TemplateStorage storage = new S3TemplateStorage(client, BUCKET, "it-" + UUID.randomUUID() + "/");

    @BeforeAll
    static void connect() {
        client = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(System.getenv("MERCURY_IT_S3_ENDPOINT")))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(System.getenv("MERCURY_IT_S3_ACCESS_KEY"), System.getenv("MERCURY_IT_S3_SECRET_KEY"))))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(BUCKET).build());
        } catch (NoSuchBucketException e) {
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void roundTripWithEtags() {
        TemplateMetadata created = storage.write("a.ftl", bytes("uno"), TemplateStorage.ABSENT);
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("uno");
        assertThat(storage.stat("a.ftl").orElseThrow().etag()).isEqualTo(created.etag());

        TemplateMetadata updated = storage.write("a.ftl", bytes("dos"), created.etag());
        assertThat(updated.etag()).isNotEqualTo(created.etag());
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("dos");
    }

    @Test
    void creatingOverAnExistingObjectAndReplacingAStaleOneAreConflicts() {
        TemplateMetadata first = storage.write("b.ftl", bytes("1"), TemplateStorage.ABSENT);
        assertThrows(TemplateConflictException.class, () -> storage.write("b.ftl", bytes("2"), TemplateStorage.ABSENT));
        storage.write("b.ftl", bytes("2"), first.etag());

        assertThrows(TemplateConflictException.class, () -> storage.write("b.ftl", bytes("3"), first.etag()));
        assertThat(storage.read("b.ftl").orElseThrow().text()).isEqualTo("2");
    }

    @Test
    void listDeleteAndMissing() {
        storage.write("c.md", bytes("x"), null);
        storage.write("d.ftl", bytes("y"), null);

        assertThat(storage.list()).containsExactly("c.md", "d.ftl");
        storage.delete("c.md");
        assertThat(storage.read("c.md")).isEmpty();
        assertThat(storage.stat("c.md")).isEmpty();
        assertThat(storage.list()).containsExactly("d.ftl");
    }

    @Test
    void freemarkerRendersFromTheBucketAndPicksUpAnEdit() throws Exception {
        TemplateMetadata first = storage.write("f.md", bytes("# Hola ${name}"), TemplateStorage.ABSENT);
        var config = FreeMarkerConfig.hardened(new StorageTemplateLoader(storage));
        config.setTemplateUpdateDelayMilliseconds(0);

        assertThat(render(config)).contains("<h1>Hola Ana</h1>");
        storage.write("f.md", bytes("## Adiós ${name}"), first.etag());
        assertThat(render(config)).contains("<h2>Adiós Ana</h2>");
    }

    private static String render(freemarker.template.Configuration config) throws Exception {
        StringWriter out = new StringWriter();
        config.getTemplate("f.md").process(Map.of("name", "Ana"), out);
        return out.toString();
    }
}
