package com.prx.mercury.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record RecipientTO(
        @NotNull @NotBlank @NotEmpty
        String identifier, // email, phone, chatId segun canal
        String name,
        Map<String, Object> customParams
) {
    public RecipientTO {
        if (customParams == null) {
            customParams = Map.of();
        }
    }
}
