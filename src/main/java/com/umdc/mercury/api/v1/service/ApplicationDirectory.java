package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.client.BackboneApplicationClient;
import com.umdc.mercury.client.to.BackboneApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/// The applications registered in backbone-rest, read through its API with the caller's session. Mercury never reads the
/// `general` schema. It fails closed: when backbone cannot answer, nobody is told about any application.
@Component
public class ApplicationDirectory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDirectory.class);
    private static final String BEARER = "Bearer ";

    private final BackboneApplicationClient client;

    public ApplicationDirectory(BackboneApplicationClient client) {
        this.client = client;
    }

    /// Every application backbone-rest knows, with the session of the caller.
    ///
    /// @throws ForbiddenException when there is no session, or backbone cannot answer or refuses it
    public List<BackboneApplication> all(String sessionToken) {
        if (Objects.isNull(sessionToken) || sessionToken.isBlank()) {
            throw new ForbiddenException("Applications cannot be listed without a session");
        }
        try {
            List<BackboneApplication> found = client.applications(BEARER + sessionToken.trim());
            return Objects.isNull(found) ? List.of() : found.stream().filter(Objects::nonNull).toList();
        } catch (Exception e) {
            LOGGER.warn("Backbone could not list the applications: {}", e.getMessage());
            throw new ForbiddenException("Applications cannot be listed", e);
        }
    }

    /// The applications backbone-rest has enabled.
    public List<BackboneApplication> active(String sessionToken) {
        return all(sessionToken).stream().filter(BackboneApplication::isActive).toList();
    }

    /// The applications whose `code_name` equals the given one, ignoring case.
    public List<BackboneApplication> byCodeName(String sessionToken, String codeName) {
        return all(sessionToken).stream().filter(a -> Objects.nonNull(a.codeName()) && a.codeName().equalsIgnoreCase(codeName)).toList();
    }
}
