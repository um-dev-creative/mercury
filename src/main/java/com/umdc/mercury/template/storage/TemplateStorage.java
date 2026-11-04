package com.umdc.mercury.template.storage;

import java.util.List;
import java.util.Optional;

/**
 * Where the content of the templates lives. Mercury reads and writes it only through this interface, so the place can
 * be a folder on the server or a bucket in the cloud without the rest of the code knowing.
 *
 * <p>A name is a plain file name accepted by {@link TemplateNames}; any other name is {@link IllegalArgumentException}
 * on writes and simply "not found" on reads. Implementations must be safe for concurrent use and write atomically: a
 * reader never sees half a file.</p>
 */
public interface TemplateStorage {

    /** Etag of "nothing stored yet", for {@link #write} when the caller expects to create the template. */
    String ABSENT = "";

    /** @return the template, or empty if it does not exist or the name is not valid. */
    Optional<StoredTemplate> read(String name);

    /** @return name, etag and last change of the template, or empty if it does not exist. Cheap: no content. */
    Optional<TemplateMetadata> stat(String name);

    /**
     * Stores the content.
     *
     * @param name         valid template file name.
     * @param content      UTF-8 bytes.
     * @param expectedEtag the etag the caller read ({@link #ABSENT} to require that the template does not exist yet), or
     *                     {@code null} to overwrite whatever is there.
     * @return the stored template's metadata.
     * @throws IllegalArgumentException  if the name is not valid.
     * @throws TemplateConflictException if {@code expectedEtag} no longer matches.
     */
    TemplateMetadata write(String name, byte[] content, String expectedEtag);

    /** Removes the template. Removing one that does not exist is not an error. */
    void delete(String name);

    /** @return the names of all stored templates, sorted. */
    List<String> list();
}
