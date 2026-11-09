package com.umdc.mercury.config;

import com.umdc.mercury.template.storage.S3TemplateStorage;
import com.umdc.mercury.template.storage.LocalTemplateStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.umdc.mercury.template.storage.TemplateStorage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateStorageConfigTest {

    @Test
    void staticCredentialsWhenBothKeysAreGiven() {
        var provider = TemplateStorageConfig.credentials(" key ", " secret ");

        assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("key");
        assertThat(credentials.secretAccessKey()).isEqualTo("secret");
    }

    @Test
    void theDefaultChainWhenAKeyIsMissing() {
        assertThat(TemplateStorageConfig.credentials("", "secret")).isInstanceOf(DefaultCredentialsProvider.class);
        assertThat(TemplateStorageConfig.credentials("key", null)).isInstanceOf(DefaultCredentialsProvider.class);
        assertThat(TemplateStorageConfig.credentials(null, null)).isInstanceOf(DefaultCredentialsProvider.class);
    }

    @Test
    void buildsAClientForAnS3CompatibleEndpointAndForAws() {
        try (var minio = TemplateStorageConfig.client("us-east-1", "http://localhost:9000", true, "k", "s");
             var aws = TemplateStorageConfig.client("eu-west-1", " ", false, "k", "s")) {
            assertThat(minio).isNotNull();
            assertThat(aws).isNotNull();
        }
    }

    private static TemplateStorage create(String mode, String localPath, String bucket) {
        return new TemplateStorageConfig().templateStorage(mode, localPath, bucket, "templates/", "us-east-1", "http://localhost:9000", true, "k", "s");
    }

    @ParameterizedTest
    @ValueSource(strings = {"s3", " S3 ", "r2", "Cloud"})
    void cloudModesGiveAnS3Storage(String mode) {
        assertThat(create(mode, "", " bucket ")).isInstanceOf(S3TemplateStorage.class);
    }

    @Test
    void cloudWithoutABucketFailsAtStartup() {
        assertThatThrownBy(() -> create("s3", "", " ")).isInstanceOf(IllegalStateException.class).hasMessageContaining("CLOUDFLARE_R2_BUCKET_NAME");
        assertThatThrownBy(() -> create("r2", "", null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void localUsesTemplatePathAndCreatesTheFolder(@TempDir Path dir) {
        Path folder = dir.resolve("made/here");

        assertThat(create("local", folder.toString(), "")).isInstanceOf(LocalTemplateStorage.class);
        assertThat(create(null, folder.toString(), "")).isInstanceOf(LocalTemplateStorage.class);
        assertThat(folder).isDirectory();
    }

    @Test
    void anyOtherValueIsTheFolderPathItself(@TempDir Path dir) {
        Path folder = dir.resolve("from-storage");

        assertThat(create(folder.toString(), "/ignored", "")).isInstanceOf(LocalTemplateStorage.class);
        assertThat(folder).isDirectory();
    }

    @Test
    void localWithoutAFolderOrWithAnUnusableOneFailsAtStartup(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("plain-file"), "x");

        assertThatThrownBy(() -> create("local", " ", "")).isInstanceOf(IllegalStateException.class).hasMessageContaining("TEMPLATE_PATH");
        assertThatThrownBy(() -> create("local", null, "")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> create("local", file.resolve("sub").toString(), "")).isInstanceOf(IllegalStateException.class);
    }
}
