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

# certs/mercury/ (gitignored — never in git history, unlike the old keystore.jks incident) is
# baked into the image here by explicit choice: these keystores/truststores are read as
# file:certs/mercury/... (relative to WORKDIR, i.e. this exact path) by application.yml — see
# spring.cloud.vault.ssl, spring.cloud.config.tls, eureka.client.tls, and umdc.security there.
# This does mean they're extractable from the built image's layers by anyone with registry
# access — that trade-off (vs. runtime-mounting them fresh on every deploy target, which kept
# failing operationally) was made deliberately; see application.yml for the resolution logic.
#
# Every file here is gitignored, so a plain `COPY certs/mercury/ certs/mercury/` copies nothing
# when the build context is a git checkout (confirmed the actual cause of a "trustAnchors
# parameter must be non-empty" TLS failure at runtime — this directory was silently empty in
# the deployed image). Each file is instead passed in as its own BuildKit secret — same
# mechanism as maven_settings above — so `docker build` needs one `--secret id=<name>,src=<path
# on the build host>` per file below:
#   prx_internal_ca, mercury_jks, backbone_jks, umdc_truststore_jks, aiven_kafka_cert,
#   aiven_kafka_key, aiven_kafka_pem, aiven_kafka_p12, svc_pem
RUN --mount=type=secret,id=prx_internal_ca,target=/run/secrets/prx-internal-ca.crt \
    --mount=type=secret,id=mercury_jks,target=/run/secrets/mercury.jks \
    --mount=type=secret,id=backbone_jks,target=/run/secrets/backbone.jks \
    --mount=type=secret,id=umdc_truststore_jks,target=/run/secrets/umdc-truststore.jks \
    --mount=type=secret,id=aiven_kafka_cert,target=/run/secrets/aiven-kafka-7e59598.cert \
    --mount=type=secret,id=aiven_kafka_key,target=/run/secrets/aiven-kafka-7e59598.key \
    --mount=type=secret,id=aiven_kafka_pem,target=/run/secrets/aiven-kafka-7e59598.pem \
    --mount=type=secret,id=aiven_kafka_p12,target=/run/secrets/aiven-kafka.p12 \
    --mount=type=secret,id=svc_pem,target=/run/secrets/svc.pem \
    mkdir -p certs/mercury && \
    cp /run/secrets/prx-internal-ca.crt certs/mercury/prx-internal-ca.crt && \
    cp /run/secrets/mercury.jks certs/mercury/mercury.jks && \
    cp /run/secrets/backbone.jks certs/mercury/backbone.jks && \
    cp /run/secrets/umdc-truststore.jks certs/mercury/umdc-truststore.jks && \
    cp /run/secrets/aiven-kafka-7e59598.cert certs/mercury/aiven-kafka-7e59598.cert && \
    cp /run/secrets/aiven-kafka-7e59598.key certs/mercury/aiven-kafka-7e59598.key && \
    cp /run/secrets/aiven-kafka-7e59598.pem certs/mercury/aiven-kafka-7e59598.pem && \
    cp /run/secrets/aiven-kafka.p12 certs/mercury/aiven-kafka.p12 && \
    cp /run/secrets/svc.pem certs/mercury/svc.pem

RUN addgroup -S "${APP_GROUP}" && adduser -S "${APP_USER}" -G "${APP_GROUP}" && \
    chown -R "${APP_USER}:${APP_GROUP}" . && \
    chmod -R 740 . && \
    chmod 750 docker-entrypoint.sh

USER ${APP_USER}:${APP_GROUP}

EXPOSE 8118

# docker-entrypoint.sh does the full `exec java ...` itself (mirroring the -D flags Docker's
# exec-form CMD can't shell-expand anyway); no separate ENTRYPOINT/CMD split needed.
CMD ["./docker-entrypoint.sh"]
