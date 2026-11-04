package com.umdc.mercury.template.content;

import java.util.List;

/**
 * A template that FreeMarker cannot parse. Carries every problem found so an editor can show them.
 */
public class TemplateSyntaxException extends RuntimeException {

    private final transient List<TemplateProblem> problems;

    public TemplateSyntaxException(List<TemplateProblem> problems) {
        super("The template has syntax errors");
        this.problems = List.copyOf(problems);
    }

    public List<TemplateProblem> problems() {
        return problems;
    }
}
