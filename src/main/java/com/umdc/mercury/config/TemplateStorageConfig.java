package com.umdc.mercury.config;

import com.umdc.mercury.template.storage.S3TemplateStorage;
import com.umdc.mercury.template.storage.TemplateStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Template storage in S3 or an S3-compatible service, chosen with {@code umdc.templates.storage=s3}. Without it the
 * templates are read from the folder (see {@link FreeMarkerConfig#templateStorage()}).
 *
 * <p>Credentials: {@code TEMPLATE_S3_ACCESS_KEY} and {@code TEMPLATE_S3_SECRET_KEY} (secrets, normally from Vault);
 * when they are empty the AWS default chain is used (instance role, environment, profile).</p>
 */
@Configuration
@ConditionalOnProperty(name = "umdc.templates.storage", havingValue = "s3")
public class TemplateStorageConfig {

    /**
     * @param bucket        bucket that holds the templates
     * @param prefix        key prefix inside the bucket
     * @param region        AWS region (any value for MinIO)
     * @param endpoint      endpoint of an S3-compatible service; empty for AWS
     * @param pathStyle     {@code true} for services that need {@code host/bucket/key} addressing (MinIO)
     * @param accessKey     access key; empty to use the default chain
     * @param secretKey     secret key; empty to use the default chain
     * @return the storage
     */
    @Bean
    public TemplateStorage templateStorage(@Value("${umdc.templates.s3.bucket}") String bucket,
                                           @Value("${umdc.templates.s3.prefix:templates/}") String prefix,
                                           @Value("${umdc.templates.s3.region:us-east-1}") String region,
                                           @Value("${umdc.templates.s3.endpoint:}") String endpoint,
                                           @Value("${umdc.templates.s3.path-style:false}") boolean pathStyle,
                                           @Value("${umdc.templates.s3.access-key:}") String accessKey,
                                           @Value("${umdc.templates.s3.secret-key:}") String secretKey) {
        return new S3TemplateStorage(client(region, endpoint, pathStyle, accessKey, secretKey), bucket, prefix);
    }

    static S3Client client(String region, String endpoint, boolean pathStyle, String accessKey, String secretKey) {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKey, secretKey))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim()));
        }
        return builder.build();
    }

    static AwsCredentialsProvider credentials(String accessKey, String secretKey) {
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
