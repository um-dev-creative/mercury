package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.ReferenceItemResponse;
import com.umdc.mercury.api.v1.to.TemplateDefinedItemResponse;

import java.util.List;
import java.util.UUID;

/**
 * Read-only lists a client needs to fill the forms of templates and campaigns without typing identifiers.
 */
public interface TemplateReferenceService {

    /** Active template types, by name. */
    List<ReferenceItemResponse> listTemplateTypes();

    /** Active severity types, by name. */
    List<ReferenceItemResponse> listSeverityTypes();

    /**
     * Active templates an application can use in a campaign.
     *
     * @param applicationId application whose templates are listed; required.
     * @param sessionToken  the caller's session token, checked against the application.
     * @throws IllegalArgumentException if {@code applicationId} is {@code null}.
     * @throws com.umdc.mercury.api.v1.exception.ForbiddenException if the caller may not use the application's templates.
     */
    List<TemplateDefinedItemResponse> listDefined(UUID applicationId, String sessionToken);
}
