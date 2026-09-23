#!/bin/sh
# Imports every *.crt trust anchor found in $CERTS_DIR — by default certs/mercury/ baked
# into the image (see Dockerfile's COPY certs/mercury/); cert-manager can inject a Secret
# at the same path in Kubernetes to add/override entries without rebuilding — into a
# writable copy of the JVM trust store, then execs the real command.
#
# Runs as the unprivileged container user — the trust store it writes to is
# /usr/local/runme/cacerts (owned by that user, set up in the Dockerfile), never the
# read-only system cacerts under $JAVA_HOME.
set -eu

CERTS_DIR="${CERTS_DIR:-/usr/local/runme/certs/mercury}"
TRUSTSTORE="/usr/local/runme/cacerts"
TRUSTSTORE_PASSWORD="changeit"

if [ -d "$CERTS_DIR" ]; then
  for cert in "$CERTS_DIR"/*.crt; do
    [ -e "$cert" ] || continue
    alias=$(basename "$cert" .crt)
    if keytool -list -keystore "$TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -alias "$alias" >/dev/null 2>&1; then
      echo "docker-entrypoint: trust anchor '$alias' already present, skipping"
    else
      echo "docker-entrypoint: importing trust anchor '$alias' from $cert"
      keytool -importcert -trustcacerts -noprompt \
        -alias "$alias" \
        -file "$cert" \
        -keystore "$TRUSTSTORE" \
        -storepass "$TRUSTSTORE_PASSWORD"
    fi
  done
fi

exec "$@"
