package com.umdc.mercury.api.v1.to;

import java.util.UUID;

/**
 * An entry of a small reference list (template types, severity types) a client shows in a selector.
 *
 * @param id          Identifier to send back in create and update requests.
 * @param name        Short name to show.
 * @param description Longer explanation; may be {@code null}.
 */
public record ReferenceItemResponse(UUID id, String name, String description) {
}
