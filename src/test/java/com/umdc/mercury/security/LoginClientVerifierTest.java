package com.umdc.mercury.security;

import com.umdc.mercury.jpa.sql.entity.LoginClientEntity;
import com.umdc.mercury.jpa.sql.repository.LoginClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginClientVerifierTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    private final LoginClientRepository repository = mock(LoginClientRepository.class);
    private LoginClientVerifier verifier;

    private static LoginClientEntity stored(String alias, String password, boolean active) {
        var entity = new LoginClientEntity();
        entity.setAlias(alias);
        entity.setPasswordHash(ENCODER.encode(password));
        entity.setActive(active);
        return entity;
    }

    private static LoginClientProperties staticRegistry(String alias, String password) {
        var client = new LoginClientProperties.LoginClient();
        client.setAlias(alias);
        client.setPassword(password);
        var properties = new LoginClientProperties();
        properties.setLoginClients(List.of(client));
        return properties;
    }

    @BeforeEach
    void setUp() {
        when(repository.findByAlias(anyString())).thenReturn(Optional.empty());
        verifier = new LoginClientVerifier(repository, staticRegistry("directory-backend", "static-pwd"), true);
    }

    @Test
    void acceptsAnActiveClientOfTheTable() {
        when(repository.findByAlias("smartkin-backend")).thenReturn(Optional.of(stored("smartkin-backend", "s3cret", true)));

        assertTrue(verifier.isRegistered("smartkin-backend", "s3cret"));
    }

    @Test
    void acceptsTheHashThatScriptsLoginClientsAddLoginClientShProduces() {
        // generated with: MERCURY_LOGIN_PASSWORD='Test-pw 123' scripts/login-clients/add-login-client.sh smartkin-backend (htpasswd, $2y$)
        var entity = new LoginClientEntity();
        entity.setAlias("smartkin-backend");
        entity.setPasswordHash("$2y$10$L0.4QuAgr3MlPMbDehTJ9esCbuNVwuqM2DjJEbQhLqCuUkvA19wTC");
        entity.setActive(true);
        when(repository.findByAlias("smartkin-backend")).thenReturn(Optional.of(entity));

        assertTrue(verifier.isRegistered("smartkin-backend", "Test-pw 123"));
        assertFalse(verifier.isRegistered("smartkin-backend", "Test-pw 124"));
    }

    @Test
    void refusesTheWrongPasswordOfAClientOfTheTable() {
        when(repository.findByAlias("smartkin-backend")).thenReturn(Optional.of(stored("smartkin-backend", "s3cret", true)));

        assertFalse(verifier.isRegistered("smartkin-backend", "other"));
    }

    @Test
    void refusesAnInactiveClientEvenWithTheRightPassword() {
        when(repository.findByAlias("smartkin-backend")).thenReturn(Optional.of(stored("smartkin-backend", "s3cret", false)));

        assertFalse(verifier.isRegistered("smartkin-backend", "s3cret"));
    }

    @Test
    void anAliasInTheTableDoesNotFallBackToTheStaticRegistry() {
        when(repository.findByAlias("directory-backend")).thenReturn(Optional.of(stored("directory-backend", "new-pwd", true)));

        assertFalse(verifier.isRegistered("directory-backend", "static-pwd"));
        assertTrue(verifier.isRegistered("directory-backend", "new-pwd"));
    }

    @Test
    void anInactiveRowDisablesTheStaticEntryOfTheSameAlias() {
        when(repository.findByAlias("directory-backend")).thenReturn(Optional.of(stored("directory-backend", "new-pwd", false)));

        assertFalse(verifier.isRegistered("directory-backend", "static-pwd"));
    }

    @Test
    void anAliasMissingFromTheTableFallsBackToTheStaticRegistry() {
        assertTrue(verifier.isRegistered("directory-backend", "static-pwd"));
        assertFalse(verifier.isRegistered("directory-backend", "wrong"));
        assertFalse(verifier.isRegistered("nobody", "static-pwd"));
    }

    @Test
    void theStaticRegistryCanBeSwitchedOff() {
        verifier = new LoginClientVerifier(repository, staticRegistry("directory-backend", "static-pwd"), false);

        assertFalse(verifier.isRegistered("directory-backend", "static-pwd"));
    }

    @Test
    void whenTheTableCannotBeReadTheStaticClientsStillWorkAndTheTableOnesDoNot() {
        when(repository.findByAlias(anyString())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertTrue(verifier.isRegistered("directory-backend", "static-pwd"));
        assertFalse(verifier.isRegistered("smartkin-backend", "s3cret"));
    }

    @Test
    void refusesMissingOrBlankCredentials() {
        assertFalse(verifier.isRegistered(null, "x"));
        assertFalse(verifier.isRegistered(" ", "x"));
        assertFalse(verifier.isRegistered("smartkin-backend", null));
    }

    @Test
    void aPasswordLongerThanBcryptAcceptsIsRefusedNotAnError() {
        when(repository.findByAlias("smartkin-backend")).thenReturn(Optional.of(stored("smartkin-backend", "s3cret", true)));

        assertFalse(verifier.isRegistered("smartkin-backend", "x".repeat(100)));
    }

    @Test
    void aMalformedOrMissingStoredHashNeverMatches() {
        var broken = stored("smartkin-backend", "s3cret", true);
        broken.setPasswordHash("not-a-bcrypt-hash");
        when(repository.findByAlias("smartkin-backend")).thenReturn(Optional.of(broken));
        assertFalse(verifier.isRegistered("smartkin-backend", "s3cret"));

        broken.setPasswordHash(null);
        assertFalse(verifier.isRegistered("smartkin-backend", "s3cret"));
    }

    @Test
    void staticClientsDeclaredWithEmptyCredentialsNeverMatch() {
        var noPassword = new LoginClientProperties.LoginClient();
        noPassword.setAlias("zerobound");
        noPassword.setPassword("");
        var noAlias = new LoginClientProperties.LoginClient();
        noAlias.setAlias("");
        noAlias.setPassword("");
        var nulls = new LoginClientProperties.LoginClient();
        var properties = new LoginClientProperties();
        properties.setLoginClients(List.of(noPassword, noAlias, nulls));
        verifier = new LoginClientVerifier(repository, properties, true);

        assertFalse(verifier.isRegistered("zerobound", ""));
        assertFalse(verifier.isRegistered("zerobound", "x"));
    }

    @Test
    void noStaticRegistryAtAllMeansNoStaticClients() {
        var properties = new LoginClientProperties();
        properties.setLoginClients(null);
        verifier = new LoginClientVerifier(repository, properties, true);

        assertFalse(verifier.isRegistered("directory-backend", "static-pwd"));
    }
}
