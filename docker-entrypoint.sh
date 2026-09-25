#!/bin/sh
# certs/mercury/ (gitignored) is baked into the image at this fixed WORKDIR-relative path;
# bootstrap.yml references it directly via file:certs/mercury/... (spring.cloud.vault.ssl,
# spring.cloud.config.tls, eureka.client.tls, umdc.security) with
# spring.cloud.config.override-none: true ensuring those local values win over whatever
# Vault/Config Server separately supply for the same keys. No JVM-wide trust store
# manipulation needed here as a result — each integration carries its own explicit trust
# material instead.
set -e

exec java \
    -XX:+UseCompactObjectHeaders \
    -Dspring.cloud.vault.enabled="${VAULT_ENABLED:-false}" \
    -Dspring.application.name=mercury \
    -Dapi.info.version=1.0.0 \
    -jar mercury.jar
