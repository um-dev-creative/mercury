package com.prx.mercury.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Represents a recipient with optional custom parameters for communication purposes.
 *
 * Fields:
 * - identifier: A unique identifier for the recipient, such as an email, phone number, or chat ID.
 *   This value is mandatory and must not be null, blank, or empty.
 * - name: The name of the recipient. This field is optional and can be null.
 * - customParams: A map containing additional custom parameters specific to the recipient.
 *   If not provided, this will default to an empty map.
 *
 * Behavior:
 * - If customParams is null during instantiation, it is automatically initialized to an empty map.
 */
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
