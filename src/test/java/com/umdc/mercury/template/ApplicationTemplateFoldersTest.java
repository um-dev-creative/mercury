package com.umdc.mercury.template;

import com.umdc.mercury.api.v1.exception.FolderInUseException;
import com.umdc.mercury.jpa.sql.entity.ApplicationTemplateFolderEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationTemplateFolderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationTemplateFoldersTest {

    private final ApplicationTemplateFolderRepository repository = mock(ApplicationTemplateFolderRepository.class);
    private final AtomicLong millis = new AtomicLong(1_000_000);
    private final Clock clock = new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    };
    private final UUID iris = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final ApplicationTemplateFolders folders = new ApplicationTemplateFolders(repository, 60, clock);

    private ApplicationTemplateFolderEntity row(UUID application, String folder) {
        return new ApplicationTemplateFolderEntity(application, folder, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void theFolderIsReadFromTheRegistry() {
        when(repository.findById(iris)).thenReturn(Optional.of(row(iris, "iris")));

        assertThat(folders.folderOf(iris)).contains("iris");
        assertThat(folders.nameOf(iris, "iris_welcome.ftl")).isEqualTo("iris/iris_welcome.ftl");
    }

    @Test
    void anApplicationWithoutARowKeepsTheBareFileName() {
        assertThat(folders.folderOf(other)).isEmpty();
        assertThat(folders.nameOf(other, "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.folderOf(null)).isEmpty();
        assertThat(folders.nameOf(null, "a.ftl")).isEqualTo("a.ftl");
    }

    @Test
    void aLocationThatAlreadyHasAFolderIsLeftAlone() {
        assertThat(folders.nameOf(iris, "x/a.ftl")).isEqualTo("x/a.ftl");
        assertThat(folders.nameOf(iris, null)).isNull();
    }

    @Test
    void theRegistryIsReadAgainOnlyWhenTheCacheExpires() {
        when(repository.findById(iris)).thenReturn(Optional.of(row(iris, "iris")));

        folders.folderOf(iris);
        folders.folderOf(iris);
        verify(repository, times(1)).findById(iris);

        millis.addAndGet(61_000);
        when(repository.findById(iris)).thenReturn(Optional.of(row(iris, "renamed")));

        assertThat(folders.folderOf(iris)).contains("renamed");
        verify(repository, times(2)).findById(iris);
    }

    @Test
    void ensureFolderGivesANewApplicationAFolderNamedLikeItsId() {
        String folder = folders.ensureFolder(other);

        assertThat(folder).isEqualTo(other.toString());
        verify(repository).saveAndFlush(any(ApplicationTemplateFolderEntity.class));
    }

    @Test
    void ensureFolderKeepsTheFolderAnApplicationAlreadyHas() {
        when(repository.findById(iris)).thenReturn(Optional.of(row(iris, "iris")));

        assertThat(folders.ensureFolder(iris)).isEqualTo("iris");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void ensureFolderReadsTheWinnersRowWhenAnotherInstanceRegisteredFirst() {
        when(repository.findById(other)).thenReturn(Optional.empty(), Optional.of(row(other, "winner")));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(folders.ensureFolder(other)).isEqualTo("winner");
    }

    @Test
    void ensureFolderReportsAClashItCannotResolve() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> folders.ensureFolder(other)).isInstanceOf(FolderInUseException.class);
    }

    @Test
    void assignCreatesTheRowAndSeesItAtOnce() {
        assertThat(folders.assign(iris, " Iris ".toLowerCase())).isEqualTo("iris");
        verify(repository).saveAndFlush(any(ApplicationTemplateFolderEntity.class));
    }

    @Test
    void assignReplacesTheFolderOfAnApplicationThatHasOne() {
        var existing = row(iris, "old");
        when(repository.findById(iris)).thenReturn(Optional.of(existing));

        folders.assign(iris, "new-name");

        assertThat(existing.getFolder()).isEqualTo("new-name");
        verify(repository).saveAndFlush(existing);
        when(repository.findById(iris)).thenReturn(Optional.of(existing));
        assertThat(folders.folderOf(iris)).contains("new-name");
    }

    @Test
    void assignKeepingTheSameFolderIsAllowed() {
        var existing = row(iris, "iris");
        when(repository.findById(iris)).thenReturn(Optional.of(existing));
        when(repository.findByFolder("iris")).thenReturn(Optional.of(existing));

        assertThat(folders.assign(iris, "iris")).isEqualTo("iris");
    }

    @Test
    void aFolderOfAnotherApplicationIsRefused() {
        when(repository.findByFolder("iris")).thenReturn(Optional.of(row(other, "iris")));

        assertThatThrownBy(() -> folders.assign(iris, "iris")).isInstanceOf(FolderInUseException.class).hasMessageContaining("iris");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void aClashDetectedByTheDatabaseIsRefusedToo() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> folders.assign(iris, "iris")).isInstanceOf(FolderInUseException.class);
    }

    @Test
    void anInvalidFolderNameIsRefused() {
        assertThatThrownBy(() -> folders.assign(iris, "Not Valid")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not a valid folder");
        assertThatThrownBy(() -> folders.assign(iris, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> folders.assign(iris, "../x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theDefaultFolderIsTheApplicationId() {
        assertThat(ApplicationTemplateFolders.defaultFolder(iris)).isEqualTo(iris.toString());
        assertThat(com.umdc.mercury.template.storage.TemplateNames.isFolder(ApplicationTemplateFolders.defaultFolder(iris))).isTrue();
    }

    @Test
    void theProductionConstructorBuildsAWorkingInstance() {
        var production = new ApplicationTemplateFolders(repository, 0);
        when(repository.findById(iris)).thenReturn(Optional.of(row(iris, "iris")));

        assertThat(production.folderOf(iris)).contains("iris");
        assertThat(production.folderOf(iris)).contains("iris");
        verify(repository, times(2)).findById(iris);
    }

}
