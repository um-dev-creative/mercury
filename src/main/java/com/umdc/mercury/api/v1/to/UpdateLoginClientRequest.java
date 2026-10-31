package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.Size;

/// Changes a login client. A field left out stays as it was.
///
/// @param description new free text (max 200)
/// @param active      `false` switches the service off without losing its row
public record UpdateLoginClientRequest(@Size(max = 200) String description, Boolean active) {
}
