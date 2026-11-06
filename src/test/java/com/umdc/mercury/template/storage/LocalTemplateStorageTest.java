package com.umdc.mercury.template.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalTemplateStorageTest {

    @TempDir
    Path dir;

    private LocalTemplateStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalTemplateStorage(dir);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void readsAFileThatAlreadyExistsInTheFolder() throws IOException {
        Files.writeString(dir.resolve("hola.ftl"), "Hola ${name}");

        StoredTemplate template = storage.read("hola.ftl").orElseThrow();

        assertThat(template.text()).isEqualTo("Hola ${name}");
        assertThat(template.metadata().name()).isEqualTo("hola.ftl");
        assertThat(template.metadata().etag()).hasSize(64);
        assertThat(template.metadata().lastModified()).isNotNull();
    }

    @Test
    void readOfAMissingTemplateIsEmpty() {
        assertThat(storage.read("nada.ftl")).isEmpty();
        assertThat(storage.stat("nada.ftl")).isEmpty();
    }

    @Test
    void readOfADirectoryIsEmpty() throws IOException {
        Files.createDirectory(dir.resolve("carpeta.ftl"));

        assertThat(storage.read("carpeta.ftl")).isEmpty();
    }

    @ParameterizedTest(name = "reads of \"{0}\" find nothing")
    @ValueSource(strings = {"../secret.ftl", "a/b/c.ftl", "../a/b.ftl", "..\\x.ftl", "/etc/passwd", ".hidden.ftl", "no-extension", "x.exe", "a..b.ftl", "%2e%2e/x.ftl", ""})
    void invalidNamesAreNeverFound(String name) {
        assertThat(storage.read(name)).isEmpty();
        assertThat(storage.stat(name)).isEmpty();
    }

    @ParameterizedTest(name = "writes of \"{0}\" are refused")
    @ValueSource(strings = {"../escape.ftl", "a/b/c.ftl", "A/b.ftl", ".hidden.ftl", "x.exe"})
    void invalidNamesAreNeverWritten(String name) {
        assertThrows(IllegalArgumentException.class, () -> storage.write(name, bytes("x"), null));
    }

    @Test
    void nullNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> storage.write(null, bytes("x"), null));
        assertThat(storage.read(null)).isEmpty();
    }

    @Test
    void writeCreatesAndReturnsTheMetadata() {
        TemplateMetadata meta = storage.write("nueva.ftl", bytes("¡Hola!"), TemplateStorage.ABSENT);

        assertThat(storage.read("nueva.ftl").orElseThrow().text()).isEqualTo("¡Hola!");
        assertThat(meta.etag()).isEqualTo(storage.stat("nueva.ftl").orElseThrow().etag());
    }

    @Test
    void writeLeavesNoTemporaryFilesBehind() {
        storage.write("nueva.ftl", bytes("x"), null);
        storage.write("nueva.ftl", bytes("y"), null);

        assertThat(storage.list()).containsExactly("nueva.ftl");
        assertThat(dir.toFile().list()).containsExactly("nueva.ftl");
    }

    @Test
    void creatingOverAnExistingTemplateIsAConflict() {
        storage.write("a.ftl", bytes("1"), null);

        assertThrows(TemplateConflictException.class, () -> storage.write("a.ftl", bytes("2"), TemplateStorage.ABSENT));
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("1");
    }

    @Test
    void updateWithTheCurrentEtagSucceeds() {
        String etag = storage.write("a.ftl", bytes("1"), null).etag();

        TemplateMetadata updated = storage.write("a.ftl", bytes("2"), etag);

        assertThat(updated.etag()).isNotEqualTo(etag);
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("2");
    }

    @Test
    void updateWithAStaleEtagIsAConflictAndKeepsTheContent() {
        String first = storage.write("a.ftl", bytes("1"), null).etag();
        storage.write("a.ftl", bytes("2"), first);

        assertThrows(TemplateConflictException.class, () -> storage.write("a.ftl", bytes("3"), first));
        assertThat(storage.read("a.ftl").orElseThrow().text()).isEqualTo("2");
    }

    @Test
    void updateOfAMissingTemplateWithAnEtagIsAConflict() {
        assertThrows(TemplateConflictException.class, () -> storage.write("a.ftl", bytes("1"), "deadbeef"));
    }

    @Test
    void sameContentSameEtag() {
        assertThat(LocalTemplateStorage.etagOf(bytes("x"))).isEqualTo(LocalTemplateStorage.etagOf(bytes("x")));
        assertThat(LocalTemplateStorage.etagOf(bytes("x"))).isNotEqualTo(LocalTemplateStorage.etagOf(bytes("y")));
    }

    @Test
    void deleteRemovesAndToleratesAMissingTemplate() {
        storage.write("a.ftl", bytes("1"), null);

        storage.delete("a.ftl");
        storage.delete("a.ftl");
        storage.delete("../x.ftl");

        assertThat(storage.read("a.ftl")).isEmpty();
    }

    @Test
    void listReturnsOnlyTemplateFilesSorted() throws IOException {
        Files.writeString(dir.resolve("b.ftl"), "b");
        Files.writeString(dir.resolve("a.md"), "a");
        Files.writeString(dir.resolve("notes.txt"), "no");
        Files.createDirectory(dir.resolve("sub"));

        assertThat(storage.list()).isEqualTo(List.of("a.md", "b.ftl"));
    }

    @Test
    void concurrentUpdatesWithTheSameEtagLetExactlyOneWin() throws Exception {
        String etag = storage.write("race.ftl", bytes("0"), null).etag();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) {
                String content = "writer-" + i;
                results.add(pool.submit(() -> {
                    try {
                        storage.write("race.ftl", bytes(content), etag);
                        return true;
                    } catch (TemplateConflictException e) {
                        return false;
                    }
                }));
            }
            long winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void writesAndReadsInsideAnApplicationFolderCreatingIt() {
        TemplateMetadata saved = storage.write("iris/iris_welcome.ftl", bytes("hola"), TemplateStorage.ABSENT);

        assertThat(saved.name()).isEqualTo("iris/iris_welcome.ftl");
        assertThat(dir.resolve("iris/iris_welcome.ftl")).hasContent("hola");
        assertThat(storage.read("iris/iris_welcome.ftl")).isPresent();
        assertThat(storage.read("zerobound/iris_welcome.ftl")).isEmpty();
        assertThat(storage.read("iris_welcome.ftl")).isEmpty();
    }

    @Test
    void theSameFileNameInTwoFoldersAreTwoTemplates() {
        storage.write("iris/a.ftl", bytes("iris"), null);
        storage.write("zerobound/a.ftl", bytes("zb"), null);

        assertThat(storage.read("iris/a.ftl").orElseThrow().text()).isEqualTo("iris");
        assertThat(storage.read("zerobound/a.ftl").orElseThrow().text()).isEqualTo("zb");
    }

    @Test
    void writeInAFolderLeavesNoTemporaryFilesBehind() throws IOException {
        storage.write("iris/a.ftl", bytes("x"), null);

        try (var files = java.nio.file.Files.list(dir.resolve("iris"))) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("a.ftl");
        }
    }

    @Test
    void listIncludesTheFilesOfEveryApplicationFolderAndIgnoresOtherDirectories() throws IOException {
        storage.write("root.ftl", bytes("x"), null);
        storage.write("iris/b.ftl", bytes("x"), null);
        storage.write("zerobound/a.md", bytes("x"), null);
        java.nio.file.Files.createDirectories(dir.resolve("Not_A_Folder"));
        java.nio.file.Files.writeString(dir.resolve("Not_A_Folder/c.ftl"), "x");
        java.nio.file.Files.createDirectories(dir.resolve("iris/deeper"));
        java.nio.file.Files.writeString(dir.resolve("iris/readme.txt"), "x");

        assertThat(storage.list()).containsExactly("iris/b.ftl", "root.ftl", "zerobound/a.md");
    }

    @Test
    void deleteInAFolderRemovesOnlyThatFile() {
        storage.write("iris/a.ftl", bytes("x"), null);
        storage.write("zerobound/a.ftl", bytes("x"), null);

        storage.delete("iris/a.ftl");

        assertThat(storage.read("iris/a.ftl")).isEmpty();
        assertThat(storage.read("zerobound/a.ftl")).isPresent();
    }
}
