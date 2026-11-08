package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.ApplicationTemplateFolderService;
import com.umdc.mercury.api.v1.to.ApplicationFolderResponse;
import com.umdc.mercury.api.v1.to.AssignFolderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * The folder of an application in the template storage. Under {@code /api/v1/templates}, so it sits behind the managed-client chain
 * (Bearer {@code mercury:message:read} for GET, {@code mercury:message:write} for PUT) and the session-token check.
 */
@RestController
@RequestMapping("/api/v1/templates/applications/{applicationId}/folder")
@Tag(name = "templates", description = "Template Management API")
public class ApplicationTemplateFolderController {

    private final ApplicationTemplateFolderService service;

    public ApplicationTemplateFolderController(ApplicationTemplateFolderService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get the template folder of an application", operationId = "getApplicationTemplateFolder")
    public ResponseEntity<ApplicationFolderResponse> get(@PathVariable UUID applicationId,
                                                         @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return ResponseEntity.ok(service.get(applicationId, sessionToken));
    }

    @PutMapping
    @Operation(summary = "Set the template folder of an application", operationId = "setApplicationTemplateFolder")
    public ResponseEntity<ApplicationFolderResponse> assign(@PathVariable UUID applicationId,
                                                            @Valid @RequestBody AssignFolderRequest request,
                                                            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return ResponseEntity.ok(service.assign(applicationId, request.folder(), sessionToken));
    }
}
