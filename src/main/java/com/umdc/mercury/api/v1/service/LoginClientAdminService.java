package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.CreateLoginClientRequest;
import com.umdc.mercury.api.v1.to.LoginClientResponse;
import com.umdc.mercury.api.v1.to.LoginClientSecretResponse;
import com.umdc.mercury.api.v1.to.RotateLoginClientPasswordRequest;
import com.umdc.mercury.api.v1.to.UpdateLoginClientRequest;

import java.util.List;
import java.util.UUID;

/// Administration of the backend services allowed to log in to Mercury (`mercury.login_clients`). Every operation first checks, against
/// backbone-rest's IAM and with the caller's own session token, that the user holds the login-client-manage permission.
public interface LoginClientAdminService {

    /// @param sessionToken the caller's session token
    /// @return every login client, without any password material
    List<LoginClientResponse> list(String sessionToken);

    /// Registers a service. The password is generated when the request carries none.
    ///
    /// @param request      the new service
    /// @param sessionToken the caller's session token
    /// @param actorId      the verified user making the change, for the audit log
    /// @return the service and, when Mercury generated it, its password (shown only this once)
    LoginClientSecretResponse create(CreateLoginClientRequest request, String sessionToken, UUID actorId);

    /// @param id           the login client
    /// @param request      the fields to change
    /// @param sessionToken the caller's session token
    /// @param actorId      the verified user making the change
    /// @return the updated service
    LoginClientResponse update(UUID id, UpdateLoginClientRequest request, String sessionToken, UUID actorId);

    /// Replaces the password and reactivates the service. The password is generated when the request carries none.
    ///
    /// @param id           the login client
    /// @param request      the new password, optional
    /// @param sessionToken the caller's session token
    /// @param actorId      the verified user making the change
    /// @return the service and, when Mercury generated it, its password (shown only this once)
    LoginClientSecretResponse rotatePassword(UUID id, RotateLoginClientPasswordRequest request, String sessionToken, UUID actorId);

    /// Removes the service. An alias that is still in the static `umdc.mercury.login-clients` list can log in through it again.
    ///
    /// @param id           the login client
    /// @param sessionToken the caller's session token
    /// @param actorId      the verified user making the change
    void delete(UUID id, String sessionToken, UUID actorId);
}
