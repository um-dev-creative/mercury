package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.TemplateNotFoundException;
import com.umdc.mercury.api.v1.to.PreviewTemplateRequest;
import com.umdc.mercury.api.v1.to.PreviewTemplateResponse;
import com.umdc.mercury.api.v1.to.TemplateContentResponse;
import com.umdc.mercury.api.v1.to.TemplateVersionResponse;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateRepository;
import com.umdc.mercury.template.content.ContentTooLargeException;
import com.umdc.mercury.template.content.TemplateSyntaxException;
import com.umdc.mercury.template.storage.LocalTemplateStorage;
import com.umdc.mercury.template.storage.TemplateConflictException;
import com.umdc.mercury.template.storage.TemplateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateContentServiceImplTest {

    private static final String TOKEN = "session";

    @TempDir
    Path dir;

    @Mock private TemplateRepository repository;
    @Mock private ApplicationAuthorizationService authorization;
    @Mock private TemplateVersions versions;

    private com.umdc.mercury.template.ApplicationTemplateFolders folders;
    private TemplateStorage storage;
    private TemplateContentServiceImpl service;
    private final UUID templateId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();
    private final UUID requester = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        storage = new LocalTemplateStorage(dir);
        folders = com.umdc.mercury.template.TemplateFolderFixtures.noFolders();
        service = new TemplateContentServiceImpl(repository, storage, versions, folders, authorization, "TEMPLATE_MANAGE", 200,
                List.of("zb_", " ", "iris_"), 1000, 2000);
    }

    private TemplateEntity template(String location, Boolean active) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(applicationId);
        TemplateEntity entity = new TemplateEntity();
        entity.setId(templateId);
        entity.setLocation(location);
        entity.setActive(active);
        entity.setApplication(application);
        return entity;
    }

    private void found(String location) {
        when(repository.findById(templateId)).thenReturn(Optional.of(template(location, true)));
    }

    @Test
    void readOfAFileThatDoesNotExistYetIsEmptyAndEditable() {
        found("nueva.ftl");

        TemplateContentResponse response = service.read(templateId, TOKEN);

        verify(authorization).assertPermission(TOKEN, applicationId, "TEMPLATE_MANAGE");
        assertThat(response.exists()).isFalse();
        assertThat(response.content()).isEmpty();
        assertThat(response.etag()).isEqualTo(TemplateStorage.ABSENT);
        assertThat(response.lastModified()).isNull();
        assertThat(response.system()).isFalse();
        assertThat(response.format()).isEqualTo("FREEMARKER");
    }

    @Test
    void readReturnsTheContentEtagAndParameters() {
        found("hola.md");
        storage.write("hola.md", "# Hola ${name}".getBytes(StandardCharsets.UTF_8), null);

        TemplateContentResponse response = service.read(templateId, TOKEN);

        assertThat(response.exists()).isTrue();
        assertThat(response.content()).isEqualTo("# Hola ${name}");
        assertThat(response.etag()).hasSize(64);
        assertThat(response.format()).isEqualTo("MARKDOWN");
        assertThat(response.parameters()).containsExactly("name");
        assertThat(response.lastModified()).isNotNull();
    }

    @Test
    void systemTemplatesAreReadOnlyInTheResponse() {
        found("zb_debt_due_es.ftl");

        assertThat(service.read(templateId, TOKEN).system()).isTrue();
    }

    @Test
    void anUnknownTemplateIs404() {
        when(repository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(TemplateNotFoundException.class, () -> service.read(templateId, TOKEN));
    }

    @Test
    void anInactiveTemplateIs404() {
        when(repository.findById(templateId)).thenReturn(Optional.of(template("a.ftl", false)));

        assertThrows(TemplateNotFoundException.class, () -> service.read(templateId, TOKEN));
    }

    @Test
    void aLocationThatIsNotAFileNameIs404() {
        found("s3://bucket/welcome.html");

        assertThrows(TemplateNotFoundException.class, () -> service.read(templateId, TOKEN));
    }

    @Test
    void withoutPermissionNothingIsReadOrWritten() {
        found("a.ftl");
        doThrow(new ForbiddenException("no")).when(authorization).assertPermission(any(), any(), any());

        assertThrows(ForbiddenException.class, () -> service.read(templateId, TOKEN));
        assertThrows(ForbiddenException.class, () -> service.save(templateId, "x", TemplateStorage.ABSENT, false, requester, TOKEN));
        assertThat(storage.read("a.ftl")).isEmpty();
    }

    @Test
    void saveCreatesTheFileWhenAbsentIsExpected() {
        found("nueva.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        TemplateContentResponse saved = service.save(templateId, "Hola ${name}", TemplateStorage.ABSENT, false, requester, TOKEN);

        assertThat(storage.read("nueva.ftl").orElseThrow().text()).isEqualTo("Hola ${name}");
        assertThat(saved.exists()).isTrue();
        assertThat(saved.etag()).hasSize(64);
        assertThat(saved.parameters()).containsExactly("name");
        verify(repository).save(any());
    }

    @Test
    void saveOverTheVersionThatWasReadSucceedsAndAStaleOneIsAConflict() {
        found("a.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        String first = service.save(templateId, "1", TemplateStorage.ABSENT, false, requester, TOKEN).etag();
        service.save(templateId, "2", first, false, requester, TOKEN);

        assertThrows(TemplateConflictException.class, () -> service.save(templateId, "3", first, false, requester, TOKEN));
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("2");
    }

    @Test
    void savingASystemTemplateNeedsConfirmation() {
        found("iris_verification_es.ftl");

        assertThrows(ForbiddenException.class, () -> service.save(templateId, "x", null, false, requester, TOKEN));
        assertThat(storage.read("iris_verification_es.ftl")).isEmpty();
        verify(repository, never()).save(any());
        verify(versions, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    void aConfirmedSystemTemplateIsSavedAndItsOriginalContentIsKeptAsABaseline() {
        found("iris_verification_es.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        storage.write("iris_verification_es.ftl", "original".getBytes(StandardCharsets.UTF_8), null);
        when(versions.hasAny(templateId)).thenReturn(false);

        TemplateContentResponse saved = service.save(templateId, "cambiado", null, true, requester, TOKEN);

        assertThat(storage.read("iris_verification_es.ftl").orElseThrow().text()).isEqualTo("cambiado");
        assertThat(saved.system()).isTrue();
        verify(versions).append(eq(templateId), eq("original"), any(), eq(TemplateVersions.BASELINE), eq(null));
        verify(versions).append(eq(templateId), eq("cambiado"), eq(saved.etag()), eq(TemplateVersions.SAVE), eq(requester));
    }

    @Test
    void noBaselineIsRecordedWhenThereIsHistoryOrNoFile() {
        found("a.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.save(templateId, "1", TemplateStorage.ABSENT, false, requester, TOKEN);
        verify(versions, never()).append(any(), any(), any(), eq(TemplateVersions.BASELINE), any());

        when(versions.hasAny(templateId)).thenReturn(true);
        String etag = storage.read("a.ftl").orElseThrow().metadata().etag();
        service.save(templateId, "2", etag, false, requester, TOKEN);
        verify(versions, never()).append(any(), any(), any(), eq(TemplateVersions.BASELINE), any());
    }

    @Test
    void everySaveRecordsAVersion() {
        found("a.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        TemplateContentResponse saved = service.save(templateId, "uno", TemplateStorage.ABSENT, false, requester, TOKEN);

        verify(versions).append(templateId, "uno", saved.etag(), TemplateVersions.SAVE, requester);
    }

    @Test
    void versionsAndOneVersionNeedThePermissionToo() {
        found("a.ftl");
        UUID other = UUID.randomUUID();
        when(versions.list(templateId)).thenReturn(List.of(new TemplateVersionResponse(2, "e2", 3, "SAVE", other, null, null)));
        when(versions.get(templateId, 2)).thenReturn(new TemplateVersionResponse(2, "e2", 3, "SAVE", other, null, "abc"));

        assertThat(service.versions(templateId, TOKEN)).hasSize(1);
        assertThat(service.version(templateId, 2, TOKEN).content()).isEqualTo("abc");
        verify(authorization, org.mockito.Mockito.times(2)).assertPermission(TOKEN, applicationId, "TEMPLATE_MANAGE");
    }

    @Test
    void restoreSavesTheOldContentAsANewVersionOverTheCurrentEtag() {
        found("a.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        String current = service.save(templateId, "actual", TemplateStorage.ABSENT, false, requester, TOKEN).etag();
        when(versions.get(templateId, 1)).thenReturn(new TemplateVersionResponse(1, "e1", 5, "SAVE", requester, null, "antiguo"));

        TemplateContentResponse restored = service.restore(templateId, 1, current, false, requester, TOKEN);

        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("antiguo");
        assertThat(restored.content()).isEqualTo("antiguo");
        verify(versions).append(templateId, "antiguo", restored.etag(), TemplateVersions.RESTORE, requester);
    }

    @Test
    void restoreOverAStaleEtagIsAConflictAndChangesNothing() {
        found("a.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        service.save(templateId, "actual", TemplateStorage.ABSENT, false, requester, TOKEN);
        when(versions.get(templateId, 1)).thenReturn(new TemplateVersionResponse(1, "e1", 5, "SAVE", requester, null, "antiguo"));

        assertThrows(TemplateConflictException.class, () -> service.restore(templateId, 1, "stale", false, requester, TOKEN));
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("actual");
    }

    @Test
    void restoreOfASystemTemplateNeedsConfirmation() {
        found("zb_x_es.ftl");
        when(versions.get(templateId, 1)).thenReturn(new TemplateVersionResponse(1, "e1", 5, "BASELINE", null, null, "original"));

        assertThrows(ForbiddenException.class, () -> service.restore(templateId, 1, "x", false, requester, TOKEN));
    }

    @Test
    void saveRefusesContentThatIsTooLarge() {
        found("a.ftl");

        assertThrows(ContentTooLargeException.class, () -> service.save(templateId, "x".repeat(201), TemplateStorage.ABSENT, false, requester, TOKEN));
        assertThat(storage.read("a.ftl")).isEmpty();
    }

    @Test
    void saveCountsBytesNotCharacters() {
        found("a.ftl");

        assertThrows(ContentTooLargeException.class, () -> service.save(templateId, "é".repeat(101), TemplateStorage.ABSENT, false, requester, TOKEN));
    }

    @Test
    void saveRefusesASyntaxErrorAndKeepsTheOldContent() {
        found("a.ftl");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        String etag = service.save(templateId, "bueno", TemplateStorage.ABSENT, false, requester, TOKEN).etag();

        TemplateSyntaxException error = assertThrows(TemplateSyntaxException.class, () -> service.save(templateId, "<#if x>", etag, false, requester, TOKEN));

        assertThat(error.problems()).isNotEmpty();
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("bueno");
    }

    @Test
    void previewChecksThePermissionOnTheApplicationAndRenders() {
        PreviewTemplateResponse response = service.preview(
                new PreviewTemplateRequest(applicationId, "p.md", "# Hola ${name}", Map.of("name", "Ana")), TOKEN);

        verify(authorization).assertPermission(TOKEN, applicationId, "TEMPLATE_MANAGE");
        assertThat(response.ok()).isTrue();
        assertThat(response.format()).isEqualTo("MARKDOWN");
        assertThat(response.output()).contains("<h1>Hola Ana</h1>");
        assertThat(response.parameters()).containsExactly("name");
    }

    @Test
    void previewReportsProblemsInsteadOfFailing() {
        PreviewTemplateResponse response = service.preview(new PreviewTemplateRequest(applicationId, "p.ftl", "<#if x>", Map.of()), TOKEN);

        assertThat(response.ok()).isFalse();
        assertThat(response.problems()).isNotEmpty();
        assertThat(response.output()).isEmpty();
    }

    @Test
    void previewRefusesAnInvalidNameAndTooMuchContent() {
        assertThrows(IllegalArgumentException.class,
                () -> service.preview(new PreviewTemplateRequest(applicationId, "../x.ftl", "x", Map.of()), TOKEN));
        assertThrows(ContentTooLargeException.class,
                () -> service.preview(new PreviewTemplateRequest(applicationId, "p.ftl", "x".repeat(201), Map.of()), TOKEN));
    }

    @Test
    void previewWithoutPermissionRendersNothing() {
        doThrow(new ForbiddenException("no")).when(authorization).assertPermission(any(), any(), any());

        assertThrows(ForbiddenException.class,
                () -> service.preview(new PreviewTemplateRequest(applicationId, "p.ftl", "x", Map.of()), TOKEN));
    }

    @Test
    void contentOfAnApplicationWithAFolderIsReadAndSavedInsideThatFolder() {
        var inFolders = com.umdc.mercury.template.TemplateFolderFixtures.folders(applicationId, "IRIS");
        var foldered = new TemplateContentServiceImpl(repository, storage, versions, inFolders, authorization, "TEMPLATE_MANAGE", 200,
                List.of("zb_", " ", "iris_"), 1000, 2000);
        found("welcome.ftl");
        storage.write("iris/welcome.ftl", "old ${name}".getBytes(StandardCharsets.UTF_8), null);
        storage.write("welcome.ftl", "root".getBytes(StandardCharsets.UTF_8), null);

        TemplateContentResponse read = foldered.read(templateId, TOKEN);
        assertThat(read.content()).isEqualTo("old ${name}");
        assertThat(read.name()).isEqualTo("welcome.ftl");

        foldered.save(templateId, "new ${name}", read.etag(), false, requester, TOKEN);

        assertThat(storage.read("iris/welcome.ftl").orElseThrow().text()).isEqualTo("new ${name}");
        assertThat(storage.read("welcome.ftl").orElseThrow().text()).isEqualTo("root");
    }

    @Test
    void aLocationThatAlreadyCarriesAFolderIsNotAnEditableFileName() {
        found("iris/welcome.ftl");

        org.junit.jupiter.api.Assertions.assertThrows(TemplateNotFoundException.class,
                () -> service.read(templateId, TOKEN));
    }
}
