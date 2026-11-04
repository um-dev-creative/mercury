package com.umdc.mercury.template.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3TemplateStorageTest {

    @Mock private S3Client client;

    private S3TemplateStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3TemplateStorage(client, "bucket", "templates");
    }

    private static S3Exception status(int code) {
        return (S3Exception) S3Exception.builder().statusCode(code).message("s3 " + code).build();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void readsAnObjectUnderThePrefixWithItsUnquotedEtag() {
        GetObjectResponse response = GetObjectResponse.builder().eTag("\"abc\"").lastModified(Instant.parse("2026-01-01T00:00:00Z")).build();
        when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(response, bytes("Hola")));

        StoredTemplate template = storage.read("hola.ftl").orElseThrow();

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client).getObjectAsBytes(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("bucket");
        assertThat(request.getValue().key()).isEqualTo("templates/hola.ftl");
        assertThat(template.text()).isEqualTo("Hola");
        assertThat(template.metadata().etag()).isEqualTo("abc");
        assertThat(template.metadata().lastModified()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void aMissingObjectIsEmptyOnReadAndStat() {
        when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("no").build());
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(status(404));

        assertThat(storage.read("a.ftl")).isEmpty();
        assertThat(storage.stat("a.ftl")).isEmpty();
    }

    @Test
    void aStatMissWithNoSuchKeyIsEmptyToo() {
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("no").build());

        assertThat(storage.stat("a.ftl")).isEmpty();
    }

    @Test
    void otherReadErrorsPropagate() {
        when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(status(500));
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(status(403));

        assertThrows(S3Exception.class, () -> storage.read("a.ftl"));
        assertThrows(S3Exception.class, () -> storage.stat("a.ftl"));
    }

    @Test
    void statReadsTheHeadersOnly() {
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().eTag("\"abc\"").lastModified(Instant.EPOCH).build());

        assertThat(storage.stat("a.ftl").orElseThrow().etag()).isEqualTo("abc");
        verify(client, never()).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void invalidNamesNeverReachTheService() {
        assertThat(storage.read("../x.ftl")).isEmpty();
        assertThat(storage.stat("a/b.ftl")).isEmpty();
        storage.delete("../x.ftl");
        assertThrows(IllegalArgumentException.class, () -> storage.write("../x.ftl", bytes("x"), null));

        verifyNoInteractions(client);
    }

    @Test
    void writeToCreateUsesIfNoneMatchStar() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().eTag("\"new\"").build());

        TemplateMetadata meta = storage.write("a.ftl", bytes("x"), TemplateStorage.ABSENT);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().ifNoneMatch()).isEqualTo("*");
        assertThat(request.getValue().ifMatch()).isNull();
        assertThat(request.getValue().key()).isEqualTo("templates/a.ftl");
        assertThat(request.getValue().contentType()).contains("utf-8");
        assertThat(meta.etag()).isEqualTo("new");
    }

    @Test
    void writeToReplaceUsesIfMatchWithTheQuotedEtag() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().eTag("\"two\"").build());

        storage.write("a.ftl", bytes("x"), "one");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().ifMatch()).isEqualTo("\"one\"");
        assertThat(request.getValue().ifNoneMatch()).isNull();
    }

    @Test
    void writeWithoutAPreconditionOverwrites() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().eTag("e").build());

        storage.write("a.ftl", bytes("x"), null);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().ifMatch()).isNull();
        assertThat(request.getValue().ifNoneMatch()).isNull();
    }

    @Test
    void aFailedPreconditionIsAConflict() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(status(412));

        assertThrows(TemplateConflictException.class, () -> storage.write("a.ftl", bytes("x"), "old"));
    }

    @Test
    void aConcurrentConditionalWriteIsAConflict() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(status(409));

        assertThrows(TemplateConflictException.class, () -> storage.write("a.ftl", bytes("x"), TemplateStorage.ABSENT));
    }

    @Test
    void replacingAnObjectThatIsGoneIsAConflictToo() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(status(404));

        assertThrows(TemplateConflictException.class, () -> storage.write("a.ftl", bytes("x"), "old"));
    }

    @Test
    void aMissingBucketOnCreateIsNotAConflict() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(status(404));

        assertThrows(S3Exception.class, () -> storage.write("a.ftl", bytes("x"), TemplateStorage.ABSENT));
    }

    @Test
    void otherWriteErrorsPropagate() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(status(500));

        assertThrows(S3Exception.class, () -> storage.write("a.ftl", bytes("x"), null));
    }

    @Test
    void deleteRemovesTheObjectUnderThePrefix() {
        storage.delete("a.ftl");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(request.capture());
        assertThat(request.getValue().key()).isEqualTo("templates/a.ftl");
    }

    @Test
    void listFollowsPagesKeepsOnlyTemplateNamesAndSorts() {
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().isTruncated(true).nextContinuationToken("next")
                        .contents(S3Object.builder().key("templates/b.ftl").build(), S3Object.builder().key("templates/").build()).build())
                .thenReturn(ListObjectsV2Response.builder().isTruncated(false)
                        .contents(S3Object.builder().key("templates/a.md").build(), S3Object.builder().key("templates/readme.txt").build()).build());

        assertThat(storage.list()).isEqualTo(List.of("a.md", "b.ftl"));

        ArgumentCaptor<ListObjectsV2Request> request = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(client, org.mockito.Mockito.times(2)).listObjectsV2(request.capture());
        assertThat(request.getAllValues().get(0).prefix()).isEqualTo("templates/");
        assertThat(request.getAllValues().get(0).delimiter()).isEqualTo("/");
        assertThat(request.getAllValues().get(1).continuationToken()).isEqualTo("next");
    }

    @Test
    void prefixesAreNormalised() {
        S3Client other = org.mockito.Mockito.mock(S3Client.class);
        when(other.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(ListObjectsV2Response.builder().build());

        for (String prefix : new String[] {null, "", "  ", "/"}) {
            new S3TemplateStorage(other, "b", prefix).list();
        }
        new S3TemplateStorage(other, "b", "/a/b").list();

        ArgumentCaptor<ListObjectsV2Request> request = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(other, org.mockito.Mockito.times(5)).listObjectsV2(request.capture());
        assertThat(request.getAllValues()).extracting(ListObjectsV2Request::prefix).containsExactly("", "", "", "", "a/b/");
    }

    @Test
    void anObjectWithoutAnEtagHasAnEmptyOne() {
        when(client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().lastModified(Instant.EPOCH).build());

        assertThat(storage.stat("a.ftl").orElseThrow().etag()).isEmpty();
    }
}
