package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.LoginClientAlreadyExistsException;
import com.umdc.mercury.api.v1.exception.LoginClientNotFoundException;
import com.umdc.mercury.api.v1.to.CreateLoginClientRequest;
import com.umdc.mercury.api.v1.to.RotateLoginClientPasswordRequest;
import com.umdc.mercury.api.v1.to.UpdateLoginClientRequest;
import com.umdc.mercury.jpa.sql.entity.LoginClientEntity;
import com.umdc.mercury.jpa.sql.repository.LoginClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginClientAdminServiceImplTest {

    private static final String TOKEN = "session";
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID APPLICATION = UUID.randomUUID();
    private static final String PERMISSION = "LOGIN_CLIENT_MANAGE";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    private final LoginClientRepository repository = mock(LoginClientRepository.class);
    private final ApplicationAuthorizationService authorization = mock(ApplicationAuthorizationService.class);
    private LoginClientAdminServiceImpl service;

    private static LoginClientEntity row(String alias, boolean active) {
        var entity = new LoginClientEntity();
        entity.setId(UUID.randomUUID());
        entity.setAlias(alias);
        entity.setPasswordHash(ENCODER.encode("old-password-123"));
        entity.setActive(active);
        return entity;
    }

    @BeforeEach
    void setUp() {
        service = new LoginClientAdminServiceImpl(repository, authorization, APPLICATION.toString(), PERMISSION);
        when(repository.save(any(LoginClientEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.saveAndFlush(any(LoginClientEntity.class))).thenAnswer(call -> call.getArgument(0));
    }

    // ---- authorization -------------------------------------------------------------------------------------------------------------

    @Test
    void everyOperationChecksThePermissionInTheConfiguredApplication() {
        var existing = row("smartkin-backend", true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.list(TOKEN);
        service.update(existing.getId(), new UpdateLoginClientRequest("x", null), TOKEN, ACTOR);
        service.rotatePassword(existing.getId(), null, TOKEN, ACTOR);
        service.delete(existing.getId(), TOKEN, ACTOR);

        verify(authorization, times(4)).assertPermission(TOKEN, APPLICATION, PERMISSION);
    }

    @Test
    void aCallerWithoutThePermissionTouchesNothing() {
        doThrow(new ForbiddenException("no")).when(authorization).assertPermission(TOKEN, APPLICATION, PERMISSION);

        assertThrows(ForbiddenException.class, () -> service.list(TOKEN));
        var createRequest = new CreateLoginClientRequest("a", null, null);
        assertThrows(ForbiddenException.class, () -> service.create(createRequest, TOKEN, ACTOR));
        UUID unknownId = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () -> service.delete(unknownId, TOKEN, ACTOR));
        verifyNoInteractions(repository);
    }

    @Test
    void withoutAConfiguredApplicationNobodyCanAdminister() {
        for (String configured : new String[] {"", "  ", null, "not-a-uuid"}) {
            var unconfigured = new LoginClientAdminServiceImpl(repository, authorization, configured, PERMISSION);

            assertThrows(ForbiddenException.class, () -> unconfigured.list(TOKEN));
        }
        verifyNoInteractions(authorization, repository);
    }

    // ---- list ----------------------------------------------------------------------------------------------------------------------

    @Test
    void listsTheClientsByAliasWithoutAnyPasswordMaterial() {
        when(repository.findAll()).thenReturn(List.of(row("zerobound", true), row("Smartkin-backend", false)));

        var clients = service.list(TOKEN);

        assertEquals(List.of("Smartkin-backend", "zerobound"), clients.stream().map(c -> c.alias()).toList());
        assertFalse(clients.get(0).active());
        assertFalse(clients.toString().contains("$2"));
    }

    // ---- create --------------------------------------------------------------------------------------------------------------------

    @Test
    void createsAClientWithAGeneratedPasswordShownOnce() {
        when(repository.findByAlias("smartkin-backend")).thenReturn(Optional.empty());

        var result = service.create(new CreateLoginClientRequest("smartkin-backend", " SmartKin REST ", null), TOKEN, ACTOR);

        assertNotNull(result.password());
        assertTrue(result.password().length() >= 30);
        var saved = ArgumentCaptor.forClass(LoginClientEntity.class);
        verify(repository).saveAndFlush(saved.capture());
        assertTrue(ENCODER.matches(result.password(), saved.getValue().getPasswordHash()));
        assertEquals("SmartKin REST", saved.getValue().getDescription());
        assertTrue(saved.getValue().getActive());
        assertEquals("smartkin-backend", result.client().alias());
        assertNotNull(result.client().createdAt());
    }

    @Test
    void createsAClientWithTheSuppliedPasswordAndNeverEchoesIt() {
        when(repository.findByAlias("zerobound")).thenReturn(Optional.empty());

        var result = service.create(new CreateLoginClientRequest("zerobound", "  ", "a-supplied-pass-1"), TOKEN, ACTOR);

        assertNull(result.password());
        var saved = ArgumentCaptor.forClass(LoginClientEntity.class);
        verify(repository).saveAndFlush(saved.capture());
        assertTrue(ENCODER.matches("a-supplied-pass-1", saved.getValue().getPasswordHash()));
        assertNull(saved.getValue().getDescription());
    }

    @Test
    void refusesAnAliasThatAlreadyExists() {
        when(repository.findByAlias("zerobound")).thenReturn(Optional.of(row("zerobound", true)));

        var duplicate = new CreateLoginClientRequest("zerobound", null, null);
        assertThrows(LoginClientAlreadyExistsException.class,
                () -> service.create(duplicate, TOKEN, ACTOR));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void theUniqueConstraintDecidesARaceBetweenTwoRegistrations() {
        when(repository.findByAlias("zerobound")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(LoginClientEntity.class))).thenThrow(new DataIntegrityViolationException("uk"));

        var duplicate = new CreateLoginClientRequest("zerobound", null, null);
        assertThrows(LoginClientAlreadyExistsException.class,
                () -> service.create(duplicate, TOKEN, ACTOR));
    }

    // ---- update --------------------------------------------------------------------------------------------------------------------

    @Test
    void switchesAClientOffAndChangesItsDescription() {
        var existing = row("zerobound", true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var updated = service.update(existing.getId(), new UpdateLoginClientRequest("Zerobound API", false), TOKEN, ACTOR);

        assertFalse(updated.active());
        assertEquals("Zerobound API", updated.description());
        assertNotNull(updated.updatedAt());
    }

    @Test
    void fieldsLeftOutOfAnUpdateStayAsTheyWere() {
        var existing = row("zerobound", true);
        existing.setDescription("keep me");
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var updated = service.update(existing.getId(), new UpdateLoginClientRequest(null, null), TOKEN, ACTOR);

        assertTrue(updated.active());
        assertEquals("keep me", updated.description());
    }

    @Test
    void aBlankDescriptionClearsIt() {
        var existing = row("zerobound", true);
        existing.setDescription("old");
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertNull(service.update(existing.getId(), new UpdateLoginClientRequest("   ", null), TOKEN, ACTOR).description());
    }

    @Test
    void anUnknownIdIsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        var updateRequest = new UpdateLoginClientRequest("x", null);
        assertThrows(LoginClientNotFoundException.class, () -> service.update(id, updateRequest, TOKEN, ACTOR));
        assertThrows(LoginClientNotFoundException.class, () -> service.rotatePassword(id, null, TOKEN, ACTOR));
        assertThrows(LoginClientNotFoundException.class, () -> service.delete(id, TOKEN, ACTOR));
    }

    // ---- rotate / delete -----------------------------------------------------------------------------------------------------------

    @Test
    void rotatingWithoutABodyGeneratesAPasswordAndReactivatesTheClient() {
        var existing = row("zerobound", false);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var result = service.rotatePassword(existing.getId(), null, TOKEN, ACTOR);

        assertNotNull(result.password());
        assertTrue(result.client().active());
        assertTrue(ENCODER.matches(result.password(), existing.getPasswordHash()));
        assertFalse(ENCODER.matches("old-password-123", existing.getPasswordHash()));
    }

    @Test
    void rotatingWithASuppliedPasswordStoresItAndDoesNotEchoIt() {
        var existing = row("zerobound", true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var result = service.rotatePassword(existing.getId(), new RotateLoginClientPasswordRequest("brand-new-pass-1"), TOKEN, ACTOR);

        assertNull(result.password());
        assertTrue(ENCODER.matches("brand-new-pass-1", existing.getPasswordHash()));
    }

    @Test
    void rotatingWithAnEmptyBodyGeneratesToo() {
        var existing = row("zerobound", true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertNotNull(service.rotatePassword(existing.getId(), new RotateLoginClientPasswordRequest(null), TOKEN, ACTOR).password());
    }

    @Test
    void deletesTheClient() {
        var existing = row("zerobound", true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.delete(existing.getId(), TOKEN, ACTOR);

        verify(repository).delete(existing);
    }
}
