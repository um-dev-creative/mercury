package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.TemplateApplicationResponse;

import java.util.List;

/// The applications whose templates a caller may manage.
public interface TemplateApplicationService {

    /// Active applications for which the caller holds the template-manage permission. A platform administrator gets all of them.
    ///
    /// @param sessionToken the caller's session token
    /// @return the applications by name; empty when the caller manages none
    List<TemplateApplicationResponse> listManageable(String sessionToken);
}
