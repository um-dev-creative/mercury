package com.umdc.mercury.security;

import java.util.Set;
import java.util.UUID;

/**
 * The backend caller a Mercury M2M token stands for: the Backbone managed client ({@code sub}) that exchanged its Backbone token for it, the application
 * Backbone says that client belongs to ({@code aid}) and the Mercury scopes it was granted. It can only act on that application.
 *
 * @param clientId      the Backbone managed client (the token's subject)
 * @param clientName    its human-readable name, for logs
 * @param applicationId the application the client belongs to
 * @param scopes        the {@code mercury:*} scopes granted
 */
public record MachineCaller(String clientId, String clientName, UUID applicationId, Set<String> scopes) {

    /** Scope that lets a caller read (templates, message status). */
    public static final String SCOPE_READ = "mercury:message:read";
    /** Scope that lets a caller send, and create, edit and delete templates. */
    public static final String SCOPE_WRITE = "mercury:message:write";

    /** Whether the caller may only look ({@code GET}/{@code HEAD}) or may change things: {@code write} covers both. */
    public boolean allows(boolean write) {
        return scopes.contains(SCOPE_WRITE) || (!write && scopes.contains(SCOPE_READ));
    }
}
