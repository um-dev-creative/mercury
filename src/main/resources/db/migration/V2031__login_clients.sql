-- Backend callers allowed to obtain a Mercury session-token through POST /api/v1/auth/token (AuthServiceImpl / LoginClientVerifier).
-- Run against the Mercury schema (PostgreSQL), like the other migrations in this folder; Mercury never migrates the schema itself.
--
-- `alias` is the NAME OF THE CALLING SERVICE (e.g. smartkin-backend), not an end user, and it is also the identity the rest of Mercury scopes
-- the caller's data by (MachineTokenInterceptor.CALLER). Only the bcrypt hash of the password is stored; generate it with
-- scripts/login-clients/add-login-client.sh. An alias that is not in this table still falls back to umdc.mercury.login-clients (application.yml),
-- so the existing directory-backend and zerobound entries keep working until they are moved here.

CREATE TABLE IF NOT EXISTS mercury.login_clients (
    id            uuid PRIMARY KEY DEFAULT general.uuid_generate_v4(),
    alias         varchar(100) NOT NULL,
    password_hash varchar(100) NOT NULL,
    description   varchar(200),
    active        boolean      NOT NULL DEFAULT true,
    created_at    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT login_clients_alias_uk UNIQUE (alias),
    CONSTRAINT login_clients_alias_not_blank CHECK (length(btrim(alias)) > 0)
);
