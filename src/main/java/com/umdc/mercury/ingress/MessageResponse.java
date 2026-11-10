package com.umdc.mercury.ingress;

import java.util.UUID;

/** What {@code POST /api/v1/messages} answers: the id to ask {@code GET /api/v1/messages/{id}} with, and the state ({@code OPENED} = queued). */
public record MessageResponse(UUID id, String status, String message) {
}
