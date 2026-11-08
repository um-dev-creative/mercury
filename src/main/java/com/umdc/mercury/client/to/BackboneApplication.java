package com.umdc.mercury.client.to;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/// An application as backbone-rest answers it in `GET /api/v1/applications`. Mercury reads applications only through that API,
/// never from the `general` schema, which belongs to backbone-rest.
///
/// @param id       application id
/// @param name     display name
/// @param codeName short code
/// @param active   whether backbone-rest has the application enabled (`null` is read as not enabled)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BackboneApplication(UUID id, String name, String codeName, Boolean active) {

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
