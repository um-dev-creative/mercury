#!/usr/bin/env bash
# Prints the SQL that registers (or rotates the password of) a backend service allowed to log in to Mercury
# (POST /api/v1/auth/token). It does NOT touch the database: review it and run it with psql against the Mercury database.
#
#   MERCURY_LOGIN_PASSWORD='the-password' scripts/login-clients/add-login-client.sh smartkin-backend "SmartKin REST" | psql ...
#
# The password is read from MERCURY_LOGIN_PASSWORD (never as an argument, which would stay in the shell history and in `ps`) and only its
# bcrypt hash appears in the output. Give the service the same alias and password (smartkin-backend: MERCURY_LOGIN_ALIAS / MERCURY_LOGIN_PASSWORD).
# Needs `htpasswd` (Apache; preinstalled on macOS, package apache2-utils on Debian).
set -euo pipefail

alias_name="${1:-}"
description="${2:-}"
if [[ -z "$alias_name" || -z "${MERCURY_LOGIN_PASSWORD:-}" ]]; then
  echo "usage: MERCURY_LOGIN_PASSWORD=... $0 <alias> [description]" >&2
  exit 1
fi
if [[ ! "$alias_name" =~ ^[A-Za-z0-9._-]{1,100}$ ]]; then
  echo "the alias may only contain letters, digits, '.', '_' and '-' (max 100)" >&2
  exit 1
fi
if (( ${#MERCURY_LOGIN_PASSWORD} > 72 )); then
  echo "bcrypt only uses the first 72 bytes: use a shorter password" >&2
  exit 1
fi
command -v htpasswd >/dev/null || { echo "htpasswd not found" >&2; exit 1; }

# htpasswd prints "<user>:<hash>"; the hash starts with $2y$, which Spring's BCryptPasswordEncoder accepts.
hash="$(htpasswd -bnBC 10 x "$MERCURY_LOGIN_PASSWORD" | cut -d: -f2- | tr -d '\r\n')"
desc_sql="${description//\'/\'\'}"

cat <<SQL
INSERT INTO mercury.login_clients (alias, password_hash, description)
VALUES ('${alias_name}', '${hash}', NULLIF('${desc_sql}', ''))
ON CONFLICT (alias) DO UPDATE
   SET password_hash = EXCLUDED.password_hash,
       description   = COALESCE(EXCLUDED.description, mercury.login_clients.description),
       active        = true,
       updated_at    = CURRENT_TIMESTAMP;
SQL
