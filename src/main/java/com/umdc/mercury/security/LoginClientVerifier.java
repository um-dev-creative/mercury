package com.umdc.mercury.security;

import com.umdc.mercury.jpa.sql.entity.LoginClientEntity;
import com.umdc.mercury.jpa.sql.repository.LoginClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Decides whether an alias/password may obtain a Mercury session-token.
///
/// 1. The alias is looked up in `mercury.login_clients` (bcrypt hash). When it is there the database decides, and an inactive row is a
///    refusal: a service that was switched off must not come back through the static registry.
/// 2. An alias that is not in the table is checked against `umdc.mercury.login-clients` of `application.yml`, as before, unless
///    `umdc.mercury.login-registry.properties-enabled` is `false` (once every service has moved to the table).
///
/// A refusal never says which part was wrong, and an unknown alias still pays for one bcrypt comparison, so the response time does not tell
/// whether the alias exists. If the database cannot be read the table clients simply do not match (fail closed) while the static ones keep
/// working.
@Component
public class LoginClientVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginClientVerifier.class);

    private final LoginClientRepository repository;
    private final LoginClientProperties properties;
    private final boolean propertiesEnabled;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    /// bcrypt hash of a random string: compared against when the alias is unknown, to spend the same time as a real check.
    private final String decoyHash = encoder.encode(java.util.UUID.randomUUID().toString());

    public LoginClientVerifier(LoginClientRepository repository, LoginClientProperties properties,
                               @Value("${umdc.mercury.login-registry.properties-enabled:true}") boolean propertiesEnabled) {
        this.repository = repository;
        this.properties = properties;
        this.propertiesEnabled = propertiesEnabled;
    }

    /// Checks a caller's credentials.
    ///
    /// @param alias    the name of the calling service
    /// @param password its password
    /// @return `true` when the pair belongs to an active login client
    public boolean isRegistered(String alias, String password) {
        if (Objects.isNull(alias) || alias.isBlank() || Objects.isNull(password)) {
            return false;
        }
        Optional<LoginClientEntity> stored = findStored(alias);
        if (stored.isPresent()) {
            return Boolean.TRUE.equals(stored.get().getActive()) && matchesHash(password, stored.get().getPasswordHash());
        }
        matchesHash(password, decoyHash);
        return propertiesEnabled && matchesProperties(alias, password);
    }

    private Optional<LoginClientEntity> findStored(String alias) {
        try {
            return repository.findByAlias(alias);
        } catch (DataAccessException e) {
            LOGGER.error("The login clients table could not be read, only the static registry can match: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean matchesHash(String password, String hash) {
        try {
            return Objects.nonNull(hash) && encoder.matches(password, hash);
        } catch (IllegalArgumentException _) {
            // a password longer than bcrypt's 72 bytes, or a malformed stored hash: never a match
            return false;
        }
    }

    private boolean matchesProperties(String alias, String password) {
        List<LoginClientProperties.LoginClient> clients = properties.getLoginClients();
        // a client declared with an empty alias or password (an unset env placeholder) never matches
        return Objects.nonNull(clients) && clients.stream().anyMatch(client ->
                Objects.nonNull(client.getAlias()) && !client.getAlias().isBlank()
                        && Objects.nonNull(client.getPassword()) && !client.getPassword().isBlank()
                        && constantTimeEquals(client.getAlias(), alias)
                        && constantTimeEquals(client.getPassword(), password));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
