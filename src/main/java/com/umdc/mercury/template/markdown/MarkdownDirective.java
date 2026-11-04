package com.umdc.mercury.template.markdown;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * {@code <@markdown>…</@markdown>}: FreeMarker fills in the body first (parameters, conditions, loops) and the result is
 * then read as Markdown and written as HTML. {@link MarkdownTemplates} wraps every {@code .md} template in it.
 */
public class MarkdownDirective implements TemplateDirectiveModel {

    public MarkdownDirective() {
        // stateless: one instance serves every template
    }

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        if (!params.isEmpty() || loopVars.length > 0) {
            throw new TemplateModelException("markdown takes no parameters");
        }
        StringWriter filled = new StringWriter();
        if (body != null) {
            body.render(filled);
        }
        env.getOut().write(MarkdownRenderer.toHtml(filled.toString()));
    }
}
