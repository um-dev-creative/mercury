package com.umdc.mercury.template.storage;

import com.umdc.mercury.template.markdown.MarkdownTemplates;
import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Lets FreeMarker read its templates from a {@link TemplateStorage} instead of from a folder. FreeMarker asks for names
 * such as {@code welcome_es_MX.ftl} before {@code welcome_es.ftl}; a name that is not a valid file name or is not stored
 * is simply "not found" so it falls back, as the folder loader did.
 */
public class StorageTemplateLoader implements TemplateLoader {

    private final TemplateStorage storage;

    public StorageTemplateLoader(TemplateStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public Object findTemplateSource(String name) {
        return storage.read(name).map(Source::new).orElse(null);
    }

    @Override
    public long getLastModified(Object templateSource) {
        Source source = (Source) templateSource;
        // asked again by FreeMarker on every refresh: a changed etag is a changed template, whatever the clock says
        return storage.stat(source.name).map(meta -> meta.etag().equals(source.etag) ? source.lastModified : Math.max(meta.lastModified().toEpochMilli(), source.lastModified + 1))
                .orElse(Long.MAX_VALUE);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        Source source = (Source) templateSource;
        String text = new String(source.content, Charset.forName(encoding));
        return new StringReader(MarkdownTemplates.forFreeMarker(source.name, text));
    }

    @Override
    public void closeTemplateSource(Object templateSource) {
        // nothing to release: the content is held in memory
    }

    /** A template as it was when FreeMarker found it. Two sources are the same if they name the same file. */
    private static final class Source {
        private final String name;
        private final String etag;
        private final long lastModified;
        private final byte[] content;

        private Source(StoredTemplate template) {
            this.name = template.metadata().name();
            this.etag = template.metadata().etag();
            this.lastModified = template.metadata().lastModified().toEpochMilli();
            this.content = template.content();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Source that && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
