package com.umdc.mercury.template.content;

import com.umdc.mercury.config.FreeMarkerConfig;
import com.umdc.mercury.template.markdown.MarkdownTemplates;
import com.umdc.mercury.template.storage.StorageTemplateLoader;
import com.umdc.mercury.template.storage.TemplateStorage;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders a template that is not saved yet with sample parameters, so an editor can show what a message will look like.
 * Nothing is sent or stored. The output and the time are limited, so a loop that never ends cannot hold Mercury.
 */
public class TemplatePreview {

    private final Configuration configuration;
    private final int maxOutputChars;
    private final long timeoutNanos;

    /** What a preview produced. {@code output} is empty when there are problems. */
    public record Result(String output, List<TemplateProblem> problems) {
        public boolean ok() {
            return problems.isEmpty();
        }
    }

    /**
     * @param storage        where {@code <#include>} reads other templates from
     * @param maxOutputChars most characters a preview may produce
     * @param timeoutMillis  longest a preview may write for
     */
    public TemplatePreview(TemplateStorage storage, int maxOutputChars, long timeoutMillis) {
        this.configuration = FreeMarkerConfig.hardened(new StorageTemplateLoader(storage));
        // an error is reported to the person, not written into the output nor into the server log
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setLogTemplateExceptions(false);
        this.maxOutputChars = maxOutputChars;
        this.timeoutNanos = timeoutMillis * 1_000_000L;
    }

    /**
     * @param name    file name, which tells whether the source is Markdown
     * @param content the template as written
     * @param params  sample parameters
     * @return the output, or what went wrong
     */
    public Result render(String name, String content, Map<String, Object> params) {
        Template template;
        try {
            template = new Template(name, new StringReader(MarkdownTemplates.forFreeMarker(name, content)), configuration);
        } catch (ParseException e) {
            return failed(Math.max(0, e.getLineNumber() - MarkdownTemplates.lineOffset(name)), e.getColumnNumber(), e.getEditorMessage());
        } catch (IOException _) {
            return failed(0, 0, "The template could not be read");
        }
        try (LimitedWriter out = new LimitedWriter(maxOutputChars, System.nanoTime() + timeoutNanos)) {
            template.process(Objects.isNull(params) ? Map.of() : params, out);
            return new Result(out.toString(), List.of());
        } catch (TemplateException e) {
            return failed(Math.max(0, e.getLineNumber() - MarkdownTemplates.lineOffset(name)), e.getColumnNumber(), e.getMessageWithoutStackTop());
        } catch (IOException e) {
            return failed(0, 0, e.getMessage());
        }
    }

    private static Result failed(int line, int column, String message) {
        return new Result("", List.of(new TemplateProblem(line, Math.max(0, column), message)));
    }

    /** Stops a template that writes too much or for too long. */
    @SuppressWarnings("PMD.AvoidStringBufferField") // short-lived and bounded by `max`
    private static final class LimitedWriter extends Writer {
        private final StringBuilder buffer = new StringBuilder();
        private final int max;
        private final long deadline;

        private LimitedWriter(int max, long deadline) {
            super();
            this.max = max;
            this.deadline = deadline;
        }

        @Override
        public void write(char[] chars, int offset, int length) throws IOException {
            if (System.nanoTime() > deadline) {
                throw new IOException("The preview took too long");
            }
            if (buffer.length() + length > max) {
                throw new IOException("The preview is longer than " + max + " characters");
            }
            buffer.append(chars, offset, length);
        }

        @Override
        public void flush() {
            // everything stays in memory
        }

        @Override
        public void close() {
            // nothing to release
        }

        @Override
        public String toString() {
            return buffer.toString();
        }
    }
}
