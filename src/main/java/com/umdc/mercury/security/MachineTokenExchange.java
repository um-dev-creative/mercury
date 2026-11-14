package com.umdc.mercury.security;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.security.to.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Exchanges a Backbone M2M token for a Mercury M2M token. An application never holds a Mercury password: it proves who it is with the token Backbone
 * issued to its managed client, which the resource-server chain already introspected (active, not revoked) before this runs. Mercury keeps what Backbone
 * says about the client (its id, its application, the {@code mercury:*} scopes) in the token it issues, so the later calls need no second round trip to
 * Backbone. Fails closed: a client Backbone does not tie to an application, or one with no Mercury scope, gets nothing.
 */
@Service
public class MachineTokenExchange {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineTokenExchange.class);
    private static final String SCOPE_PREFIX = "SCOPE_";

    private final SessionJwtServiceImpl sessionJwtService;

    public MachineTokenExchange(SessionJwtServiceImpl sessionJwtService) {
        this.sessionJwtService = sessionJwtService;
    }

    /**
     * @param principal the Backbone managed client, as introspected
     * @return the Mercury M2M token
     * @throws ForbiddenException the client has no application (an older backbone-rest) or holds no Mercury scope
     */
    public AuthResponse exchange(OAuth2AuthenticatedPrincipal principal) {
        UUID applicationId = applicationOf(principal);
        Set<String> scopes = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(SCOPE_PREFIX + "mercury:"))
                .map(authority -> authority.substring(SCOPE_PREFIX.length()))
                .collect(Collectors.toSet());
        if (!scopes.contains(MachineCaller.SCOPE_READ) && !scopes.contains(MachineCaller.SCOPE_WRITE)) {
            LOGGER.warn("M2M exchange refused: the client holds no Mercury scope. clientId={}", principal.getName());
            throw new ForbiddenException("The client holds no Mercury scope");
        }
        String name = principal.getAttribute("client_name");
        String token = sessionJwtService.generateMachineToken(principal.getName(), name, applicationId, scopes);
        LOGGER.info("M2M token issued. clientId={}, applicationId={}", principal.getName(), applicationId);
        return new AuthResponse(token);
    }

    private static UUID applicationOf(OAuth2AuthenticatedPrincipal principal) {
        Object raw = principal.getAttribute(ApplicationAwareTokenIntrospector.APPLICATION_ID_CLAIM);
        if (Objects.isNull(principal.getName()) || !(raw instanceof String text)) {
            LOGGER.warn("M2M exchange refused: Backbone does not tie the client to an application. clientId={}", principal.getName());
            throw new ForbiddenException("The client is not tied to an application");
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException _) {
            throw new ForbiddenException("The client is not tied to an application");
        }
    }
}
