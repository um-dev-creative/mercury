FROM amazoncorretto:25.0.4-alpine3.24
LABEL version="0.0.3"
LABEL description="Mercury API"
LABEL maintainer="Luis Mata luis.antonio.mata@gmail.com"

ARG TARGET_FILE=target/
ARG JAR_FILE=mercury.jar
ARG APP_USER=jvapps
ARG APP_GROUP=appmng

WORKDIR /usr/local/runme

COPY ${TARGET_FILE}${JAR_FILE} ${JAR_FILE}
COPY docker-entrypoint.sh docker-entrypoint.sh

# certs/mercury/ (gitignored — never in git history, unlike the old keystore.jks incident)
# is baked into the image here by explicit choice: these keystores/truststores are read as
# file:certs/mercury/... (relative to WORKDIR, i.e. this exact path) by bootstrap.yml — see
# spring.cloud.vault.ssl, spring.cloud.config.tls, eureka.client.tls, and umdc.security there.
# This does mean they're extractable from the built image's layers by anyone with registry
# access — that trade-off (vs. runtime-mounting them fresh on every deploy target, which kept
# failing operationally) was made deliberately; see bootstrap.yml for the resolution logic.
COPY certs/mercury/ certs/mercury/

RUN addgroup -S "${APP_GROUP}" && adduser -S "${APP_USER}" -G "${APP_GROUP}" && \
    chown -R "${APP_USER}:${APP_GROUP}" . && \
    chmod -R 740 . && \
    chmod 750 docker-entrypoint.sh

USER ${APP_USER}:${APP_GROUP}

EXPOSE 8118

# docker-entrypoint.sh does the full `exec java ...` itself (mirroring the -D flags Docker's
# exec-form CMD can't shell-expand anyway); no separate ENTRYPOINT/CMD split needed.
CMD ["./docker-entrypoint.sh"]
