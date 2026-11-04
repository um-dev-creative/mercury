package com.umdc.mercury.template.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Templates as files in a folder of the server (the folder FreeMarker has always read).
 *
 * <p>A write goes to a temporary file in the same folder and is then moved over the target, so a reader sees the old or
 * the new content, never a mix. Writes of one name are serialized, which is what makes the etag check safe inside one
 * Mercury instance; with several instances on one shared folder, use a storage with its own conditional writes.</p>
 */
public class LocalTemplateStorage implements TemplateStorage {

    private final Path root;
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    public LocalTemplateStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<StoredTemplate> read(String name) {
        Path file = resolve(name);
        if (file == null || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            byte[] content = Files.readAllBytes(file);
            return Optional.of(new StoredTemplate(new TemplateMetadata(name, etagOf(content), Files.getLastModifiedTime(file).toInstant()), content));
        } catch (IOException _) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TemplateMetadata> stat(String name) {
        return read(name).map(StoredTemplate::metadata);
    }

    @Override
    public TemplateMetadata write(String name, byte[] content, String expectedEtag) {
        TemplateNames.require(name);
        synchronized (locks.computeIfAbsent(name, key -> new Object())) {
            if (expectedEtag != null) {
                String current = stat(name).map(TemplateMetadata::etag).orElse(ABSENT);
                if (!current.equals(expectedEtag)) {
                    throw new TemplateConflictException(name);
                }
            }
            Path target = resolve(name);
            try {
                Path temporary = Files.createTempFile(root, ".tmp-", ".part");
                try {
                    Files.write(temporary, content);
                    move(temporary, target);
                } finally {
                    Files.deleteIfExists(temporary);
                }
                return new TemplateMetadata(name, etagOf(content), Files.getLastModifiedTime(target).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException("The template could not be written", e);
            }
        }
    }

    @Override
    public void delete(String name) {
        Path file = resolve(name);
        if (file == null) {
            return;
        }
        synchronized (locks.computeIfAbsent(name, key -> new Object())) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new UncheckedIOException("The template could not be deleted", e);
            }
        }
    }

    @Override
    public List<String> list() {
        try (Stream<Path> files = Files.list(root)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(TemplateNames::isValid)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("The templates could not be listed", e);
        }
    }

    /** @return the file of a valid name inside the root, or {@code null}. */
    private Path resolve(String name) {
        if (!TemplateNames.isValid(name)) {
            return null;
        }
        Path file = root.resolve(name).normalize();
        return file.startsWith(root) && root.equals(file.getParent()) ? file : null;
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static String etagOf(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
