# Requires `mvn -DskipTests clean package` to have produced ${TARGET_FILE}${JAR_FILE} first —
# this image packages an already-built jar, it does not compile from source.
FROM amazoncorretto:21.0.11-alpine3.23
LABEL version="0.0.2"
LABEL description="Mercury API"
LABEL maintainer="Luis Mata luis.antonio.mata@gmail.com"

ARG TARGET_FILE=target/
ARG JAR_FILE=mercury.jar
ARG APP_USER=jvapps
ARG APP_GROUP=appmng

WORKDIR /usr/local/runme

COPY ${TARGET_FILE}${JAR_FILE} ${JAR_FILE}
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

# certs/mercury/ (gitignored — never in git history, unlike the old keystore.jks incident)
# is baked into the image here by explicit choice: these keystores/truststores are read as
# file:certs/mercury/... (relative to WORKDIR, i.e. this exact path) by bootstrap.yml, and
# docker-entrypoint.sh imports any *.crt in there as a JVM trust anchor at container start.
# This does mean they're extractable from the built image's layers by anyone with registry
# access — that trade-off (vs. runtime-mounting them fresh on every deploy target, which kept
# failing operationally) was made deliberately; see bootstrap.yml for the resolution logic.
COPY certs/mercury/ certs/mercury/

RUN addgroup -S ${APP_GROUP} && adduser -S ${APP_USER} -G ${APP_GROUP} && \
    cp "${JAVA_HOME}/lib/security/cacerts" cacerts && \
    chown -R ${APP_USER}:${APP_GROUP} . && \
    chmod -R 740 . && \
    chmod 755 /usr/local/bin/docker-entrypoint.sh

USER ${APP_USER}:${APP_GROUP}

EXPOSE 8118

ENTRYPOINT ["docker-entrypoint.sh"]
# spring.cloud.vault.enabled is intentionally not passed as -D here: in exec-form CMD, Docker
# never expands ${VAULT_ENABLED} (no shell involved), so the JVM used to receive the literal
# string "${VAULT_ENABLED}" as the property value. Set it as a plain SPRING_CLOUD_VAULT_ENABLED
# env var instead — Spring Boot's relaxed binding maps it to the property on its own.
CMD ["java", \
     "-Djavax.net.ssl.trustStore=/usr/local/runme/cacerts", \
     "-Djavax.net.ssl.trustStorePassword=changeit", \
     "-Dspring.application.name=mercury", \
     "-Dapi.info.version=1.0.0", \
     "-jar", "mercury.jar"]
