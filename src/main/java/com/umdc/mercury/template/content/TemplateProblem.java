package com.umdc.mercury.template.content;

/**
 * Something wrong with a template, located so an editor can point at it.
 *
 * @param line    Line of the template the person wrote, from 1; 0 when it is not tied to a line.
 * @param column  Column from 1; 0 when unknown.
 * @param message What is wrong, without the template content.
 */
public record TemplateProblem(int line, int column, String message) {
}
