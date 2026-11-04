package com.umdc.mercury.template.storage;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Templates as objects in a bucket of S3 or of an S3-compatible service (MinIO and the like), under a key prefix.
 *
 * <p>The etag is the one the service returns for the object. Conditional writes are done by the service itself
 * ({@code If-None-Match: *} to create, {@code If-Match} to replace), so two Mercury instances never overwrite each other:
 * the second one gets {@code 412} (or {@code 409} while the first is still writing) and that becomes a
 * {@link TemplateConflictException}. Objects are written whole, so a reader never sees half a template.</p>
 */
public class S3TemplateStorage implements TemplateStorage {

    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";
    private static final int PRECONDITION_FAILED = 412;
    private static final int CONFLICT = 409;
    private static final int NOT_FOUND = 404;

    private final S3Client client;
    private final String bucket;
    private final String prefix;

    /**
     * @param client the S3 client
     * @param bucket bucket that holds the templates
     * @param prefix key prefix inside the bucket (for example {@code templates/}); empty for the bucket root
     */
    public S3TemplateStorage(S3Client client, String bucket, String prefix) {
        this.client = Objects.requireNonNull(client);
        this.bucket = Objects.requireNonNull(bucket);
        this.prefix = normalize(prefix);
    }

    @Override
    public Optional<StoredTemplate> read(String name) {
        if (!TemplateNames.isValid(name)) {
            return Optional.empty();
        }
        try {
            ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(prefix + name).build());
            GetObjectResponse response = object.response();
            return Optional.of(new StoredTemplate(new TemplateMetadata(name, unquote(response.eTag()), response.lastModified()), object.asByteArray()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public Optional<TemplateMetadata> stat(String name) {
        if (!TemplateNames.isValid(name)) {
            return Optional.empty();
        }
        try {
            HeadObjectResponse head = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(prefix + name).build());
            return Optional.of(new TemplateMetadata(name, unquote(head.eTag()), head.lastModified()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public TemplateMetadata write(String name, byte[] content, String expectedEtag) {
        TemplateNames.require(name);
        PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucket).key(prefix + name).contentType(CONTENT_TYPE);
        if (ABSENT.equals(expectedEtag)) {
            request.ifNoneMatch("*");
        } else if (Objects.nonNull(expectedEtag)) {
            request.ifMatch("\"" + expectedEtag + "\"");
        }
        try {
            PutObjectResponse response = client.putObject(request.build(), RequestBody.fromBytes(content));
            return new TemplateMetadata(name, unquote(response.eTag()), Instant.now());
        } catch (S3Exception e) {
            if (e.statusCode() == PRECONDITION_FAILED || e.statusCode() == CONFLICT) {
                throw new TemplateConflictException(name, e);
            }
            // a conditional write on a key that does not exist yet answers 404 when If-Match is used
            if (e.statusCode() == NOT_FOUND && Objects.nonNull(expectedEtag) && !ABSENT.equals(expectedEtag)) {
                throw new TemplateConflictException(name, e);
            }
            throw e;
        }
    }

    @Override
    public void delete(String name) {
        if (!TemplateNames.isValid(name)) {
            return;
        }
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(prefix + name).build());
    }

    @Override
    public List<String> list() {
        List<String> names = new ArrayList<>();
        String token = "";
        do {
            ListObjectsV2Response page = client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).delimiter("/")
                    .continuationToken(token.isEmpty() ? null : token).build());
            for (S3Object object : page.contents()) {
                String name = object.key().substring(prefix.length());
                if (TemplateNames.isValid(name)) {
                    names.add(name);
                }
            }
            token = Boolean.TRUE.equals(page.isTruncated()) ? Objects.requireNonNullElse(page.nextContinuationToken(), "") : "";
        } while (!token.isEmpty());
        return names.stream().sorted().toList();
    }

    private static String normalize(String prefix) {
        if (Objects.isNull(prefix) || prefix.isBlank() || "/".equals(prefix.trim())) {
            return "";
        }
        String trimmed = prefix.trim().replaceFirst("^/+", "");
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    private static String unquote(String etag) {
        if (Objects.isNull(etag)) {
            return "";
        }
        return etag.length() >= 2 && etag.startsWith("\"") && etag.endsWith("\"") ? etag.substring(1, etag.length() - 1) : etag;
    }
}
