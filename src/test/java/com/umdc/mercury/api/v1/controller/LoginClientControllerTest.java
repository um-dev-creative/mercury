package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.api.v1.service.LoginClientAdminService;
import com.umdc.mercury.api.v1.to.CreateLoginClientRequest;
import com.umdc.mercury.api.v1.to.LoginClientResponse;
import com.umdc.mercury.api.v1.to.LoginClientSecretResponse;
import com.umdc.mercury.api.v1.to.RotateLoginClientPasswordRequest;
import com.umdc.mercury.api.v1.to.UpdateLoginClientRequest;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginClientControllerTest {

    private static final String TOKEN = "session";
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final LoginClientAdminService service = mock(LoginClientAdminService.class);
    private final SessionJwtServiceImpl sessionJwtService = mock(SessionJwtServiceImpl.class);
    private final LoginClientController controller = new LoginClientController(service, sessionJwtService);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static LoginClientResponse client() {
        return new LoginClientResponse(ID, "smartkin-backend", null, true, null, null);
    }

    @Test
    void listsForAVerifiedUser() {
        when(sessionJwtService.getVerifiedUid(TOKEN)).thenReturn(Optional.of(ACTOR));
        when(service.list(TOKEN)).thenReturn(List.of(client()));

        var response = controller.list(TOKEN);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createsAndPointsAtTheNewResource() {
        var request = new CreateLoginClientRequest("smartkin-backend", null, null);
        when(sessionJwtService.getVerifiedUid(TOKEN)).thenReturn(Optional.of(ACTOR));
        when(service.create(request, TOKEN, ACTOR)).thenReturn(new LoginClientSecretResponse(client(), "generated"));

        var response = controller.create(request, TOKEN);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/api/v1/login-clients/" + ID, response.getHeaders().getLocation().toString());
        assertEquals("generated", response.getBody().password());
    }

    @Test
    void updatesRotatesAndDeletesAsTheVerifiedUser() {
        when(sessionJwtService.getVerifiedUid(TOKEN)).thenReturn(Optional.of(ACTOR));
        var update = new UpdateLoginClientRequest("x", false);
        var rotate = new RotateLoginClientPasswordRequest(null);
        when(service.update(ID, update, TOKEN, ACTOR)).thenReturn(client());
        when(service.rotatePassword(ID, rotate, TOKEN, ACTOR)).thenReturn(new LoginClientSecretResponse(client(), "p"));

        assertEquals(HttpStatus.OK, controller.update(ID, TOKEN, update).getStatusCode());
        assertEquals(HttpStatus.OK, controller.rotatePassword(ID, TOKEN, rotate).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(ID, TOKEN).getStatusCode());
        verify(service).delete(ID, TOKEN, ACTOR);
    }

    @Test
    void aTokenThatIsNotVerifiedNeverReachesTheService() {
        when(sessionJwtService.getVerifiedUid(TOKEN)).thenReturn(Optional.empty());

        assertThrows(InvalidSessionTokenException.class, () -> controller.list(TOKEN));
        var createRequest = new CreateLoginClientRequest("a", null, null);
        assertThrows(InvalidSessionTokenException.class, () -> controller.create(createRequest, TOKEN));
        var updateRequest = new UpdateLoginClientRequest(null, null);
        assertThrows(InvalidSessionTokenException.class, () -> controller.update(ID, TOKEN, updateRequest));
        assertThrows(InvalidSessionTokenException.class, () -> controller.rotatePassword(ID, TOKEN, null));
        assertThrows(InvalidSessionTokenException.class, () -> controller.delete(ID, TOKEN));
        verifyNoInteractions(service);
    }

    @Test
    void theRequestsValidateAliasAndPassword() {
        assertTrue(validator.validate(new CreateLoginClientRequest("smartkin-backend", "ok", "a-valid-password")).isEmpty());
        assertTrue(validator.validate(new CreateLoginClientRequest("smartkin-backend", null, null)).isEmpty());

        assertEquals(1, validator.validate(new CreateLoginClientRequest("bad alias;", null, null)).size());
        assertEquals(1, validator.validate(new CreateLoginClientRequest("", null, null)).stream().filter(v -> v.getPropertyPath().toString().equals("alias")).count() >= 1 ? 1 : 0);
        assertEquals(1, validator.validate(new CreateLoginClientRequest("a".repeat(101), null, null)).size());
        assertEquals(1, validator.validate(new CreateLoginClientRequest("a", "d".repeat(201), null)).size());
        assertEquals(1, validator.validate(new CreateLoginClientRequest("a", null, "short")).size());
        assertEquals(1, validator.validate(new CreateLoginClientRequest("a", null, "has a space in it 1")).size());
        assertEquals(1, validator.validate(new CreateLoginClientRequest("a", null, "x".repeat(73))).size());
        assertEquals(1, validator.validate(new RotateLoginClientPasswordRequest("short")).size());
        assertTrue(validator.validate(new RotateLoginClientPasswordRequest(null)).isEmpty());
        assertEquals(1, validator.validate(new UpdateLoginClientRequest("d".repeat(201), null)).size());
    }
}
