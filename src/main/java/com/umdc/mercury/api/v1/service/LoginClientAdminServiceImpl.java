package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.LoginClientAlreadyExistsException;
import com.umdc.mercury.api.v1.exception.LoginClientNotFoundException;
import com.umdc.mercury.api.v1.to.CreateLoginClientRequest;
import com.umdc.mercury.api.v1.to.LoginClientResponse;
import com.umdc.mercury.api.v1.to.LoginClientSecretResponse;
import com.umdc.mercury.api.v1.to.RotateLoginClientPasswordRequest;
import com.umdc.mercury.api.v1.to.UpdateLoginClientRequest;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.entity.LoginClientEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import com.umdc.mercury.jpa.sql.repository.LoginClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoginClientAdminServiceImpl implements LoginClientAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginClientAdminServiceImpl.class);
    private static final String NOT_CONFIGURED = "Login clients administration is not configured";
    private static final int GENERATED_PASSWORD_BYTES = 24;
    private static final int EXACTLY_ONE = 1;

    private final LoginClientRepository repository;
    private final ApplicationAuthorizationService authorization;
    private final ApplicationRepository applications;
    private final String adminApplicationId;
    private final String adminApplication;
    private final String manageLoginClientsPermission;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public LoginClientAdminServiceImpl(LoginClientRepository repository, ApplicationAuthorizationService authorization,
                                       ApplicationRepository applications,
                                       @Value("${umdc.security.login-clients.admin-application-id:}") String adminApplicationId,
                                       @Value("${umdc.security.login-clients.admin-application:iris}") String adminApplication,
                                       @Value("${umdc.security.permissions.login-client-manage:LOGIN_CLIENT_MANAGE}") String manageLoginClientsPermission) {
        this.repository = repository;
        this.authorization = authorization;
        this.applications = applications;
        this.adminApplicationId = adminApplicationId;
        this.adminApplication = adminApplication;
        this.manageLoginClientsPermission = manageLoginClientsPermission;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginClientResponse> list(String sessionToken) {
        authorize(sessionToken);
        return repository.findAll().stream()
                .sorted(Comparator.comparing(LoginClientEntity::getAlias, String.CASE_INSENSITIVE_ORDER))
                .map(LoginClientAdminServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public LoginClientSecretResponse create(CreateLoginClientRequest request, String sessionToken, UUID actorId) {
        authorize(sessionToken);
        if (repository.findByAlias(request.alias()).isPresent()) {
            throw new LoginClientAlreadyExistsException("A login client with this alias already exists");
        }
        String generated = Objects.isNull(request.password()) ? generatePassword() : null;
        var entity = new LoginClientEntity();
        entity.setId(UUID.randomUUID());
        entity.setAlias(request.alias());
        entity.setDescription(blankToNull(request.description()));
        entity.setPasswordHash(encoder.encode(Objects.isNull(generated) ? request.password() : generated));
        entity.setActive(true);
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        LoginClientEntity saved = save(entity);
        LOGGER.info("Login client created. alias={}, by={}", saved.getAlias(), actorId);
        return new LoginClientSecretResponse(toResponse(saved), generated);
    }

    @Override
    @Transactional
    public LoginClientResponse update(UUID id, UpdateLoginClientRequest request, String sessionToken, UUID actorId) {
        authorize(sessionToken);
        LoginClientEntity entity = find(id);
        if (Objects.nonNull(request.description())) {
            entity.setDescription(blankToNull(request.description()));
        }
        if (Objects.nonNull(request.active())) {
            entity.setActive(request.active());
        }
        entity.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        LoginClientEntity saved = repository.save(entity);
        LOGGER.info("Login client updated. alias={}, active={}, by={}", saved.getAlias(), saved.getActive(), actorId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public LoginClientSecretResponse rotatePassword(UUID id, RotateLoginClientPasswordRequest request, String sessionToken, UUID actorId) {
        authorize(sessionToken);
        LoginClientEntity entity = find(id);
        String supplied = Objects.isNull(request) ? null : request.password();
        String generated = Objects.isNull(supplied) ? generatePassword() : null;
        entity.setPasswordHash(encoder.encode(Objects.isNull(generated) ? supplied : generated));
        entity.setActive(true);
        entity.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        LoginClientEntity saved = repository.save(entity);
        LOGGER.info("Login client password rotated. alias={}, by={}", saved.getAlias(), actorId);
        return new LoginClientSecretResponse(toResponse(saved), generated);
    }

    @Override
    @Transactional
    public void delete(UUID id, String sessionToken, UUID actorId) {
        authorize(sessionToken);
        LoginClientEntity entity = find(id);
        repository.delete(entity);
        LOGGER.info("Login client deleted. alias={}, by={}", entity.getAlias(), actorId);
    }

    /// The caller must hold the login-client-manage permission in the administering application: the one whose `code_name` is
    /// `admin-application` (default `iris`), or the id forced in `admin-application-id`. If it cannot be resolved to exactly one
    /// application nobody can administer the clients (fail closed).
    private void authorize(String sessionToken) {
        authorization.assertPermission(sessionToken, administeringApplication(), manageLoginClientsPermission);
    }

    private UUID administeringApplication() {
        if (Objects.nonNull(adminApplicationId) && !adminApplicationId.isBlank()) {
            try {
                return UUID.fromString(adminApplicationId.trim());
            } catch (IllegalArgumentException e) {
                LOGGER.error("umdc.security.login-clients.admin-application-id is not a UUID (value '{}'): remove LOGIN_CLIENTS_ADMIN_APPLICATION_ID so the application is found by code_name, or set the application's UUID", adminApplicationId.trim());
                throw new ForbiddenException(NOT_CONFIGURED, e);
            }
        }
        if (Objects.isNull(adminApplication) || adminApplication.isBlank()) {
            LOGGER.warn("Login clients administration refused: umdc.security.login-clients.admin-application is empty");
            throw new ForbiddenException(NOT_CONFIGURED);
        }
        List<ApplicationEntity> found = applications.findAllByCodeNameIgnoreCase(adminApplication.trim());
        if (found.size() != EXACTLY_ONE) {
            LOGGER.warn("Login clients administration refused: {} applications have the code_name '{}' (exactly one is needed)",
                    found.size(), adminApplication.trim());
            throw new ForbiddenException(NOT_CONFIGURED);
        }
        return found.get(0).getId();
    }

    private LoginClientEntity find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new LoginClientNotFoundException("Login client not found"));
    }

    private LoginClientEntity save(LoginClientEntity entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // two requests registering the same alias at once: the unique constraint of the table decides
            throw new LoginClientAlreadyExistsException("A login client with this alias already exists", e);
        }
    }

    private String generatePassword() {
        byte[] bytes = new byte[GENERATED_PASSWORD_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String blankToNull(String value) {
        return Objects.isNull(value) || value.isBlank() ? null : value.trim();
    }

    private static LoginClientResponse toResponse(LoginClientEntity entity) {
        return new LoginClientResponse(entity.getId(), entity.getAlias(), entity.getDescription(), Boolean.TRUE.equals(entity.getActive()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
