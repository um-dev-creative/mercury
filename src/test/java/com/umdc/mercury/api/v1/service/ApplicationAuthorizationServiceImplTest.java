package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationAuthorizationServiceImpl unit tests")
class ApplicationAuthorizationServiceImplTest {

    private static final String SESSION_TOKEN = "session-token-value";
    private static final String PERMISSION = "TEMPLATE_MANAGE";

    @Mock
    private BackbonePermissionClient backbonePermissionClient;

    @Mock
    private AdministeringApplication administeringApplication;

    @Mock
    private com.umdc.mercury.security.SessionJwtServiceImpl sessions;

    private ApplicationAuthorizationServiceImpl service;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        service = new ApplicationAuthorizationServiceImpl(backbonePermissionClient, administeringApplication, sessions, "PLATFORM_ADMIN");
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

    @Nested
    @DisplayName("platform administrators")
    class PlatformAdministrators {

        private final UUID irisId = UUID.randomUUID();

        private PermissionCheckResponse answer(boolean granted, String permission) {
            return new PermissionCheckResponse(granted, permission, granted ? "Permission granted" : "not granted");
        }

        @Test
        @DisplayName("a platform administrator passes in an application where the role does not grant the permission")
        void platformAdminIsGrantedEverywhere() {
            when(administeringApplication.id(any())).thenReturn(irisId);
            when(backbonePermissionClient.check(new PermissionCheckRequest(PERMISSION, applicationId, SESSION_TOKEN)))
                    .thenReturn(answer(false, PERMISSION));
            when(backbonePermissionClient.check(new PermissionCheckRequest("PLATFORM_ADMIN", irisId, SESSION_TOKEN)))
                    .thenReturn(answer(true, "PLATFORM_ADMIN"));

            assertThatCode(() -> service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("the platform-admin check is not made when the application itself grants the permission")
        void applicationGrantSkipsThePlatformCheck() {
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class))).thenReturn(answer(true, PERMISSION));

            service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION);

            verifyNoInteractions(administeringApplication);
        }

        @Test
        @DisplayName("without the platform-admin permission the denial stands")
        void notPlatformAdminIsDenied() {
            when(administeringApplication.id(any())).thenReturn(irisId);
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class))).thenReturn(answer(false, PERMISSION));

            assertThrows(ForbiddenException.class, () -> service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION));
        }

        @Test
        @DisplayName("an unresolvable administering application means nobody is a platform administrator")
        void unresolvableAdministeringApplicationFailsClosed() {
            when(administeringApplication.id(any())).thenThrow(new ForbiddenException("not configured"));

            assertThat(service.isPlatformAdmin(SESSION_TOKEN)).isFalse();
        }

        @Test
        @DisplayName("a missing session token is never a platform administrator, and holds() refuses it too")
        void missingTokenFailsClosed() {
            assertThat(service.isPlatformAdmin(null)).isFalse();
            assertThat(service.isPlatformAdmin(" ")).isFalse();
            assertThat(service.holds(null, applicationId, PERMISSION)).isFalse();
            assertThat(service.holds(SESSION_TOKEN, null, PERMISSION)).isFalse();
            verifyNoInteractions(backbonePermissionClient);
        }

        @Test
        @DisplayName("a backbone failure while checking platform admin fails closed")
        void backboneFailureFailsClosed() {
            when(administeringApplication.id(any())).thenReturn(irisId);
            when(backbonePermissionClient.check(any(PermissionCheckRequest.class))).thenThrow(new RuntimeException("down"));

            assertThat(service.isPlatformAdmin(SESSION_TOKEN)).isFalse();
        }
    }

    @Nested
    @DisplayName("an application's M2M token")
    class MachineCaller {

        private com.umdc.mercury.security.MachineCaller caller(UUID application) {
            return new com.umdc.mercury.security.MachineCaller(UUID.randomUUID().toString(), "svc", application,
                    java.util.Set.of(com.umdc.mercury.security.MachineCaller.SCOPE_WRITE));
        }

        @Test
        @DisplayName("holds the permission in its own application without asking Backbone")
        void ownApplicationIsGranted() {
            when(sessions.getMachineCaller(SESSION_TOKEN)).thenReturn(java.util.Optional.of(caller(applicationId)));

            assertThat(service.holds(SESSION_TOKEN, applicationId, PERMISSION)).isTrue();
            service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION);
            org.mockito.Mockito.verifyNoInteractions(backbonePermissionClient);
        }

        @Test
        @DisplayName("is refused in any other application, and is never a platform administrator")
        void otherApplicationIsRefused() {
            when(sessions.getMachineCaller(SESSION_TOKEN)).thenReturn(java.util.Optional.of(caller(UUID.randomUUID())));

            assertThat(service.holds(SESSION_TOKEN, applicationId, PERMISSION)).isFalse();
            assertThat(service.isPlatformAdmin(SESSION_TOKEN)).isFalse();
            org.junit.jupiter.api.Assertions.assertThrows(com.umdc.mercury.api.v1.exception.ForbiddenException.class,
                    () -> service.assertPermission(SESSION_TOKEN, applicationId, PERMISSION));
            org.mockito.Mockito.verifyNoInteractions(backbonePermissionClient);
        }
    }
}
