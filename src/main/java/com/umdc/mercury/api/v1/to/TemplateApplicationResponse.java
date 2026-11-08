package com.umdc.mercury.api.v1.to;

import java.util.UUID;

/// An application whose templates the caller may manage.
///
/// @param id       application id
/// @param name     display name
/// @param codeName short code, also the template folder
public record TemplateApplicationResponse(UUID id, String name, String codeName) {
}
