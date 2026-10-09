package com.umdc.mercury.security;

import com.umdc.mercury.jpa.nosql.document.RevokedTokenDocument;
import com.umdc.mercury.jpa.nosql.repository.RevokedTokenNSRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("TokenRevocationService unit tests")
class TokenRevocationServiceTest {

    private RevokedTokenNSRepository repository;
    private TokenRevocationService service;

    @BeforeEach
    void setUp() {
        repository = mock(RevokedTokenNSRepository.class);
        service = new TokenRevocationService(repository);
    }

    @Test
    @DisplayName("revoke persists the jti with the token's own expiry")
    void revoke_persistsDocument() {
        Instant exp = Instant.now().plusSeconds(600);

        service.revoke("jti-1", exp);

        ArgumentCaptor<RevokedTokenDocument> captor = ArgumentCaptor.forClass(RevokedTokenDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().jti()).isEqualTo("jti-1");
        assertThat(captor.getValue().expiresAt()).isEqualTo(exp);
        assertThat(captor.getValue().revokedAt()).isNotNull();
    }

    @Test
    @DisplayName("revoke rejects a missing jti or expiry")
    void revoke_invalidArguments_throws() {
        Instant exp = Instant.now();
        assertThatThrownBy(() -> service.revoke(null, exp)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.revoke(" ", exp)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.revoke("jti-1", null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("isRevoked reflects the denylist")
    void isRevoked_reflectsRepository() {
        when(repository.existsById("revoked")).thenReturn(true);
        when(repository.existsById("fine")).thenReturn(false);

        assertThat(service.isRevoked("revoked")).isTrue();
        assertThat(service.isRevoked("fine")).isFalse();
    }

    @Test
    @DisplayName("a token without a jti is never reported revoked and never hits the store")
    void isRevoked_noJti_false() {
        assertThat(service.isRevoked(null)).isFalse();
        assertThat(service.isRevoked("")).isFalse();
        verifyNoInteractions(repository);
    }
}
