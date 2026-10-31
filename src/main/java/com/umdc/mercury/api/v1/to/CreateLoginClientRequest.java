package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/// Registers a backend service allowed to log in to Mercury.
///
/// @param alias       the name of the calling service: letters, digits, `.`, `_` and `-` (max 100)
/// @param description free text (max 200), optional
/// @param password    12 to 72 printable ASCII characters without spaces; optional, Mercury generates one when absent
public record CreateLoginClientRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,100}$", message = "alias may only contain letters, digits, '.', '_' and '-' (max 100)") String alias,
        @Size(max = 200) String description,
        @Pattern(regexp = LoginClientPasswords.PATTERN, message = LoginClientPasswords.MESSAGE) String password) {
}
