#!/bin/sh
# certs/mercury/ (gitignored) is MOUNTED read-only at this fixed WORKDIR-relative path (never baked into the image: it holds private keys);
# application.yml references it directly via file:certs/mercury/... (spring.cloud.vault.ssl,
# spring.cloud.config.tls, eureka.client.tls, umdc.security) with
# spring.cloud.config.override-none: true ensuring those local values win over whatever
# Vault/Config Server separately supply for the same keys. No JVM-wide trust store
# manipulation needed here as a result — each integration carries its own explicit trust
# material instead.
set -e

# Fail fast and say what is missing: without the mount, Tomcat/Vault/Kafka fail later with errors that never mention it.
missing=""
for f in prx-internal-ca.crt mercury.jks backbone.jks umdc-truststore.jks; do
    [ -r "certs/mercury/$f" ] || missing="$missing $f"
done
if [ "${KAFKA_SECURITY_PROTOCOL:-PLAINTEXT}" = "SSL" ]; then
    for f in aiven-kafka-7e59598.cert aiven-kafka-7e59598.key aiven-kafka-7e59598.pem; do
        [ -r "certs/mercury/$f" ] || missing="$missing $f"
    done
fi
if [ -n "$missing" ]; then
    echo "mercury: certificates missing or unreadable in /usr/local/runme/certs/mercury:$missing" >&2
    echo "mercury: mount your certs/mercury folder read-only there (docker-compose.yml does it) and make the files readable (chmod 644)." >&2
    exit 78
fi

exec java \
    -XX:+UseCompactObjectHeaders \
    -Dspring.cloud.vault.enabled="${VAULT_ENABLED:-false}" \
    -Dspring.application.name=mercury \
    -Dapi.info.version=1.0.0 \
    -jar mercury.jar
