package com.umdc.mercury.config;

import com.umdc.mercury.template.storage.S3TemplateStorage;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;

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
    void theBeanIsAnS3Storage() {
        var storage = new TemplateStorageConfig().templateStorage("bucket", "templates/", "us-east-1", "http://localhost:9000", true, "k", "s");

        assertThat(storage).isInstanceOf(S3TemplateStorage.class);
    }
}
