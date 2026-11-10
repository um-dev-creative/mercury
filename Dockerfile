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

# No templates are baked into the image: they live in the storage set by TEMPLATE_STORAGE (a Cloudflare R2/S3 bucket, or the
# folder TEMPLATE_PATH, which Mercury creates if missing; mount a writable volume there for local storage).

# certs/mercury/ (gitignored) is NOT part of the image: it holds private keys (the Aiven Kafka client key, svc.pem, the keystores), and an
# image pushed to a registry is readable by whoever can pull it (a public Docker Hub repository means everyone). The folder is mounted
# READ-ONLY at run time at this exact WORKDIR-relative path, which application.yml reads as file:certs/mercury/... (spring.cloud.vault.ssl,
# spring.cloud.config.tls, eureka.client.tls, umdc.security). docker-compose.yml does the mount; docker-entrypoint.sh refuses to start,
# naming the missing files, when it is not there. The files must be readable by the image's non-root user (chmod 644 on a Linux host).
# An empty mount point is created here so the directory exists with the right owner.
RUN mkdir -p certs/mercury

RUN addgroup -S "${APP_GROUP}" && adduser -S "${APP_USER}" -G "${APP_GROUP}" && \
    chown -R "${APP_USER}:${APP_GROUP}" . && \
    chmod -R 740 . && \
    chmod 750 docker-entrypoint.sh

USER ${APP_USER}:${APP_GROUP}

# The Aiven client certificates are mounted (see above) and application.yml defaults to PLAINTEXT
# for local dev, which Aiven drops ("Node -1 disconnected"). Override at run time if ever needed.
ENV KAFKA_SECURITY_PROTOCOL=SSL

EXPOSE 8118

# docker-entrypoint.sh does the full `exec java ...` itself (mirroring the -D flags Docker's
# exec-form CMD can't shell-expand anyway); no separate ENTRYPOINT/CMD split needed.
CMD ["./docker-entrypoint.sh"]
