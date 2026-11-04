package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.TemplateReferenceService;
import com.umdc.mercury.api.v1.to.ReferenceItemResponse;
import com.umdc.mercury.api.v1.to.TemplateDefinedItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Read-only lists that fill the selectors of the template and campaign forms.
 *
 * <p>Rooted under {@code /api/v1/templates} so they sit behind the same managed-client chain (Bearer with
 * {@code mercury:message:read}) and the same session-token check as the template API. The literal paths win over
 * {@code /{id}}.</p>
 */
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "templates", description = "Template Management API")
public class TemplateReferenceController {

    private final TemplateReferenceService referenceService;

    public TemplateReferenceController(TemplateReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/types")
    @Operation(summary = "List the template types", operationId = "listTemplateTypes")
    public ResponseEntity<List<ReferenceItemResponse>> templateTypes(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return ResponseEntity.ok(referenceService.listTemplateTypes());
    }

    @GetMapping("/severities")
    @Operation(summary = "List the severity types", operationId = "listSeverityTypes")
    public ResponseEntity<List<ReferenceItemResponse>> severityTypes(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return ResponseEntity.ok(referenceService.listSeverityTypes());
    }

    @GetMapping("/defined")
    @Operation(summary = "List the templates an application can use in a campaign", operationId = "listDefinedTemplates")
    public ResponseEntity<List<TemplateDefinedItemResponse>> defined(@RequestParam("applicationId") UUID applicationId,
                                                                     @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return ResponseEntity.ok(referenceService.listDefined(applicationId, sessionToken));
    }
}
