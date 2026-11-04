package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.api.v1.service.TemplateContentService;
import com.umdc.mercury.api.v1.to.PreviewTemplateRequest;
import com.umdc.mercury.api.v1.to.PreviewTemplateResponse;
import com.umdc.mercury.api.v1.to.SaveTemplateContentRequest;
import com.umdc.mercury.api.v1.to.TemplateContentResponse;
import com.umdc.mercury.api.v1.to.TemplateProblemsResponse;
import com.umdc.mercury.api.v1.to.TemplateVersionResponse;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.mercury.template.content.TemplateSyntaxException;
import com.umdc.mercury.template.storage.TemplateStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Content of the templates: read, save (with optimistic locking through the ETag) and preview.
 *
 * <p>Under {@code /api/v1/templates}, so it sits behind the managed-client chain (Bearer {@code mercury:message:read}
 * for GET, {@code mercury:message:write} for the rest) and the session-token check, like the template API.</p>
 */
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "templates", description = "Template Management API")
public class TemplateContentController {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or missing session token.";
    private static final String ANY = "*";

    private final TemplateContentService contentService;
    private final SessionJwtServiceImpl sessionJwtService;

    public TemplateContentController(TemplateContentService contentService, SessionJwtServiceImpl sessionJwtService) {
        this.contentService = contentService;
        this.sessionJwtService = sessionJwtService;
    }

    @GetMapping("/{id}/content")
    @Operation(summary = "Read the content of a template", operationId = "getTemplateContent")
    public ResponseEntity<TemplateContentResponse> get(@PathVariable("id") UUID id,
                                                       @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        verified(sessionToken);
        TemplateContentResponse content = contentService.read(id, sessionToken);
        return ResponseEntity.ok().eTag(quote(content.etag())).body(content);
    }

    /**
     * Saves the content. {@code If-Match: "<etag>"} saves over the version that was read; {@code If-None-Match: *}
     * creates the file and fails if it already exists. Neither is {@code 428}: a save never overwrites blindly.
     */
    @PutMapping("/{id}/content")
    @Operation(summary = "Save the content of a template", operationId = "putTemplateContent")
    public ResponseEntity<Object> put(@PathVariable("id") UUID id,
                                      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
                                      @RequestParam(value = "confirmSystem", defaultValue = "false") boolean confirmSystem,
                                      @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
                                      @Valid @RequestBody SaveTemplateContentRequest request) {
        UUID requesterId = verified(sessionToken);
        String expected = expectedEtag(ifMatch, ifNoneMatch);
        if (expected == null) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body(new TemplateProblemsResponse("Send If-Match with the etag you read, or If-None-Match: * to create the file", List.of()));
        }
        try {
            TemplateContentResponse saved = contentService.save(id, request.content(), expected, confirmSystem, requesterId, sessionToken);
            return ResponseEntity.ok().eTag(quote(saved.etag())).body(saved);
        } catch (TemplateSyntaxException e) {
            return ResponseEntity.unprocessableEntity().body(new TemplateProblemsResponse(e.getMessage(), e.problems()));
        }
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "List the saved versions of a template's content", operationId = "listTemplateVersions")
    public ResponseEntity<List<TemplateVersionResponse>> versions(@PathVariable("id") UUID id,
                                                                  @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        verified(sessionToken);
        return ResponseEntity.ok(contentService.versions(id, sessionToken));
    }

    @GetMapping("/{id}/versions/{version}")
    @Operation(summary = "Read one saved version", operationId = "getTemplateVersion")
    public ResponseEntity<TemplateVersionResponse> version(@PathVariable("id") UUID id, @PathVariable("version") int version,
                                                           @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        verified(sessionToken);
        return ResponseEntity.ok(contentService.version(id, version, sessionToken));
    }

    /** Saves an older version's content as a new version. Needs {@code If-Match} with the etag of the current content. */
    @PostMapping("/{id}/versions/{version}/restore")
    @Operation(summary = "Restore an older version", operationId = "restoreTemplateVersion")
    public ResponseEntity<Object> restore(@PathVariable("id") UUID id, @PathVariable("version") int version,
                                          @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                          @RequestParam(value = "confirmSystem", defaultValue = "false") boolean confirmSystem,
                                          @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        UUID requesterId = verified(sessionToken);
        String expected = expectedEtag(ifMatch, null);
        if (expected == null) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body(new TemplateProblemsResponse("Send If-Match with the etag of the current content", List.of()));
        }
        try {
            TemplateContentResponse restored = contentService.restore(id, version, expected, confirmSystem, requesterId, sessionToken);
            return ResponseEntity.ok().eTag(quote(restored.etag())).body(restored);
        } catch (TemplateSyntaxException e) {
            return ResponseEntity.unprocessableEntity().body(new TemplateProblemsResponse(e.getMessage(), e.problems()));
        }
    }

    @PostMapping("/preview")
    @Operation(summary = "Render a template that is not saved yet", operationId = "previewTemplate")
    public ResponseEntity<PreviewTemplateResponse> preview(@Valid @RequestBody PreviewTemplateRequest request,
                                                           @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        verified(sessionToken);
        return ResponseEntity.ok(contentService.preview(request, sessionToken));
    }

    private UUID verified(String sessionToken) {
        return sessionJwtService.getVerifiedUid(sessionToken).orElseThrow(() -> new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE));
    }

    /** @return the etag the caller expects ({@code ""} to create), or {@code null} if it sent no precondition. */
    static String expectedEtag(String ifMatch, String ifNoneMatch) {
        if (ANY.equals(ifNoneMatch == null ? null : ifNoneMatch.trim())) {
            return TemplateStorage.ABSENT;
        }
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        String value = ifMatch.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    private static String quote(String etag) {
        return "\"" + etag + "\"";
    }
}
