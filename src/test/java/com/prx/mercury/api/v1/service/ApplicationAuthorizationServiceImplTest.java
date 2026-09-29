package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.service.ApplicationAuthorizationServiceImpl;
import com.umdc.mercury.client.BackbonePermissionClient;
import com.umdc.mercury.client.to.PermissionCheckRequest;
import com.umdc.mercury.client.to.PermissionCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationAuthorizationServiceImpl unit tests")
class ApplicationAuthorizationServiceImplTest {

    private static final String SESSION_TOKEN = "session-token-value";
    private static final String PERMISSION = "TEMPLATE_MANAGE";

    @Mock
    private BackbonePermissionClient backbonePermissionClient;

    private ApplicationAuthorizationServiceImpl service;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        service = new ApplicationAuthorizationServiceImpl(backbonePermissionClient);
        applicationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("guard clauses")
    class GuardClauses {

        @Test
        @DisplayName("throws when sessionToken is null")
        void nullSessionToken_throws() {
            assertThrows(ForbiddenException.class,
                    () -> service.assertPermission(null, applicationId, PERMISSION));
        }

        @Test
        @DisplayName("throws when sessionToken is blank")
        void blankSessionToken_throws() {
            assertThrows(ForbiddenException.class,
                    () -> service.assertPermission("   ", applicationId, PERMISSION));
        }

        @Test
        @DisplayName("throws when applicationId is null")
        void nullApplicationId_throws() {
            assertThrows(ForbiddenException.class,
                    () -> service.assertPermission(SESSION_TOKEN, null, PERMISSION));
        }
    }

    @Nested
    @DisplayName("delegates to BackbonePermissionClient")
    class Delegation {

        @Test
        @DisplayName("does not throw when backbone grants the permission")
        void granted_doesNotThrow() {
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class)))
                    .thenReturn(new PermissionCheckResponse(true, PERMISSION, "Permission granted"));

            assertThatCode(() -> service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sends the correct request shape to backbone")
        void sendsCorrectRequest() {
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class)))
                    .thenReturn(new PermissionCheckResponse(true, PERMISSION, "Permission granted"));

            service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION);

            ArgumentCaptor<PermissionCheckRequest> captor = ArgumentCaptor.forClass(PermissionCheckRequest.class);
            verify(backbonePermissionClient).check(captor.capture());
            assertThat(captor.getValue().sessionToken()).isEqualTo(SESSION_TOKEN);
            assertThat(captor.getValue().applicationId()).isEqualTo(applicationId);
            assertThat(captor.getValue().permission()).isEqualTo(PERMISSION);
        }

        @Test
        @DisplayName("throws ForbiddenException when backbone denies the permission")
        void denied_throws() {
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class)))
                    .thenReturn(new PermissionCheckResponse(false, PERMISSION, "not granted"));

            assertThrows(ForbiddenException.class,
                    () -> service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION));
        }

        @Test
        @DisplayName("fails closed (denies) when backbone returns a null response")
        void nullResponse_failsClosed() {
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class))).thenReturn(null);

            assertThrows(ForbiddenException.class,
                    () -> service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION));
        }

        @Test
        @DisplayName("fails closed (denies) when the backbone call throws")
        void clientThrows_failsClosed() {
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class)))
                    .thenThrow(new RuntimeException("backbone unreachable"));

            assertThrows(ForbiddenException.class,
                    () -> service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION));
        }
    }
}
