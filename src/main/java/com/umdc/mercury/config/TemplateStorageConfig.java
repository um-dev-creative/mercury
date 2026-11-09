package com.umdc.mercury.config;

import com.umdc.mercury.template.storage.LocalTemplateStorage;
import com.umdc.mercury.template.storage.S3TemplateStorage;
import com.umdc.mercury.template.storage.TemplateStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * The one {@link TemplateStorage} bean. {@code TEMPLATE_STORAGE} (property {@code umdc.templates.storage}) says where the
 * templates live:
 * <ul>
 *   <li>{@code s3}, {@code r2} or {@code cloud}: a bucket of S3 or of an S3-compatible service (Cloudflare R2), set with
 *   {@code CLOUDFLARE_R2_*};</li>
 *   <li>{@code local} (or empty): the folder {@code TEMPLATE_PATH};</li>
 *   <li>anything else is taken as the path of the local folder itself (for example {@code /data/templates}).</li>
 * </ul>
 * Mercury ships no templates: the storage holds all of them, and a local folder is created when it does not exist.
 *
 * <p>The choice is made when the bean is created, not with {@code @ConditionalOnProperty}: in a GraalVM native image
 * conditions are fixed when the image is built, so the variable set on the container would have no effect.</p>
 */
@Configuration
public class TemplateStorageConfig {

    private static final Set<String> CLOUD_MODES = Set.of("s3", "r2", "cloud");

    /**
     * @param mode          {@code TEMPLATE_STORAGE}: a cloud mode, {@code local}, or a folder path
     * @param localPath     folder used by {@code local} ({@code TEMPLATE_PATH})
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
    public TemplateStorage templateStorage(@Value("${umdc.templates.storage:local}") String mode,
                                           @Value("${spring.freemarker.template-loader-path:}") String localPath,
                                           @Value("${umdc.templates.s3.bucket:}") String bucket,
                                           @Value("${umdc.templates.s3.prefix:templates/}") String prefix,
                                           @Value("${umdc.templates.s3.region:us-east-1}") String region,
                                           @Value("${umdc.templates.s3.endpoint:}") String endpoint,
                                           @Value("${umdc.templates.s3.path-style:false}") boolean pathStyle,
                                           @Value("${umdc.templates.s3.access-key:}") String accessKey,
                                           @Value("${umdc.templates.s3.secret-key:}") String secretKey) {
        String selected = mode == null ? "" : mode.trim();
        if (CLOUD_MODES.contains(selected.toLowerCase(Locale.ROOT))) {
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalStateException("TEMPLATE_STORAGE=" + selected + " needs the bucket: set CLOUDFLARE_R2_BUCKET_NAME");
            }
            return new S3TemplateStorage(client(region, endpoint, pathStyle, accessKey, secretKey), bucket.trim(), prefix);
        }
        String folder = selected.isEmpty() || "local".equalsIgnoreCase(selected) ? localPath : selected;
        return local(folder);
    }

    static TemplateStorage local(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new IllegalStateException("Local template storage needs a folder: set TEMPLATE_PATH or put the folder path in TEMPLATE_STORAGE");
        }
        Path root = Path.of(folder.trim());
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("The template folder cannot be created or is not writable: " + root, e);
        }
        return new LocalTemplateStorage(root);
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
