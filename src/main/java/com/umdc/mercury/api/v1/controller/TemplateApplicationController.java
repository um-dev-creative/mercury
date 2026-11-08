package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.TemplateApplicationService;
import com.umdc.mercury.api.v1.to.TemplateApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// The applications whose templates the caller may manage. Rooted under `/api/v1/templates` so it sits behind the same
/// managed-client chain and session-token check as the template API; the literal path wins over `/{id}`.
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "templates", description = "Template Management API")
public class TemplateApplicationController {

    private final TemplateApplicationService applicationService;

    public TemplateApplicationController(TemplateApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/applications")
    @Operation(summary = "List the applications whose templates the caller may manage", operationId = "listManageableApplications")
    public ResponseEntity<List<TemplateApplicationResponse>> manageable(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return ResponseEntity.ok(applicationService.listManageable(sessionToken));
    }
}
