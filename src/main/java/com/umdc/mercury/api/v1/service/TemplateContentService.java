package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.PreviewTemplateRequest;
import com.umdc.mercury.api.v1.to.PreviewTemplateResponse;
import com.umdc.mercury.api.v1.to.TemplateContentResponse;
import com.umdc.mercury.api.v1.to.TemplateVersionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Reads and writes the content of a template (the file its {@code location} names) and previews content that is not
 * saved yet. Every operation needs the template permission on the template's application.
 */
public interface TemplateContentService {

    /**
     * @param templateId   an active template.
     * @param sessionToken the caller's session token.
     * @return the content, which is empty (and {@code exists=false}) while the file has not been written.
     * @throws com.umdc.mercury.api.v1.exception.TemplateNotFoundException if the template is unknown or inactive, or its
     *         {@code location} is not a file name.
     * @throws com.umdc.mercury.api.v1.exception.ForbiddenException        if the caller lacks the permission.
     */
    TemplateContentResponse read(UUID templateId, String sessionToken);

    /**
     * Stores the content if the file is still as the caller read it.
     *
     * @param templateId   an active template.
     * @param content      the whole template as written.
     * @param confirmSystem whether the caller confirmed changing a system template (without it, a system template is {@code 403}).
     * @param expectedEtag etag the caller read, or {@link com.umdc.mercury.template.storage.TemplateStorage#ABSENT} to
     *                     create the file.
     * @param requesterId  verified id of the caller, for the log.
     * @param sessionToken the caller's session token.
     * @return the stored content.
     * @throws com.umdc.mercury.template.content.TemplateSyntaxException  if FreeMarker cannot read the content.
     * @throws com.umdc.mercury.template.content.ContentTooLargeException if it exceeds the limit.
     * @throws com.umdc.mercury.template.storage.TemplateConflictException if the file changed since it was read.
     * @throws com.umdc.mercury.api.v1.exception.ForbiddenException        if the caller lacks the permission or the
     *         template is a system template.
     */
    TemplateContentResponse save(UUID templateId, String content, String expectedEtag, boolean confirmSystem, UUID requesterId, String sessionToken);

    /**
     * @param templateId an active template.
     * @param sessionToken the caller's session token.
     * @return the recorded versions of the content, newest first, without the content itself.
     */
    List<TemplateVersionResponse> versions(UUID templateId, String sessionToken);

    /**
     * @param templateId an active template.
     * @param version    version number.
     * @param sessionToken the caller's session token.
     * @return that version with its content.
     * @throws com.umdc.mercury.api.v1.exception.TemplateNotFoundException if there is no such version.
     */
    TemplateVersionResponse version(UUID templateId, int version, String sessionToken);

    /**
     * Saves an older version's content as a new version (the history keeps going forward; nothing is erased).
     *
     * @param expectedEtag etag of the current content the caller saw.
     * @param confirmSystem whether the caller confirmed changing a system template.
     * @return the content after the restore.
     */
    TemplateContentResponse restore(UUID templateId, int version, String expectedEtag, boolean confirmSystem, UUID requesterId, String sessionToken);

    /**
     * Renders content with sample parameters. Nothing is stored or sent.
     *
     * @param request      content and parameters.
     * @param sessionToken the caller's session token.
     * @return the output or the problems.
     */
    PreviewTemplateResponse preview(PreviewTemplateRequest request, String sessionToken);
}
