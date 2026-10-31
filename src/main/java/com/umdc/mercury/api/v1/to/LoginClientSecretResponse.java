package com.umdc.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonInclude;

/// The answer of creating a login client or rotating its password. `password` is present **only when Mercury generated it** and is shown
/// this once: afterwards only a bcrypt hash exists.
///
/// @param client   the login client
/// @param password the generated password, absent when the caller supplied its own
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginClientSecretResponse(LoginClientResponse client, String password) {
}
