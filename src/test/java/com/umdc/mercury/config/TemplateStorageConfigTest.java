package com.umdc.mercury.config;

import com.umdc.mercury.template.storage.S3TemplateStorage;
import com.umdc.mercury.template.storage.LocalTemplateStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Test
    void theBeanIsAnS3StorageWhenTheModeIsS3() {
        var storage = new TemplateStorageConfig().templateStorage(null, " S3 ", " bucket ", "templates/", "us-east-1", "http://localhost:9000", true, "k", "s");

        assertThat(storage).isInstanceOf(S3TemplateStorage.class);
    }

    @Test
    void s3WithoutABucketFailsAtStartup() {
        var config = new TemplateStorageConfig();

        assertThatThrownBy(() -> config.templateStorage(null, "s3", " ", "templates/", "us-east-1", "", false, "", ""))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLOUDFLARE_R2_BUCKET_NAME");
        assertThatThrownBy(() -> config.templateStorage(null, "s3", null, "templates/", "us-east-1", "", false, "", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anyOtherModeUsesTheLocalFolder(@TempDir Path dir) {
        var freeMarker = new FreeMarkerConfig();
        ReflectionTestUtils.setField(freeMarker, "templateLoaderPath", dir.toString());
        var config = new TemplateStorageConfig();

        assertThat(config.templateStorage(freeMarker, "local", "", "", "", "", false, "", "")).isInstanceOf(LocalTemplateStorage.class);
        assertThat(config.templateStorage(freeMarker, null, "", "", "", "", false, "", "")).isInstanceOf(LocalTemplateStorage.class);
    }
}
