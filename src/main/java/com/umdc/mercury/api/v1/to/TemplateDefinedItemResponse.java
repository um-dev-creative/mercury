package com.umdc.mercury.api.v1.to;

import java.util.UUID;

/**
 * A template an application can put in a campaign (a {@code template_defined} row) with the base template it uses.
 *
 * @param id               Identifier a campaign takes as {@code templateId}.
 * @param templateId       Identifier of the base template ({@code mercury.templates}).
 * @param description      Description of the base template.
 * @param location         File name of the template content.
 * @param fileFormat       Format of the content (for example {@code FTL} or {@code HTML}).
 * @param templateTypeName Name of the template type.
 */
public record TemplateDefinedItemResponse(UUID id, UUID templateId, String description, String location,
                                          String fileFormat, String templateTypeName) {
}
