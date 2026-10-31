package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.Pattern;

/// Replaces the password of a login client and reactivates it.
///
/// @param password 12 to 72 printable ASCII characters without spaces; optional, Mercury generates one when absent
public record RotateLoginClientPasswordRequest(
        @Pattern(regexp = LoginClientPasswords.PATTERN, message = LoginClientPasswords.MESSAGE) String password) {
}
